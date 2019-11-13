package com.ds.iot.framework.simulation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.Getter;

@Getter
public class MessageLog implements Serializable {

  List<LogMessage> log = new ArrayList<>();

  HashMap<String, List<LogMessage>> logMap = new LinkedHashMap<>();

  HashMap<String, HashMap<String, List<LogMessage>>> deviceLogs = new LinkedHashMap<>();

  private void putToLog(LogMessage message, HashMap<String, List<LogMessage>> log){
    if(log.containsKey(message.getMessage())) {
      log.get(message.getMessage()).add(message);
    } else {
      List list = new ArrayList();
      list.add(message);
      log.put(message.getMessage(), list);
    }
  }

  private void add(LogMessage message, HashMap<String, List<LogMessage>> deviceLog) {
    putToLog(message, logMap);
    putToLog(message, deviceLog);
    log.add(message);
  }

  public void addAll(List<LogMessage> messages, String id) {
    if(messages.size() > 0) {
      HashMap<String, List<LogMessage>> deviceLog = new HashMap<>();
      messages.forEach(v -> {
        add(v, deviceLog);
      });
    }
  }

  public void joinLog(MessageLog log) {
    this.logMap.putAll(log.logMap);
    this.deviceLogs.putAll(log.deviceLogs);
    this.log.addAll(log.log);
  }

  public boolean contains(String s) {
    return logMap.containsKey(s);
  }

  public boolean containsRegEx(String regEx) {
     return logMap.keySet()
        .stream()
        .filter(s -> s.matches(regEx)).count() > 0;
  }

  public int size() {
    return logMap.size();
  }

}
