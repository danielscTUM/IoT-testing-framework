# Device definition

For the definition of a device a json file and a java class are needed. The json file
describes the interface, state, connection details, and telemetry format of a device and the
java class is used to define the methods that determine it's behaviour.
Here we show the example of the definition of a radiator device.

````
{
  "className": "com.ds.testdevices.devices.Radiator",
  "state": {
    "initialState": {
      "temperature": "80"
    },
    "stateChange": {
      "startCommand": "startChange",
      "type": "data",
      "interval": 1,
      "data": {
        "path": "pathToFolder/RadiatorTelemetryData1",
        "mode": "simple"
      }
    }
  },
  "tag": "radiator",
  "connection": {
    "protocol": "MQTT",
    "url": "tcp://localhost:1883"
  },
  "userInterface": [
    "changeTemperature\\d* -> changeTemperature",
    "register\\?user=.* -> register"
  ],
  "telemetry": {
    "interval": 1,
    "messageTemplate": {"id": "${deviceId}","temperature": "${temperature}"},
    "startCommand": "TestTelemetry",
  }
}
````

- className: full class name of the associated class
- state: describes the initial state of the device and how the state changes over time
  - stateChange: 
    - startCommand: command that has to be send to the device to start the state change. The function takes no parameters.
    - interval (sec): Interval between state changes
    - type: "data" | "function" , there are two different forms how the state of a device can change
    automatically over time. 
      - data: If the type "data" is chosen, a json array of device states is used to update the state
        - path: path of the data file
        - mode: "simple" | "repeat". In "simple" mode the data is used once and when the end of the data array is
         reached the device state is updated always with the last value. In "repeat" mode the data is repeatedly used to
         change the state of the device.
      - (function): If the type "function" is used a method of the associated class determines the state changes
       - name: name of the function to change the state. The function takes no parameters.
- connection: details of the connection to the cloud
  - protocol: "MQTT" | "Websocket" | "AMQP" | "Azure" | \<user defined connection strategy>
  - url: url of cloud connection
- userInterface: json array that describes the interface of the device. The entries have the structure "regEx -> methodName".
  Methods of the associated class are called when the device receives a message that matches their regular expression.   
- telemetry:
  - interval (sec): Interval between send events
  - messageTemplate: json object that describes the telemetry format. Fields of the associated class can be incorporated
  by using ${fieldName}.
  - startCommand: command that has to be send to the device to start telemetry
  
The associated class of our radiator example is shown here:

 ```java
@Getter
@Setter
@NoArgsConstructor
public class Radiator extends Device {

  private int temperature = 0;
  private String userId;

  public void changeTemperature(String command, Object data) {
    try {
      this.temperature = Integer.parseInt(getURLParams(command).get("temperature"));
      getCommunicationStrategy().send(this.topic,
          "print?message=Temperature for device " + getDeviceId() + " has been set to " + temperature);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }

  public void register(String command, Object data) {
    this.userId = getURLParams(this, command).get("userId");
    this.topic = userId + "-in";
  }
}
````

The class extends is a child of the Device class. The parent class provides some
basic fields like the communication strategy that is used to send messages, the topic that is used to publish
telemetry data and a log for errors and infos respectively.

The Radiator class itself defines the methods of the device that were declared in the userInterface in the JSON file.
All methods take two parameters. A String parameter for the command that triggered the execution of the method and an Object parameter that
can be used to provide additional data.

The `register(String command, Object data)` method is used to define the userId of a possible owner of the device and
thereby determine the topic that is used to send telemetry data.
The `changeTemperature(String command, Object data)` method can be used to change the radiators temperature.


# Scenario
During a device simulation, simulated devices execute a predefined scenario. We follow the example from above
and define the scenario for our radiator device. 
```
Scenario registerRadiator = scenario("RegisterRadiator")
  .exec("connect")
  .exec("register?userId=User1")
  .pause(2)
  .exec("changeTemperature5");
