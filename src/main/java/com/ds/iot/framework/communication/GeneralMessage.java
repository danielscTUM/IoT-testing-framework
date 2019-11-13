package com.ds.iot.framework.communication;

import com.microsoft.azure.sdk.iot.device.MessageProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;

@Setter
public class GeneralMessage {

  public GeneralMessage(String payload) {this.payload = payload;}
  private String payload;
  private Map properties = new HashMap();

  public String getProperty(String key) {
    return properties.containsKey(key) ? properties.get(key).toString() : null;
  }

  public void setProperty(String key, String value) {
    this.properties.put(key, value);
  }

  public List<MessageProperty> getProperties() {
    List<MessageProperty> result = new ArrayList<MessageProperty>();
    for(Object key : properties.keySet()) {
      result.add(new MessageProperty(key.toString(), properties.get(key).toString()));
    }
    return result;
  }

  public String getPayload() {
    return payload;
  }
}
