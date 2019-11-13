package com.ds.iot.framework.simulation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ScheduledTask {

  private Runnable task;
  private long delay;

  @Override
  public String toString() {
    return delay + "";
  }
}
