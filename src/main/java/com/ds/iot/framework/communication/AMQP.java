package com.ds.iot.framework.communication;

import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.simulation.Device;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;

@TestDriver.CommunicationStrategy(key = "AMQP")
public class AMQP extends CommunicationStrategy {

  private String url;
  private Channel channel;
  private Device device;
  private ConnectionFactory factory;
  private static final String EXCHANGE_NAME = "topic_logs";

  @Override
  public void init(String url, Device device, HashMap params) {
    this.url = url;
    this.device = device;
    factory = new ConnectionFactory();
    factory.setHost(this.url);
  }

  @Override
  public void connect(BiConsumer<Object, Object> callback) {
    try {
      long start = (new Date()).getTime();
      Connection connection = factory.newConnection();
      channel = connection.createChannel();
      channel.exchangeDeclare(EXCHANGE_NAME, "topic");
      callback.accept(null, null);
    } catch (IOException | TimeoutException e) {
      callback.accept(null, e);
    }
  }

  @Override
  public void send(String topic, Object message) {
    try {

      String routingKey = topic;
      channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.toString().getBytes("UTF-8"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void subscribe(String topic, Device d) {

    try {
      channel.exchangeDeclare(EXCHANGE_NAME, "topic");
      String queueName = channel.queueDeclare().getQueue();

      channel.queueBind(queueName, EXCHANGE_NAME, topic);

      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        GeneralMessage message = new GeneralMessage(new String(delivery.getBody()));
        device.tell(message);
      };
      channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void disconnect() {

  }
}
