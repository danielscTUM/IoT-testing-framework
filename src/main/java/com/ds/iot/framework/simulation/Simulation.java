package com.ds.iot.framework.simulation;


import static org.junit.jupiter.api.Assertions.assertEquals;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.actors.DeviceActor;
import com.ds.iot.framework.actors.UserActor;
import com.ds.iot.framework.metrics.Metrics;
import com.ds.iot.framework.simulation.SimulatedUser.UserMethod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opentest4j.AssertionFailedError;
import redis.clients.jedis.Jedis;

@Getter
@Setter
public class Simulation {

  private ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(10);
  private ActorSystem actorSystem = ActorSystem.create("system");
  private LinkedHashMap<String, SimulatedDevice> deviceMap = new LinkedHashMap<>();
  private List<SimulatedUser> userList = new ArrayList<>();
  MessageLog infoLog = new MessageLog();
  MessageLog errorLog = new MessageLog();
  private NoArgumentFunction checkExpression;
  Long pod_number = Long.parseLong(Optional.ofNullable(System.getenv("POD_NUMBER")).orElse("0"));
  Long pod_count = Long.parseLong(Optional.ofNullable(System.getenv("NUMBER_OF_PODS")).orElse("1"));
  String test_env = System.getenv("TEST_ENV");
  Jedis jedis;
  Date simulationStart;
  long startTime;
  private Metrics metrics = new Metrics();
  private String simulationName;

  public static Simulation simulation() {
    return new Simulation();
  }


