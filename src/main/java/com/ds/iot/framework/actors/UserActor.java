package com.ds.iot.framework.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.ds.iot.framework.exceptions.TechnicalException;
import com.ds.iot.framework.simulation.NamedProcedure;
import com.ds.iot.framework.simulation.SimulatedUser;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import lombok.Getter;

@Getter
public class UserActor extends AbstractActor {

  List<NamedProcedure> namedProcedures;
  private SimulatedUser user;

  public UserActor(SimulatedUser user) {
    this.user = user;
    this.namedProcedures = user.getNamedProcedures();
  }

  public static Props props(SimulatedUser user) {
    return Props.create(UserActor.class, () -> new UserActor(user));
  }

  private void matchPattern(String command, Object payload) {
    for(NamedProcedure n : this.namedProcedures) {
      if(command.matches(n.getPattern())) {
        try {
          n.getProcedure().accept(command, payload);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return;
      }
    }
    throw new TechnicalException("Simulated User received unknown command: " + command);
  }

  private void handleUnknownFormat() {
    throw new Error("User actor received message with wrong format");
  }

  @Override
  public Receive createReceive() {
    ReceiveBuilder builder = receiveBuilder();
    builder = builder
        .match(SimpleEntry.class, c -> matchPattern(c.getKey().toString(), c.getValue()))
        .matchAny(unknown -> handleUnknownFormat());
    return builder.build();
  }
}
