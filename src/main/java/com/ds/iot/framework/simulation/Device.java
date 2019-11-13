package com.ds.iot.framework.simulation;

import static java.util.Arrays.asList;

import akka.actor.ActorRef;
import com.ds.iot.framework.communication.CommunicationStrategy;
import com.ds.iot.framework.communication.GeneralMessage;
import com.ds.iot.framework.metrics.Metrics;
import com.ds.iot.framework.simulation.LogMessage;
import com.ds.iot.framework.simulation.NamedProcedure;
import com.ds.iot.framework.simulation.SimulatedUser;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.text.StringSubstitutor;
import org.json.JSONArray;
import org.json.JSONObject;

@NoArgsConstructor
@Setter
@Getter
public class Device {

  protected String deviceId;
  protected String url;
  private List<NamedProcedure> commonCommands = new LinkedList<NamedProcedure>(
      asList(new NamedProcedure("connect", ((c, p) -> connect())),
          new NamedProcedure("onError", ((e, p) -> onError((String) e)))));
  private ActorRef deviceActor;
  private CommunicationStrategy communicationStrategy;
  private List<LogMessage> errorLog = new ArrayList<>();
  private List<LogMessage> infoLog = new ArrayList<>();
  private ScheduledExecutorService executor;
  public String tag;
  public String topic;
  public Metrics metrics;
  public SimulatedUser user;

  public Device(String deviceId, CommunicationStrategy communicationStrategy) {
    this.deviceId = deviceId;
    this.communicationStrategy = communicationStrategy;
  }

  public void tell(String message) {
    this.deviceActor.tell(new SimpleEntry<>(message, null), ActorRef.noSender());
  }

  public void tell(String message, Object payload) {
    this.deviceActor.tell(new SimpleEntry<>(message, payload), null);
  }

  public void tell(GeneralMessage message) {
    this.deviceActor.tell(message, null);
  }

  public void tell(Object message) {
    this.deviceActor.tell(message, null);
  }

  public SimpleEntry<String, Object> handleMessage(GeneralMessage message) {
    return new SimpleEntry<>(message.getPayload(), message);
  }

  public SimpleEntry<String, Object> handleCustomFormatMessage(Object message) {
    return new SimpleEntry<>(message.toString(), message);
  }

  private void connect() {
    this.communicationStrategy.connectInternal(this);
  }

  public void onError(String message) {
    logError(message);
  }

  public void startStateChange(long frequency, JSONArray data, String mode) {
    if (data != null) {
      LambdaFunction lambdaFunction = () -> {
        AtomicInteger atomicInteger = new AtomicInteger();
        return () -> {
          if (atomicInteger.get() == data.length()) {
            switch (mode) {
              case "repeat":
                atomicInteger.set(0);
                break;
              case "simple":
                atomicInteger.set(data.length() - 1);
                break;
              default:
                atomicInteger.set(0);
                break;
            }
          }
          return data.getJSONObject(atomicInteger.getAndIncrement());
        };
      };
      JSONObjectFunction jsonObjectFunction = lambdaFunction.op();
      executor.scheduleAtFixedRate(() -> changeState(jsonObjectFunction), 0, frequency,
          TimeUnit.MILLISECONDS);
    }
  }

  public void startStateChange(long frequency, Method method) {
    executor.scheduleAtFixedRate(() -> {
          try {
            method.invoke(this);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
          }
        }, 0, frequency,
        TimeUnit.SECONDS);
  }

  private void changeState(JSONObjectFunction l) {
    JSONObject obj = l.op();
    for (String key : obj.keySet()) {
      Object value = obj.get(key);
      if (value != null) {
        Field field = getField(key, this);
        field.setAccessible(true);
        try {
          field.set(this, value);
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void startTelemetry(long frequency, JSONObject messageTemplate) {
    Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
    Matcher matcher = pattern.matcher(messageTemplate.toString());
    final List<String> matches = new ArrayList<>();
    while (matcher.find()) {
      matches.add(matcher.group(1));
    }
    executor.scheduleAtFixedRate(() -> doTelemetry(messageTemplate, matches), 0, frequency,
        TimeUnit.MILLISECONDS);
  }

  public interface LambdaFunction {

    public JSONObjectFunction op();
  }

  public interface JSONObjectFunction {

    public JSONObject op();
  }

  public String getTag() {
    return tag != null ? tag : "";
  }


  private Field getField(String key, Object object) {
    List<Field> fields = Arrays.asList(object.getClass().getDeclaredFields());
    for (Field field : fields) {
      if (field.getName().equals(key)) {
        return field;
      }
    }
    fields = Arrays.asList(object.getClass().getSuperclass().getDeclaredFields());
    for (Field field : fields) {
      if (field.getName().equals(key)) {
        return field;
      }
    }
    return null;
  }


  private Object getValueFromField(String key, Object obj) {
    if (getField(key, this) != null) {
      Field field = getField(key, obj);
      field.setAccessible(true);
      try {
        return field.get(obj);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private void doTelemetry(JSONObject messageTemplate, List<String> keys) {
    Map valuesMap = new HashMap();
    for (String key : keys) {
      valuesMap.put(key, getValueFromField(key, this));
    }
    StringSubstitutor substitutor = new StringSubstitutor(valuesMap);
    sendTelemetry(substitutor.replace(messageTemplate));
  }

  protected void sendTelemetry(String message) {
    communicationStrategy.send(topic, message);
  }

  public void logError(String message) {
    log(message, errorLog);
  }

  public void logInfo(String message) {
    log(message, infoLog);
  }

  private void log(String message, List<LogMessage> log) {
    log.add(new LogMessage(message, new Date(), deviceId));
  }
}