  /**
   * Runs the simulation
   *
   * @param intendedDuration duration of the simulation in seconds
   * @return simulation object
   */
  public Simulation start(int intendedDuration) {
    System.out.println("Start simulation");
    System.out.println(pod_count);
    System.out.println(pod_number);
    System.out.println(test_env);
    simulationName = Thread.currentThread().getStackTrace()[2].getMethodName();
    System.out.println(simulationName);
    if (test_env != null) {
      jedis = new Jedis("redis-master");
      if(pod_number == 0){
        jedis.del(simulationName + "_log_counter");
      }
      jedis.incr(simulationName + "_counter");
      while (Integer.parseInt(jedis.get(simulationName + "_counter")) < pod_count) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
    SimpleEntry<List<ScheduledTask>,Long> taskList = createTaskList();
    List<ScheduledTask> tasks = taskList.getKey();
    /*Collections.sort(tasks, (a, b) -> (int)(a.getDelay() - b.getDelay()));
    for(ScheduledTask task : tasks) {
      System.out.println(task);
    }*/
    Long maxTimer = taskList.getValue();

    startTime = System.nanoTime();
    simulationStart = new Date();
    for (ScheduledTask task : tasks) {
      this.executorService.schedule(task.getTask(), task.getDelay(), TimeUnit.MILLISECONDS);
    }

    this.executorService.schedule(() -> {
      stop();
    }, Math.max(intendedDuration, maxTimer + 1000), TimeUnit.MILLISECONDS);

    try {
      this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    System.out.println("Stop communication");
    deviceMap.forEach((key, device) -> {
      System.out.println("Disconnect");
      device.getDevice().getCommunicationStrategy().disconnect();
      infoLog.addAll(device.getDevice().getInfoLog(), device.getDevice().getDeviceId());
      errorLog.addAll(device.getDevice().getErrorLog(), device.getDevice().getDeviceId());
    });

    //create Map with number of device tags
    HashMap<String, Long> tags = new HashMap<>();
    deviceMap.forEach((key, value) -> {
      String tag = value.getDevice().getTag();
      if (tags.containsKey(tag)) {
        tags.put(tag, tags.get(tag) + 1);
      } else {
        tags.put(tag, 1L);
      }
    });

    if (test_env != null) {
      System.out.println("Store pod info");
      storePodInfo(tags);

      if (pod_number == 0) {
        loadPodInfos(tags);
      }
    }
    if (actorSystem != null) {
      actorSystem.terminate();
    }
    infoLog.log.sort(Comparator.comparing(LogMessage::getDate));
    errorLog.log.sort(Comparator.comparing(LogMessage::getDate));
    long simulationEnd = System.nanoTime();
    long duration = simulationEnd - startTime;
    long preparationDuration = startTime - TestDriver.timeMap.get("testcase_start");
    if (pod_number == 0) {
      createStatistics(duration, preparationDuration, tags);
    }
    return this;
  }

  private SimpleEntry<List<ScheduledTask>,Long> createTaskList() {
    List<ScheduledTask> tasks = new ArrayList<>();
    List<Long> maxTimer = Arrays.asList(0L);
    this.deviceMap.forEach((k, v) -> {
      long timer = (long) (v.getDelay() * 1000);
      if(v.getScenario() != null) {
        for (Command command : v.getScenario().getExecution()) {
          if (command instanceof Pause) {
            timer += ((Pause) command).duration * 1000;
          } else {
            tasks.add(new ScheduledTask(() -> {
              v.getDevice().tell(command.name);
            }, timer));
            timer += 100;
          }
        }
      }
      long max = Math.max(maxTimer.get(0), timer);
      maxTimer.set(0, max);
    });

    this.userList.forEach(user -> {
      long timer = 0L;
      for (Command command : user.getScenario().getExecution()) {
        if (command instanceof Pause) {
          timer += ((Pause) command).duration * 1000;
        } else {
          tasks.add(new ScheduledTask(() -> {
            user.tell(command.name, null);
          }, timer));
          timer += 100;
        }
      }
    });
    return new SimpleEntry<>(tasks, maxTimer.get(0));
  }

  private void loadPodInfos(HashMap<String, Long> tags) {
    System.out.println("Load pod infos");
    ObjectMapper objectMapper = new ObjectMapper();
    jedis.del(simulationName + "_counter");
    while (Integer.parseInt(jedis.get(simulationName + "_log_counter")) < pod_count) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        System.out.println("Error while sleep");
        e.printStackTrace();
      }
    }
    jedis.del(simulationName + "_log_counter");
    for (int i = 1; i < pod_count; i++) {
      try {
        infoLog.joinLog(objectMapper.readValue(jedis.get(i + "_info"), MessageLog.class));
        errorLog.joinLog(objectMapper.readValue(jedis.get(i + "_error"), MessageLog.class));
        HashMap<String, Long> loadedTags = objectMapper
            .readValue(jedis.get(i + "_deviceMap"), new TypeReference<HashMap<String, Long>>() {
            });
        JSONObject podMetric = new JSONObject(jedis.get(i + "_metrics"));
        metrics.merge(podMetric);
        jedis.del(i + "_info");
        jedis.del(i + "_error");
        jedis.del(i + "_deviceMap");
        jedis.del(i + "_metrics");
        loadedTags.forEach((key, value) -> {
          if (tags.containsKey(key)) {
            tags.put(key, tags.get(key) + value);
          } else {
            tags.put(key, value);
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void storePodInfo(HashMap<String, Long> tags) {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      jedis.set(pod_number + "_info", objectMapper.writeValueAsString(infoLog));
      jedis.set(pod_number + "_error", objectMapper.writeValueAsString(errorLog));
      jedis.set(pod_number + "_deviceMap", objectMapper.writeValueAsString(tags));
      jedis.set(pod_number + "_metrics", objectMapper.writeValueAsString(metrics));
      Thread.sleep(1000); // make sure that the values have been set
      jedis.incr(simulationName + "_log_counter");
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void stop() {
    this.executorService.shutdown();
    System.out.println("---------------------------");
  }

  public interface NoArgumentFunction {

    public void op(MessageLog l);
  }

  public Simulation checkMessageInterval(String message, int max, int min) {
    LinkedHashMap<String, Object> filteredLog = infoLog.getLogMap().entrySet().stream()
        .filter((e) -> e.getKey().matches(message))
        .collect(
            LinkedHashMap::new,                                   // Supplier
            (map, item) -> map.put(item.getKey(), item.getValue()),          // Accumulator
            Map::putAll);
    Object[] filteredArr = filteredLog.values().toArray();
    for (int i = 0; i < filteredArr.length - 1; i++) {
      long timeDiff =
          ((LogMessage) filteredArr[i + 1]).getDate().getTime() - ((LogMessage) filteredArr[i])
              .getDate().getTime();
      if (timeDiff > max || timeDiff < min) {
        throw new AssertionFailedError("The interval between the messages of type " + message +
            " was not in the interval of [" + min + ", " + max + "] but " + timeDiff);
      }
    }
    return this;
  }

  public Simulation checkMessageResponse(String messageA, String messageB, String idA, String idB,
      int maxDelay) {
    LinkedHashMap<String, Object> filteredLogA = infoLog.getDeviceLogs().getOrDefault(idA, new LinkedHashMap<String, List<LogMessage>>()).entrySet().stream()
        .filter((e) -> e.getKey().matches(messageA))
        .collect(
            LinkedHashMap::new,
            (map, item) -> map.put(item.getKey(), item.getValue()),
            Map::putAll);
    LinkedHashMap<String, Object> filteredLogB = infoLog.getDeviceLogs().getOrDefault(idB, new LinkedHashMap<>()).entrySet().stream()
        .filter((e) -> e.getKey().matches(messageB))
        .collect(
            LinkedHashMap::new,
            (map, item) -> map.put(item.getKey(), item.getValue()),
            Map::putAll);

    Object[] filteredArrA = filteredLogA.values().toArray();
    Object[] filteredArrB = filteredLogB.values().toArray();
    for (int i = 0; i < filteredArrA.length; i++) {
      LogMessage logMessageA = (LogMessage) filteredArrA[i];
      long minTime = logMessageA.getDate().getTime();
      long maxTime = logMessageA.getDate().getTime() + maxDelay;
      boolean responseExists = false;
      for (int k = 0; i < filteredArrB.length; k++) {
        LogMessage logMessageB = (LogMessage) filteredArrB[k];
        long logMessageBTime = logMessageB.getDate().getTime();
        if (logMessageBTime >= minTime && logMessageBTime <= maxTime) {
          responseExists = true;
          break;
        }
      }
      if (!responseExists) {
        throw new AssertionFailedError(
            "A message of type " + messageA + " was not followed by a message of type " + messageB +
                " with a maximal delay of " + maxDelay + " seconds");
      }
    }
    return this;
  }

  public Simulation checkLogContainsMessage(String message) {
    return checkLog(log -> {assertEquals(true, log.contains(message), "Log did not contain message: '" + message + "'");});
  }

  private Simulation checkLog(NoArgumentFunction f) {
    if (pod_number == 0) {
      f.op(this.infoLog);
    }
    return this;
  }

  public Simulation checkMedianConnectionDuration(int milliseconds) {
    if(pod_number == 0) {
      assertEquals(metrics.getResponseDuration().getSnapshot().getMedian() <= milliseconds, true);
    }
    return this;
  }

  public Simulation checkErrors() {
    if (pod_number == 0) {
      assertEquals(0, errorLog.size());
    }
    return this;
  }

  public Simulation setUserDeviceGroups(List<SimulatedUser>... users) {
    for(List<SimulatedUser> userList : users) {
      for(SimulatedUser user : userList) {
        connectUserToSimulation(user);
      }
    }
    return this;
  }

  public Simulation setUserDeviceGroups(SimulatedUser... users) {
    for(SimulatedUser user : users) {
      connectUserToSimulation(user);
    }
    return this;
  }

  private void connectUserToSimulation(SimulatedUser user) {
    user.setNamedProcedures(extractUserMethods(user));
    ActorRef userActor = actorSystem.actorOf(UserActor.props(user));
    user.setUserActor(userActor);
    user.getDevices().forEach((k,d) -> {
      connectDeviceToSimulation(d);
      deviceMap.put(d.getDeviceId(), new SimulatedDevice(d, 0, null));
    });
    userList.add(user);
  }

  private List<NamedProcedure> extractUserMethods(SimulatedUser user) {
    List<NamedProcedure> result = new ArrayList<>();
    List<Method> allMethods = new ArrayList<>();
    allMethods.addAll(Arrays.asList(user.getClass().getDeclaredMethods()));

    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(UserMethod.class)) {
        String key = method.getAnnotation(UserMethod.class).key();
        result.add(new NamedProcedure(key, (c,o) -> {
          try {
            method.invoke(user, c, o);
          } catch (IllegalAccessException e) {
            e.printStackTrace();
          } catch (InvocationTargetException e) {
            e.printStackTrace();
          }
        }));
      }
    }
    return result;
  }

  private void connectDeviceToSimulation(Device d) {
    ActorRef deviceActor = actorSystem.actorOf(DeviceActor.props(d));
    d.setDeviceActor(deviceActor);
    d.setExecutor(this.executorService);
    d.metrics = metrics;
  }


  public Simulation setDevices(List<SimulatedDevice>... deviceLists) {
    LinkedHashMap<String, SimulatedDevice> deviceMap = new LinkedHashMap<>();
    for (List<SimulatedDevice> devices : deviceLists) {
      devices.forEach(d -> {
        connectDeviceToSimulation(d.getDevice());
        deviceMap.put(d.getDevice().getDeviceId(), d);
      });
    }
    this.deviceMap = deviceMap;
    return this;
  }

  public void createStatistics(long duration, long preparationDuration, HashMap<String, Long> tagMap) {
      JSONObject test = new JSONObject();
      JSONObject tags = new JSONObject();
      JSONArray infoLogMessages = createLogJsonArray(infoLog.log);
      JSONArray errorLogMessages = createLogJsonArray(errorLog.log);
      AtomicInteger deviceCount = new AtomicInteger(0);
      JSONObject connections = new JSONObject();
      tagMap.forEach((key, value) -> {
        tags.put(key, value);
        deviceCount.addAndGet(value.intValue());
      });
      test.put("infoLog", infoLogMessages);
      test.put("errorLog", errorLogMessages);
      test.put("name", simulationName);
      test.put("duration", duration);
      test.put("setupDuration", preparationDuration);
      test.put("tagMap", tags);
      test.put("deviceCount", deviceCount);
      connections.put("count", metrics.getResponseDuration().getCount());
      connections.put("failedConnections", metrics.getFailedConnections().getCount());
      connections.put("meanRate", metrics.getResponses().getMeanRate());
      connections.put("q1Duration", metrics.getResponseDuration().getSnapshot().getValue(0.25));
      connections.put("q3Duration", metrics.getResponseDuration().getSnapshot().getValue(0.75));
      connections.put("maxDuration", metrics.getResponseDuration().getSnapshot().getMax());
      connections.put("medianDuration", metrics.getResponseDuration().getSnapshot().getMedian());
      connections.put("minDuration", metrics.getResponseDuration().getSnapshot().getMin());
      test.put("connections", connections);

      TestDriver.outputData.put(test);
  }

  private JSONArray createLogJsonArray(List<LogMessage> logs) {
    JSONArray logMessages = new JSONArray();
    Arrays.stream(logs.toArray()).forEach(m -> {
      JSONObject obj = new JSONObject();
      LogMessage message = (LogMessage) m;
      obj.put("message", message.getMessage());
      obj.put("deviceId", message.getDeviceId());
      obj.put("time", message.getDate().getTime() - simulationStart.getTime());
      logMessages.put(obj);
    });
    return logMessages;
  }
}