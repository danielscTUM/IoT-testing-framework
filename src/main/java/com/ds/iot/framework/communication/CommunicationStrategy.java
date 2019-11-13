package com.ds.iot.framework.communication;

import com.codahale.metrics.Timer;
import com.ds.iot.framework.simulation.Device;
import java.util.HashMap;
import java.util.function.BiConsumer;

public abstract class CommunicationStrategy {

  public void connectInternal(Device device) {
    final Timer.Context context = device.metrics.getResponses().time();
    this.connect(((connAck, throwable) -> {
      if (throwable != null) {
        device.metrics.getFailedConnections().inc();
        device.logError("Connection failure");
        ((Throwable)throwable).printStackTrace();
      } else {
        device.metrics.getResponseDuration().update(context.stop());
      }
    }));
  }

  public abstract void init(String url, Device device, HashMap params);

  public abstract void connect(BiConsumer<Object,Object> callback);

  public abstract  void send(String topic, Object message);

  public abstract void subscribe(String topic, Device d);

  public abstract void disconnect();

}
