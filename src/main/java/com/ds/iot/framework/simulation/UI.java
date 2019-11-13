package com.ds.iot.framework.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class UI {

  public static int count = 0;
  public static Long pod_number = Long
      .parseLong(Optional.ofNullable(System.getenv("POD_NUMBER")).orElse("0"));

  public static Scenario scenario(String name) {
    return new Scenario(name);
  }

  public static List<SimulatedDevice> atOnce(List<Device> devices, Scenario scenario) {
    List<SimulatedDevice> simulatedDevices = new ArrayList<>();
    for (int i = 0; i < devices.size(); i++) {
      if (devices.get(i).getDeviceId() == null) {
        count += 1;
      }
      simulatedDevices.add(new SimulatedDevice(devices.get(i), 0, scenario));
    }
    return simulatedDevices;
  }

  public static SimulatedUser userDeviceGroup(SimulatedUser user, List<Device>... devices) {
    for (List<Device> device : devices) {
      user.devices.put(device.get(0).getDeviceId(), device.get(0));
      device.get(0).setUser(user);
    }
    return user;
  }

  public static List<SimulatedUser> userDeviceGroup(Class<? extends SimulatedUser> userClass,
      Scenario scenario, String... devicePaths) {
    return userDeviceGroup(1, userClass, scenario, devicePaths);
  }

  public static List<SimulatedUser> userDeviceGroup(int number, Class<? extends SimulatedUser> userClass,
      Scenario scenario, String... devicePaths) {
    List<SimulatedUser> users = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      try {
        SimulatedUser user = userClass.getDeclaredConstructor().newInstance();

        for (String devicePath : devicePaths) {
          Device device = JsonExtractor.buildDevice(devicePath, "").get(0);
          device.setDeviceId(device.getTag() + i);
          user.devices.put(device.getTag(), device);
          user.setScenario(scenario);
          device.setUser(user);
        }
        users.add(user);
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
    return users;
  }

  public static List<SimulatedDevice> ramp(List<Device> devices, long seconds, Scenario scenario) {
    List<SimulatedDevice> simulatedDevices = new ArrayList<>();
    for (int i = 0; i < devices.size(); i++) {
      if (devices.get(i).getDeviceId() == null) {
        count += 1;
      }
      simulatedDevices
          .add(new SimulatedDevice(devices.get(i), (seconds * 1.0) / devices.size() * i, scenario));
    }
    return simulatedDevices;
  }

  public static List<SimulatedDevice> ramp(int number, String json, Class<? extends Device> cls,
      long seconds, Scenario scenario) {
    ObjectMapper mapper = new ObjectMapper();
    List<Device> devices = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      try {
        Device obj = mapper.readValue(json, cls);
        devices.add(obj);
        count += 1;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return ramp(devices, seconds, scenario);
  }

  public static Map<String, String> getURLParams(String command)
      throws URISyntaxException {
    Map<String, String> paramsMap = new HashMap<>();
    List<NameValuePair> params = URLEncodedUtils.parse(new URI(command), Charset.forName("UTF-8"));
    params.forEach(p -> paramsMap.put(p.getName(), p.getValue()));
    return paramsMap;
  }
}
