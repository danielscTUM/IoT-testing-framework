package com.ds.iot.framework.simulation;

import java.util.ArrayList;
import java.util.List;

public class Scenario {

  public String name;
  private List<Command> execution = new ArrayList<>();

  public Scenario(String name) {
    this.name = name;
  }

  public Scenario exec(String command) {
    this.execution.add(new Command(command));
    return this;
  }

  public Scenario pause(long seconds) {
    this.execution.add(new Pause(seconds));
    return this;
  }

  public List<Command> getExecution() {
    return execution;
  }
}
