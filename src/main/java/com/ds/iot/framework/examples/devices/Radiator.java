package com.ds.iot.framework.examples.devices;

import static com.ds.iot.framework.simulation.UI.getURLParams;

import com.ds.iot.framework.simulation.Device;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Random;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Radiator extends Device {

  private double temperature = 0.0;
  private Random rand = new Random();

  public void changeTemperature(String command, Object data) {
    try {
      this.temperature = Integer.parseInt(getURLParams(command)
          .get("temperature"));
    } catch (URISyntaxException e) {
      logError("Wrong message syntax");
    }

    getCommunicationStrategy().send(this.topic,
        "print?message=Temperature of device " + getDeviceId() +
            " has been set to " + temperature);
  }

  public void register(String command, Object data) {
    try {
      String userId = getURLParams(command).get("userId");
      this.topic = userId + "-in";
    } catch (URISyntaxException e) {
      logError("Wrong message syntax");
    }
  }

  public void changeState() {
    this.temperature += (rand.nextDouble() - 0.5);
  }

  @Override
  public void sendTelemetry(String message) {
    getCommunicationStrategy().send(getDeviceId(), "print?message=" + message);
  }

  @Override
  public SimpleEntry<String, Object> handleCustomFormatMessage(Object data) {
    return null;
  }
}