```

The `scenario(String name)` method creates a scenario. With `exec(String command)` we can add commands that are send
to the device during execution. The `pause(long seconds)`method adds pause to the scenario.

# Device Simulation

```
simulation().setDevices(
    atOnce(JsonExtractor.buildDevice(radiatorPath, 1), registerRadiator)
).start(1000);
```

A simulation is determined by the devices that are simulated and the time the simulation should go on.
We build devices using the method `buildDevice(String pathToJsonFile, int n, String idPrefix)`. A device always executes a scenario
but the start of the execution does not have to coincide with the start of the simulation. If we want the
execution to start immediately we use the method `atOnce(List<Device> devices, Scenario scenario)`.
To spread the execution start of a list of devices the method `ramp(List<Device> devices, long seconds, Scenario scenario)` 
can be used. Eventually the simulation is kicked of with the `start(long milliseconds`) method that also defines
the duration of the simulation.

# Checks
After the simulation checks can be performed to assert that the test was successful.

```
simulation().setDevices(
    atOnce(JsonExtractor.buildDevice(radiatorPath, 1, "radiator"), registerRadiator)
).start(1000).checkErrors();
```

The method `checkErrors()` checks that the errorLogs of the simulated devices are empty,
`checkLogContainsMessage(String message)` check that the infoLog contains a specific message.
`checkMessageInterval(String message, long max, long min)` can be used to test if the intervals between two messages that match
with the regular expression "message" ranges between "max" and "min".
`checkMessageResponse(String messageA, String messageB, String idA, String idB, long maxDelay)` can be used to test that every
message that matches "messageA" in the message log of a device with idA is responded by a message that matches "messageB" in the log of a device
with "idB" and that the delay between those messages is smaller than "maxDelay".

# Logs
Two logs are created at the end of the simulation from the individual logs of the simulated devices. The error log and the info log.
The error log contains per default all incidents of messages that devices are unable to handle. The info log contains all messages that 
devices received during the simulation. It is also possible to log custom events as well. This is done by using the methods
`logError(String message)` and `logInfo(String message)` that are provided by the `Device` superclass.

# Connection Strategies
There exist four default connection strategies that can be used to connect a simulated device to an IoT backend.
Which of them is used, is defined in the JSON file of a device definition under the key "connection.protocol". The options are "MQTT", "AMQP", "WebSocket" and "Azure".

### Azure
The Azure connection strategy connects a simulated device with an Azure IoT Hub. To use this option, you have to specify additional parameters
under "connection.params". Absolutely mandatory is the parameter "iotHubConnectionString". If you want to connect as a registered device, the parameter "deviceConnectionString"
is needed as well. However, if you want to register devices on the fly the parameter is not needed but you have to make sure that this is allowed by your IoT Hub settings.

### Create new connection strategies
To create a custom connection strategy you have to provide a Java class that is marked with the annotation `@CommunicationStrategy(key=<key>)` and that inherits from the class 'CommunicationStrategyBase'. The "key" argument in the annotation determines how the protocol can be specified in the definition of devices. 

# Message handling
When a device receives a message, it's message handler compares a key that is extracted from the message to the list of regular expressions which form the device interface that has been defined by the user of the testing framework. 
If the key matches one of the entries, the method connected with this entry is executed to simulate the device behavior. In addition, the event is logged in the device's info log. 
If the message does not match any of the entries the event is logged in the device's error log. 

How the key that is compared to the device interface is generated from a message can be modified. By default, the whole message is used as a key.

If the received message is mapped to the class `General message` (as it is done for all basic connection strategies), you can overwrite the method `SimpleEntry<String, Object> handleMessage(GeneralMessage message)` in the class
of your device to 
change the mapping. The return type is a combination of a String that holds the command that is compared to the device interface and an Object that can
hold additional data.

If the recived message has an arbitrary format, the method `SimpleEntry<String, Object> handleCustomFormatMessage(Object message)` has to be overwritten to extract the key.

# Simulated User
Beside the simple interaction model, where simulated devices react to commands from a given scenario, a dynamic
interaction between the testing framework and the simulated devices is possible using simulated users. In this case, the
simulated user is controlled by a (mostly simpler) scenario and  it reacts dynamically to responses from the simulated devices.

The definition of a simulated User is done by a simple Java class that inherits from the superclass "SimulatedUser" and defines the
methods of it's interface which have to be marked with the annotation `UserMethod(key=<key>).` When a simulated user gets a command from a
scenario that matches the regular expression <key> the respective method is executed.

To specify a simulated user and his devices the method `userDeviceGroup(Class<? extends SimulatedUser> userClass, Scenario scenario, String... devicePaths)` is used
where the user class, the scenario that is executed on the user and one or more paths of files with JSON files specifying simulated devices are handed over as
arguments.

A test case using an extended simulated user could look like this:
``` 
@SimulationTest
  public static void userBasedLoadTest() {
    Scenario userScenario = scenario("userScenario").exec("connect").exec("requestPairingTan");

    simulation().setUserDeviceGroups(
        userDeviceGroup(AzureUser.class, userScenario, "AzureDevice2", "AzureAppDevice")
    ).start(5000);
  }
```

# Results 
At the end of a test run, the results are provided in a "results" folder that contains an HTML file, that can be read offline with a modern browser, and all the necessary additional files to create a simple page containing the information about the executed test cases which can be inspected interactively by selecting a single test case and filtering the message log for interesting parts.

# Execute on cluster
To run device simulations on a cluster, the script run_tests.sh is used with two parameters
The first parameter is the number of pods that should be created and the second parameter is the docker
image that should be run on the pods.

```
scripts/run_tests.sh 3 backendTest:latest
```

In our example we run the container backendTest:latest on 3 pods.


