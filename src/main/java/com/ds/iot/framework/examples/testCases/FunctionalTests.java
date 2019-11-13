package com.ds.iot.framework.examples.testCases;


import static com.ds.iot.framework.simulation.Simulation.simulation;
import static com.ds.iot.framework.simulation.UI.atOnce;
import static com.ds.iot.framework.simulation.UI.scenario;
import static com.ds.iot.framework.simulation.UI.userDeviceGroup;

import com.ds.iot.framework.TestDriver.SimulationTest;
import com.ds.iot.framework.examples.users.AzureUser;
import com.ds.iot.framework.simulation.JsonExtractor;
import com.ds.iot.framework.simulation.Scenario;
import com.ds.iot.framework.simulation.SimulatedUser;


public class FunctionalTests {

  public static String radiatorPath = "RadiatorDevice1";
  public static String appPath = "AppDevice";

  @SimulationTest
  public static void changeTemperatureTest() {
    Scenario registerRadiator = scenario("RegisterRadiator").exec("connect").pause(1)
        .exec("register?userId=User1").pause(3)
        .exec("changeTemperature?temperature=5");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        atOnce(JsonExtractor.buildDevice(radiatorPath, 1, "radiator"), registerRadiator),
        atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp)
    ).start(5000).checkLogContainsMessage(
        "print?message=Temperature of device radiator0 has been set to 5.0");
  }

  //@SimulationTest
  public static void azureTest2() {
    SimulatedUser user1 = new AzureUser();
    user1.exec("connect");

    simulation().setUserDeviceGroups(
        userDeviceGroup(user1, JsonExtractor.buildDevice("AzureDevice", "heatPump"),
            JsonExtractor.buildDevice("AzureAppDevice", "app"))
    ).start(7000);
  }
}
