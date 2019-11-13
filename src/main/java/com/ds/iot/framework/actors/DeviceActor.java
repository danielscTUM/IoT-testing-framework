package com.ds.iot.framework.actors;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import com.ds.iot.framework.communication.GeneralMessage;
import com.ds.iot.framework.simulation.Device;
import com.ds.iot.framework.simulation.NamedProcedure;
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.List;


public class DeviceActor extends AbstractActor {

  final String deviceId;
  List<NamedProcedure> namedProcedures;
  private Device device;

  public DeviceActor(Device device) {
    this.deviceId = device.getDeviceId();
    this.namedProcedures = device.getCommonCommands();
    this.device = device;
  }

  public static Props props(Device device) {
    return Props.create(DeviceActor.class, () -> new DeviceActor(device));
  }

  private void matchPattern(String command, Object payload) {
    for(NamedProcedure n : this.namedProcedures) {
      if(command.matches(n.getPattern())) {
        try {
          Date date = new Date();
          device.logInfo(command);
          n.getProcedure().accept(command, payload);
        } catch (Exception e) {
          device.onError(e.getMessage());
          e.printStackTrace();
        }
        return;
      }
    }
    device.onError("Unknown command");
  }

  private void matchPattern(SimpleEntry<String, Object> message) {
    matchPattern(message.getKey(), message.getValue());
  }

  @Override
  public Receive createReceive() {
    ReceiveBuilder builder = receiveBuilder();
    builder = builder
        .match(SimpleEntry.class, c -> matchPattern(c.getKey().toString(), c.getValue()))
        .match(GeneralMessage.class, m -> matchPattern(device.handleMessage(m)))
        .matchAny(any -> matchPattern(device.handleCustomFormatMessage(any)));
    return builder.build();
  }
}