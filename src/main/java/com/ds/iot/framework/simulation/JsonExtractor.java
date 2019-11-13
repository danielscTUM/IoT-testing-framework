package com.ds.iot.framework.simulation;

import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.communication.CommunicationStrategy;
import com.ds.iot.framework.communication.HiveMqtt;
import com.ds.iot.framework.exceptions.TechnicalException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonExtractor {

  static String readFile(Path path, Charset encoding)
      throws IOException {
    byte[] encoded = Files.readAllBytes(path);
    return new String(encoded, encoding);
  }

  /**
   * Builds device from json object
   */
  private static Device buildDevice(JSONObject json, String id) {
    try {
      Device device = createDeviceObject(json);
      device.getCommonCommands().addAll(extractNamedProcedures(json, device));
      extractTag(json, device);
      JSONObject connection = json.optJSONObject("connection");
      if(connection == null) {
        throw new TechnicalException("Missing key 'connection' in device definition");
      }
      String protocol = connection.getString("protocol");
      String host = connection.getString("url");
      JSONObject params = connection.has("params") ? connection.getJSONObject("params") : null;
      device.setDeviceId(id);
      CommunicationStrategy strategy = null;
      HashMap paramsMap = new HashMap();
      if (params != JSONObject.NULL && params != null) {
        paramsMap = (HashMap) toMap(params);
      }
      for (String key : TestDriver.strategyHashMap.keySet()) {
        if (key.equals(protocol)) {
          try {

            Constructor<?> ctor = TestDriver.strategyHashMap.get(key).getConstructor();
            strategy = (CommunicationStrategy) ctor.newInstance();
            strategy.init(host, device, paramsMap);
            break;
          } catch (InstantiationException e) {
            e.printStackTrace();
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (NoSuchMethodException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
          }
        }
      }
      if (strategy == null) {
        strategy = new HiveMqtt();
        strategy.init(host, device, paramsMap);
      }
      device.setCommunicationStrategy(strategy);
      return device;
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }

  private static Path getAbsolutePath(String file) {
    List<File> descriptionDirectories = findDirectoriesWithSameName("device_descriptions",
        new File(System.getProperty("user.dir")), 3);
    File descriptionDirectory = null;
    if (descriptionDirectories.size() > 0) {
      descriptionDirectory = descriptionDirectories.get(0);
    }
    return Paths.get(descriptionDirectory.toString(), file);
  }

  public static List<Device> buildDevice(String path, String idPrefix) {
    List<Device> result = new ArrayList<>();
    Long pod_number = Long.parseLong(Optional.ofNullable(System.getenv("POD_NUMBER")).orElse("-1"));
    Long pod_count = Long
        .parseLong(Optional.ofNullable(System.getenv("NUMBER_OF_PODS")).orElse("1"));

    try {
      JSONObject json = new JSONObject(readFile(getAbsolutePath(path), StandardCharsets.UTF_8));
      // only build devices the pod is responsible for
      if (pod_count > 1 && 0 != pod_number) {
        return result;
      }
      result.add(buildDevice(json, idPrefix));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Build n devices from json file
   *
   * @param path path to json file
   * @param n number of devices
   * @return list of created devices
   */
  public static List<Device> buildDevice(String path, int n, String idPrefix) {
    List<Device> result = new ArrayList<>();

    Long pod_number = Long.parseLong(Optional.ofNullable(System.getenv("POD_NUMBER")).orElse("-1"));
    Long pod_count = Long
        .parseLong(Optional.ofNullable(System.getenv("NUMBER_OF_PODS")).orElse("1"));

    try {
      JSONObject json = new JSONObject(readFile(getAbsolutePath(path), StandardCharsets.UTF_8));
      for (int i = 0; i < n; i++) {
        // only build devices the pod is responsible for
        if (pod_count > 1 && i % pod_count != pod_number) {
          continue;
        }
        result.add(buildDevice(json, idPrefix + i));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private static void extractTag(JSONObject json, Device device) {
    String jsonTag = json.optString("tag");
    if (jsonTag != null) {
      String tag = jsonTag;
      device.tag = tag;
    }
  }

  private static NamedProcedure extractTelemetryProcedure(JSONObject json, Device device) {
    NamedProcedure result = null;
    JSONObject telemetry = json.optJSONObject("telemetry");
    if (telemetry != null) {
      int interval = telemetry.getInt("interval");
      String command = telemetry.getString("startCommand");
      JSONObject messageTemplate = telemetry.getJSONObject("messageTemplate");
      result = new NamedProcedure(command, (t, o) -> {
        device.startTelemetry(interval, messageTemplate);
      });
    }
    return result;
  }

  private static NamedProcedure extractStateChangeProcedure(JSONObject json, Device device) {
    NamedProcedure result = null;
    JSONObject state = json.optJSONObject("state");
    if (state != null) {
      JSONObject stateChange = state.optJSONObject("stateChange");
      if (stateChange != null) {
        int interval = stateChange.getInt("interval");
        String command = stateChange.getString("startCommand");
        String type = stateChange.getString("type");
        if ("data".equals(type)) {
          JSONObject dataObj = stateChange.optJSONObject("data");
          if (dataObj != null) {
            try {
              JSONArray data = readJson(getAbsolutePath(dataObj.getString("path")));
              String mode = dataObj.optString("mode") != null ? dataObj.getString("mode") : "";
              result = new NamedProcedure(command, (t, o) -> {
                device.startStateChange(interval, data, mode);
              });
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        } else if ("function".equals(type)) {
          JSONObject function = stateChange.optJSONObject("function");
          String name = stateChange.optString("name", null);
          if (name != null) {
            try {
              Method method = device.getClass().getMethod(name);
              result = new NamedProcedure(command, (t, o) -> {
                device.startStateChange(interval, method);
              });
            } catch (NoSuchMethodException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
    return result;
  }

  private static Device createDeviceObject(JSONObject json)
      throws ClassNotFoundException, IOException {
    Object obj;
    ObjectMapper mapper = new ObjectMapper();
    JSONObject state = json.optJSONObject("state");
    if (state != null) {
      JSONObject initialState = state.optJSONObject("initialState");
      if (initialState != null) {
        obj = mapper.readValue(initialState.toString(), Class.forName(json.getString("className")));
        return (Device) obj;
      }
    }
    obj = mapper.readValue("{}", Class.forName(json.getString("className")));
    return (Device) obj;
  }

  /**
   * Extracts list of named procedures from json
   *
   * @return list of named procedures
   */
  private static List<NamedProcedure> extractNamedProcedures(JSONObject json, Device device) {
    JSONArray arr = json.getJSONArray("userInterface");
    List<NamedProcedure> namedProcedures = new ArrayList<>();
    arr.forEach(e -> {
      String[] np = e.toString().split("->");
      try {
        Method method = device.getClass().getMethod(np[1].trim(), String.class, Object.class);
        namedProcedures.add(new NamedProcedure(np[0].trim(), (t, o) -> {
          try {
            method.invoke(device, t, o);
          } catch (IllegalAccessException e1) {
            e1.printStackTrace();
          } catch (InvocationTargetException e1) {
            e1.printStackTrace();
          } catch (IllegalArgumentException e1) {
            System.err.println(method.getName() + " was invoked with wrong number of arguments");
          }
        }));
      } catch (NoSuchMethodException e1) {
        e1.printStackTrace();
      }
    });
    NamedProcedure telemetryProcedure = extractTelemetryProcedure(json, device);
    if (telemetryProcedure != null) {
      namedProcedures.add(telemetryProcedure);
    }
    NamedProcedure stateChangeProcedure = extractStateChangeProcedure(json, device);
    if (stateChangeProcedure != null) {
      namedProcedures.add(stateChangeProcedure);
    }
    return namedProcedures;
  }

  private static JSONArray readJson(Path path) throws IOException {
    return new JSONArray(readFile(path, StandardCharsets.UTF_8));
  }

  public static List<File> findDirectoriesWithSameName(String name, File root, int max_depth) {
    List<File> result = new ArrayList<>();
    if (max_depth > 0) {
      for (File file : root.listFiles()) {
        if (file.isDirectory()) {
          if (file.getName().equals(name)) {
            result.add(file);
          }
          if (result.size() == 0) {
            result.addAll(findDirectoriesWithSameName(name, file, max_depth - 1));
          }
        }
      }
    }
    return result;
  }

  public static Map<String, Object> jsonToMap(JSONObject json) throws JSONException {
    Map<String, Object> retMap = new HashMap<String, Object>();

    if (json != JSONObject.NULL) {
      retMap = toMap(json);
    }
    return retMap;
  }

  public static Map<String, Object> toMap(JSONObject object) throws JSONException {
    Map<String, Object> map = new HashMap<String, Object>();

    Iterator<String> keysItr = object.keys();
    while (keysItr.hasNext()) {
      String key = keysItr.next();
      Object value = object.get(key);

      if (value instanceof JSONArray) {
        value = toList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = toMap((JSONObject) value);
      }
      map.put(key, value);
    }
    return map;
  }

  public static List<Object> toList(JSONArray array) throws JSONException {
    List<Object> list = new ArrayList<Object>();
    for (int i = 0; i < array.length(); i++) {
      Object value = array.get(i);
      if (value instanceof JSONArray) {
        value = toList((JSONArray) value);
      } else if (value instanceof JSONObject) {
        value = toMap((JSONObject) value);
      }
      list.add(value);
    }
    return list;
  }
}
