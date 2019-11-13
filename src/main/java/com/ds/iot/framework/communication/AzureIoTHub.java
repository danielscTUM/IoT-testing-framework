package com.ds.iot.framework.communication;

import com.ds.iot.framework.TestDriver;
import com.ds.iot.framework.exceptions.TechnicalException;
import com.ds.iot.framework.simulation.Device;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodData;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.service.RegistryManager;
import com.microsoft.azure.sdk.iot.service.auth.AuthenticationType;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.function.BiConsumer;

@TestDriver.CommunicationStrategy(key = "Azure")
public class AzureIoTHub extends CommunicationStrategy {

  String iotHubConnectionString;
  String devicePrimaryKey = "";
  DeviceClient client;
  Device device;
  static String ioTDevicePrefix = "iot-testing-framework-";
  boolean isNewDevice;

  @Override
  public void init(String url, Device device, HashMap params) {
    this.device = device;
    if (params.containsKey("iotHubConnectionString")) {
      iotHubConnectionString = (String) params.get("iotHubConnectionString");
    } else {
      throw new TechnicalException(
          "Missing parameter in Azure connection definition: 'iotHubConnectionString'");
    }

    if (params.containsKey("iotDevicePrefix")) {
      ioTDevicePrefix = (String) params.get("iotDevicePrefix");
    }

    String connectionString;
    if (params.containsKey("deviceConnectionString")) {
      connectionString = (String) params.get("deviceConnectionString");
      isNewDevice = false;
    } else {
      createDevice(device.getDeviceId());
      connectionString = "HostName=" + url + ";DeviceId=" + ioTDevicePrefix + device.getDeviceId()
          + ";SharedAccessKey=" + devicePrimaryKey;
      isNewDevice = true;
    }

    try {
      this.client = new DeviceClient(connectionString, IotHubClientProtocol.MQTT);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }

    MessageCallback callback = new MessageCallbackMqtt();
    client.setMessageCallback(callback, device);

    System.out.println("Successfully created an IoT Hub client.");
  }

  @Override
  public void connect(BiConsumer<Object, Object> callback) {
    long time = 3600;
    client.setOption("SetSASTokenExpiryTime", time);
    client.registerConnectionStatusChangeCallback(new IotHubConnectionStatusChangeCallbackLogger(),
        new Object());

    try {
      client.open();
      callback.accept(null, null);
      SampleDeviceMethodCallback deviceMethodCallback = new SampleDeviceMethodCallback();
      try {
        client
            .subscribeToDeviceMethod(deviceMethodCallback, device, new DeviceMethodStatusCallBack(),
                null);
      } catch (IOException e) {
        e.printStackTrace();
      }

    } catch (IOException e) {
      callback.accept(null, e);
    }
  }

  @Override
  public void send(String topic, Object message) {
    GeneralMessage generalMessage = (GeneralMessage) message;
    Message azureMessage = new Message(generalMessage.getPayload());
    azureMessage.setMessageId(topic);

    for (MessageProperty property : generalMessage.getProperties()) {
      azureMessage.setProperty(property.getName(), property.getValue());
    }

    EventCallback callback = new EventCallback();
    client.sendEventAsync(azureMessage, callback, azureMessage);
  }

  @Override
  public void subscribe(String topic, Device d) {

  }

  @Override
  public void disconnect() {
    try {
      if (isNewDevice) {
        deleteDevice(ioTDevicePrefix + this.device.getDeviceId());
      }
      client.closeNow();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void createDevice(String id) {
    try {
      RegistryManager registryManager = RegistryManager
          .createFromConnectionString(iotHubConnectionString);
      com.microsoft.azure.sdk.iot.service.Device mydevice = com.microsoft.azure.sdk.iot.service.Device
          .createDevice(ioTDevicePrefix + id, AuthenticationType.SAS);
      mydevice.getPrimaryKey();
      try {
        com.microsoft.azure.sdk.iot.service.Device d = registryManager.addDevice(mydevice);
        devicePrimaryKey = d.getPrimaryKey();
      } catch (Exception er) {
        er.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  void deleteDevice(String id) {
    try {
      RegistryManager registryManager = RegistryManager
          .createFromConnectionString(iotHubConnectionString);
      com.microsoft.azure.sdk.iot.service.Device mydevice = registryManager.getDevice(id);
      registryManager.removeDevice(mydevice);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (IotHubException e) {
      e.printStackTrace();
    }

  }

  protected static class MessageCallbackMqtt implements
      com.microsoft.azure.sdk.iot.device.MessageCallback {

    public IotHubMessageResult execute(Message msg, Object context) {
      GeneralMessage message = new GeneralMessage(new String(msg.getBytes()));
      for (MessageProperty property : msg.getProperties()) {
        message.setProperty(property.getName(), property.getValue());
      }
      ((Device) context)
          .tell(message);
      return IotHubMessageResult.COMPLETE;
    }
  }

  protected static class IotHubConnectionStatusChangeCallbackLogger implements
      IotHubConnectionStatusChangeCallback {

    @Override
    public void execute(IotHubConnectionStatus status,
        IotHubConnectionStatusChangeReason statusChangeReason, Throwable throwable,
        Object callbackContext) {
      if (throwable != null) {
        throwable.printStackTrace();
      }
    }
  }

  protected static class SampleDeviceMethodCallback implements
      com.microsoft.azure.sdk.iot.device.DeviceTwin.DeviceMethodCallback {

    @Override
    public DeviceMethodData call(String methodName, Object methodData, Object context) {
      ((Device) context).tell(methodName);

      return new DeviceMethodData(200, "Successful");
    }
  }

  protected static class EventCallback implements IotHubEventCallback {

    public void execute(IotHubStatusCode status, Object context) {
    }
  }

  protected static class DeviceMethodStatusCallBack implements IotHubEventCallback {

    public void execute(IotHubStatusCode status, Object context) {
    }
  }
}
