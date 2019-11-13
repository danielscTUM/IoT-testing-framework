package com.ds.iot.framework;

import static com.ds.iot.framework.simulation.Simulation.simulation;
import static com.ds.iot.framework.simulation.UI.atOnce;
import static com.ds.iot.framework.simulation.UI.ramp;
import static com.ds.iot.framework.simulation.UI.scenario;

import com.ds.iot.framework.TestDriver.SimulationTest;
import com.ds.iot.framework.examples.testCases.FunctionalTests;
import com.ds.iot.framework.simulation.JsonExtractor;
import com.ds.iot.framework.simulation.Scenario;

public class Main {

  public static String radiatorPath = "RadiatorDevice1";
  public static String appPath = "AppDevice";

  public static void main(String[] args) {
    TestDriver.start(FunctionalTests.class);
  }

  //@SimulationTest
  public static void test1() {
    Scenario telemetry = scenario("Telemetry").exec("connect").exec("register?userId=User1")
        .pause(1).exec("TestTelemetry").exec("startChange");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 2, "radiator"), 1, telemetry)
    ).start(5000)
        .checkMessageInterval("print\\?message=\\{\"temperature\":\"\\d*\",\"id\":\"radiator1\"\\}", 1000,
            1000);
  }

  @SimulationTest
  public static void functionalTest1() {
    Scenario telemetry = scenario("Telemetry").exec("connect").exec("register?userId=User1").exec("changeTemperature?temperature=42");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        //atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 1, "radiator"), 1, telemetry)
    ).start(0).checkMessageResponse("changeTemperature\\?temperature=\\d*",
        "print\\?message=Temperature for device .* has been set to \\d*", "radiator0", "app0", 2000);
  }

  @SimulationTest
  public static void scalabilityTest1() {
    Scenario telemetry = scenario("Telemetry").exec("connect").exec("register?userId=User1").exec("changeTemperature?temperature=42");
    Scenario signInApp = scenario("SignIn").exec("connect").pause(1).exec("signIn?userId=User1");

    simulation().setDevices(
        //atOnce(JsonExtractor.buildDevice(appPath, 1, "app"), signInApp),
        ramp(JsonExtractor.buildDevice(radiatorPath, 2, "radiator"), 1, telemetry)
    ).start(0).checkLogContainsMessage(("test"));
  }
}
