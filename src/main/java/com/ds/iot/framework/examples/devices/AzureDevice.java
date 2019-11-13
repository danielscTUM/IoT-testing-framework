package com.ds.iot.framework.examples.devices;

import com.ds.iot.framework.communication.GeneralMessage;
import com.ds.iot.framework.simulation.Device;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
public class AzureDevice extends Device {

  double minTemperature = 20;
  double minHumidity = 60;
  double temperature;
  double humidity;
  String correlationId;

  Random rand = new Random();

  public void stateChange() {
    temperature = minTemperature + rand.nextDouble() * 15;
    humidity = minHumidity + rand.nextDouble() * 20;
  }

  public void requestPairingTan(String command, Object roleId) {
    correlationId = UUID.randomUUID().toString();

    GeneralMessage message = new GeneralMessage("{'roleId': '" + roleId + "' }");
    message.setProperty("methodName", "RequestPairingTan");
    message.setProperty("correlation-id", correlationId);

    getCommunicationStrategy().send(correlationId, message);
  }

  public void receivePairingTan(String message, Object data) {
    String correlationIdFromProperties = ((GeneralMessage) data).getProperty("correlation-id");

    JSONObject requestData = new JSONObject(((GeneralMessage) data).getPayload());

    String tan = requestData.getString("tan");

    if (!correlationIdFromProperties.equals(correlationId)) {
      logError("CorrelationIdDoesNotMatch");
    } else if (tan == null) {
      logError("TanResponseWithoutTan");
    } else {
      getUser().tell("receivePairingTan", tan);
    }
  }

  /*public SimpleEntry<String, Object> handleMessage(GeneralMessage message) {
    return new SimpleEntry<>(message.getProperty("methodName"), message);
  }*/

  @Override
  protected void sendTelemetry(String messageString) {
    JSONObject messageJson = new JSONObject(messageString);
    GeneralMessage message = new GeneralMessage(messageString);
    message.setProperty("temperatureAlert", messageJson.getInt("temperature") > 30 ? "true" : "false");
    getCommunicationStrategy().send(" ", message);
  }
}
