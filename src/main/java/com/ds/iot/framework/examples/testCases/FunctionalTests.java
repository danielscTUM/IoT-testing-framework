package com.ds.iot.framework.examples.testCases;


import static com.ds.iot.framework.simulation.Simulation.simulation;
import static com.ds.iot.framework.simulation.UI.atOnce;
import static com.ds.iot.framework.simulation.UI.ramp;
import static com.ds.iot.framework.simulation.UI.scenario;

import com.ds.iot.framework.TestDriver.SimulationTest;
import com.ds.iot.framework.simulation.JsonExtractor;
import com.ds.iot.framework.simulation.Scenario;


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

  @SimulationTest
  public static void test1() {
    Scenario telemetry = scenario("Telemetry").exec("connect").exec("register?userId=User1")
        .pause(1).exec("TestTelemetry").exec("startChange");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 2, "radiator"), 1, telemetry)
    ).start(5000)
        .checkMessageInterval("print\\?message=\\{\"temperature\":\"\\d*\",\"id\":\"radiator1\"\\}", 900,
            1100);
  }

  @SimulationTest
  public static void functionalTest2() {
    Scenario telemetry = scenario("Telemetry").exec("connect").exec("register?userId=User1").exec("changeTemperature?temperature=42");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 1, "radiator"), 1, telemetry)
    ).start(0).checkMessageResponse("changeTemperature\\?temperature=\\d*",
        "print\\?message=Temperature for device .* has been set to \\d*", "radiator0", "app0", 2000);
  }
}
