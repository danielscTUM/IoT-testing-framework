package com.ds.iot.framework.simulation;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LogMessage {

  private String message;
  private Date date;
  private String deviceId;

  @Override
  public String toString() {
    return date + " " + deviceId + " " + message;
  }
}
