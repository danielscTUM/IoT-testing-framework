package com.ds.iot.framework.simulation;

import akka.actor.ActorRef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SimulatedUser {

  @Retention(RetentionPolicy.RUNTIME)
  public @interface UserMethod{
    String key();
  }

  public HashMap<String, Device> devices = new HashMap<>();

  private ActorRef userActor;

  public List<NamedProcedure> namedProcedures;

  private Scenario scenario = new Scenario("userScenario");

  public void setScenario(Scenario scenario) {
    this.scenario = scenario;
  }

  public SimulatedUser exec(String command) {
    this.scenario.exec(command);
    return this;
  }

  public SimulatedUser pause(long seconds) {
    this.scenario.pause(seconds);
    return this;
  }

  public void tell(String command, Object payload) {
    userActor.tell(new SimpleEntry<String, Object>(command, payload), null);
  }
}
