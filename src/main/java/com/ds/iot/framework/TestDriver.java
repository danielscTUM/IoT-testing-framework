package com.ds.iot.framework;

import static com.ds.iot.framework.simulation.UI.pod_number;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opentest4j.AssertionFailedError;

public class TestDriver {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface SimulationTest {

  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface CommunicationStrategy {
    String key();
  }

  public static JSONArray outputData = new JSONArray();
  public static HashMap<String, Class<com.ds.iot.framework.communication.CommunicationStrategy>> strategyHashMap = new HashMap();
  public static HashMap<String, Long> timeMap = new HashMap();

  public static void start(Class... classNames) {
    timeMap.put("test_start",System.nanoTime());
    int counter = -1;

    fillCommunicationStrategyMap();

    Map<String, String> tests = new HashMap();
    List<Method> allMethods = new ArrayList<>();
    for(Class className : classNames) {
      allMethods.addAll(Arrays.asList(className.getDeclaredMethods()));
    }
    for (final Method method : allMethods) {
      if (method.isAnnotationPresent(SimulationTest.class)) {
        System.out.println("Execute test " + method.getName());
        // SimulationTest annotation = method.getAnnotation(SimulationTest.class);
        try {
          counter++;
          timeMap.put("testcase_start",System.nanoTime());
          method.invoke(null);
          System.out.println("----------");
          timeMap.put("simulation_stop", System.nanoTime());
          if (pod_number == 0) {
            JSONObject test = (JSONObject) outputData.get(counter);
            if (test != null) {
              test.put("result", "positive");
            }
            System.out.println("Test successful");
          }
          // tests.put(method.getName(), "positiv");
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          if (e.getTargetException().getClass() == AssertionFailedError.class) {
            AssertionFailedError error = (AssertionFailedError) e.getTargetException();
            if (pod_number == 0) {
              JSONObject test = (JSONObject) outputData.get(counter);
              if (test != null) {
                test.put("result", "negative");
                test.put("errorMessage", error.getMessage());
              }
              System.out.println("Test failed: " + error.getMessage());
            }
            // tests.put(method.getName(), "negative");
          } else {
            e.printStackTrace();
          }
        }
      }
    }
    long endTime = System.nanoTime();
    timeMap.put("test_end", endTime);
    createOutput(outputData, endTime - timeMap.get("test_start"));
    File statusFile = new File("status");
    try {
      FileUtils.writeStringToFile(statusFile, "finished", Charset.forName("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (pod_number == 0 && System.getenv("TEST_ENV") != null) {
      while (true) {
        try {
          Thread.sleep(3000);
          File f = new File("finish");
          if(f.exists()) {
            System.exit(0);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    System.exit(0);
  }

  private static void fillCommunicationStrategyMap() {
    try (ScanResult result = new ClassGraph().enableClassInfo().enableAnnotationInfo().scan()) {
      ClassInfoList classInfos = result.getClassesWithAnnotation(CommunicationStrategy.class.getName());
      System.out.println("Communication Strategies: ");
      classInfos.stream().forEach(classInfo -> {
        Class classObj = classInfo.loadClass();
        String key = (String)classInfo.getAnnotationInfo(CommunicationStrategy.class.getName()).getParameterValues().getValue("key");
        strategyHashMap.put(key, classObj);
        System.out.println(" - key: " + key + " class: " + classObj.getName());
      });
    }
  }


  public static void createOutput(JSONArray json, long duration) {
    if (pod_number == 0) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("data", json);
      jsonObject.put("duration", duration);
      final StringBuilder jsStringBuilder = new StringBuilder();
      InputStream in = Main.class.getResourceAsStream("/templates/outputTemplate/data.js");
      BufferedReader reader = new BufferedReader(new InputStreamReader(in));
      reader.lines().forEach(str -> {
        jsStringBuilder.append(str);
      });
      try {
        String jsString = jsStringBuilder.toString();
        jsString = jsString.replace("$data", jsonObject.toString());
        InputStream is = Main.class.getResourceAsStream("/output.zip");
        is.transferTo(new FileOutputStream(new File("output.zip")));
        try {
          ZipFile zipFile = new ZipFile("output.zip");
          zipFile.extractAll("result");
        } catch (ZipException e) {
          e.printStackTrace();
        }
          FileUtils
              .writeStringToFile(new File("result/data.js"), jsString, Charset.forName("UTF-8"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
