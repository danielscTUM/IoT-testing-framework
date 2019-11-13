package com.ds.iot.framework.examples.devices;

import com.ds.iot.framework.simulation.Device;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class App extends Device {

  public void signIn(String command, Object data) {
    Pattern pattern = Pattern.compile("signIn\\?userId=(.*)");
    Matcher matcher = pattern.matcher(command);
    if (matcher.matches()) {
      String user = matcher.group(1);
      if(getCommunicationStrategy() != null) {
        getCommunicationStrategy().subscribe(user + "-in", this);
      }
    }
  }

  public void print(String command, Object data) {
    Pattern pattern = Pattern.compile("print\\?message=(.*)");
    Matcher matcher = pattern.matcher(command);
    if (matcher.matches()) {
      String message = matcher.group(1);
    }
  }

  public void configureDustabzugshaube(String command, Object data) {
    getCommunicationStrategy().send("backend", command);
  }
}
