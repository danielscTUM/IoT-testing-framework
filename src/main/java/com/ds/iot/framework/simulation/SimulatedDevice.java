package com.ds.iot.framework.simulation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SimulatedDevice {

  private Device device;
  private double delay;
  private Scenario scenario;
}
