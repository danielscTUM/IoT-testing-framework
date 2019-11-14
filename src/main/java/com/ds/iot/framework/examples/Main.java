package com.ds.iot.framework.examples;

import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.examples.testCases.FunctionalTests;
import com.ds.iot.framework.examples.testCases.LoadTests;

public class Main {

  public static void main(String[] args) {
    TestDriver.start(LoadTests.class);
  }
}
