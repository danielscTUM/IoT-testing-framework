package com.ds.iot.framework.communication;

import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.simulation.Device;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

@TestDriver.CommunicationStrategy(key="MQTT")
public class HiveMqtt extends CommunicationStrategy{

  private Mqtt5Client client;
  private String url;
  private Device device;

  @Override
  public void init(String url, Device device, HashMap params) {
    this.url = url;
    this.device = device;
    client = Mqtt5Client.builder().serverHost(this.url).build();
  }

  @Override
  public void connect(BiConsumer<Object,Object> callback) {
    long start = (new Date()).getTime();
    client.toAsync().connect().orTimeout(2000, TimeUnit.MILLISECONDS).whenCompleteAsync(callback);
  }

  @Override
  public void send(String topic, Object message) {
    if(client != null) {
      client.toAsync().publishWith().topic(topic).payload(message.toString().getBytes()).send();
    }
  }

  @Override
  public void subscribe(String topic, Device d) {
    if(client != null) {
      client.toAsync().subscribeWith()
          .topicFilter(topic)
          .qos(MqttQos.AT_LEAST_ONCE)
          .callback(msg -> {
            GeneralMessage message = new GeneralMessage(new String(msg.getPayloadAsBytes()));
            device.tell(message);
          })
          .send();
    }
  }

  @Override
  public void disconnect() {
    if(client != null) {
      client.toAsync().disconnect();
    }
  }
}
