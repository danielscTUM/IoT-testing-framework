package com.ds.iot.framework.communication;

import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.simulation.Device;
import java.util.HashMap;
import java.util.function.BiConsumer;
import lombok.Setter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@TestDriver.CommunicationStrategy(key="Websocket")
public class WebSocketCommunication extends CommunicationStrategy{

  private Device device;
  private String url;
  private StompSessionHandler1 stompSessionHandler = new StompSessionHandler1();
  public StompSession session;

  @Setter
  private class StompSessionHandler1 extends StompSessionHandlerAdapter {

    private BiConsumer<Object, Object> callback;

    @Override
    public void afterConnected(StompSession session1, StompHeaders connectedHeaders) {
      callback.accept(null, null);
      session = session1;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      if(payload != null) {
        device.tell(new GeneralMessage(payload.toString()));
      }
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
      System.err.println(exception);
    }
  }


  @Override
  public void init(String url, Device device, HashMap params) {
    this.url = url;
    this.device = device;
  }

  @Override
  public void connect(BiConsumer<Object,Object> callback) {
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient stompClient = new WebSocketStompClient(client);
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    stompSessionHandler.setCallback(callback);
    stompClient.connect(url, stompSessionHandler);
  }

  @Override
  public void send(String topic, Object message) {
    session.send(topic, message);
  }

  @Override
  public void subscribe(String topic, Device d) {
    session.subscribe(topic, stompSessionHandler);
  }

  @Override
  public void disconnect() {
    if(session != null) {
      session.disconnect();
    }
  }
}
