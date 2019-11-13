package com.ds.iot.framework.examples.users;

import com.ds.iot.framework.simulation.SimulatedUser;

public class AzureUser extends SimulatedUser {

  @UserMethod(key = "connect")
  public void connect(String command, Object payload) {
    getDevices().get("heatPump").tell("connect");
    //getDevices().get("app").tell("connect");
  }

  @UserMethod(key = "requestPairingTan")
  public void requestPairingTan(String command, Object payload) {
    getDevices().get("heatPump").tell("requestPairingTan", "Technician");
  }

  @UserMethod(key = "receivePairingTan")
  public void receivePairingTan(String command, Object payload) {
    devices.get("app").tell("pairDevice?tan=" + payload);
  }
}
