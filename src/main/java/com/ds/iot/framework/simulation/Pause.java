package com.ds.iot.framework.simulation;

public class Pause extends Command {

  public long duration;

  public Pause(long duration) {
    super("pause");
    this.duration = duration;
  }
}
