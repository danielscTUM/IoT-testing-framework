package com.ds.iot.framework.examples.testCases;

import static com.ds.iot.framework.simulation.Simulation.simulation;
import static com.ds.iot.framework.simulation.UI.atOnce;
import static com.ds.iot.framework.simulation.UI.ramp;
import static com.ds.iot.framework.simulation.UI.scenario;

import com.ds.iot.framework.TestDriver.SimulationTest;
import com.ds.iot.framework.simulation.JsonExtractor;
import com.ds.iot.framework.simulation.Scenario;

public class LoadTests {

  public static String radiatorPath = "RadiatorDevice1";
  public static String appPath = "AppDevice";

  //@SimulationTest
  public void appLoadTestJson() {
    Scenario telemetry = scenario("Telemetry").exec("connect").pause(1)
        .exec("register?userId=User1").pause(1).exec("TestTelemetry");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 1000, "radiator"), 1, telemetry)
    ).checkErrors();
  }

  @SimulationTest
  public static void scalabilityTest() {
    Scenario telemetry = scenario("Telemetry").exec("connect").exec("register?userId=User1").pause(1).exec("startChange").exec("startTelemetry");
    Scenario signInApp = scenario("SignIn").exec("connect").exec("signIn?userId=User1");

    simulation().setDevices(
        atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 1, "radiator"), 1, telemetry)
    ).start(5000).checkMedianConnectionDuration(50);
  }
}
