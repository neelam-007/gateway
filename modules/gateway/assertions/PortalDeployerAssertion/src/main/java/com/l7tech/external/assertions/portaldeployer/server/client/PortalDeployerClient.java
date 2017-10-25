package com.l7tech.external.assertions.portaldeployer.server.client;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Created by BAGJO04 on 2017-10-11.
 */
public class PortalDeployerClient implements MqttCallback {
  private static final Logger logger = Logger.getLogger(PortalDeployerClient.class.getName());
  private SSLSocketFactory sslSocketFactory;
  private MqttClient mqttClient;
  private MqttClientPersistence mqttClientPersistence;
  private MqttConnectOptions mqttConnectOptions;
  private String mqttBrokerUri;
  private String clientId;
  private String topic;
  private int connectionTimeout = 60;
  private int keepAliveInterval = 30;
  private boolean cleanSession = true;

  public PortalDeployerClient(String mqttBrokerUri, String clientId, String topic, int connectionTimeout, int keepAliveInterval, SSLSocketFactory sslSocketFactory) throws
          PortalDeployerClientException {
    logger.log(Level.INFO, String.format("mqttBrokerUri [%s], " + "clientId [%s], " + "topic [%s]", mqttBrokerUri, clientId, topic));
    this.sslSocketFactory = sslSocketFactory;
    this.mqttBrokerUri = mqttBrokerUri;
    this.clientId = clientId;
    this.topic = topic;
    mqttClientPersistence = new MemoryPersistence();
    mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setConnectionTimeout(this.connectionTimeout);
    mqttConnectOptions.setKeepAliveInterval(this.keepAliveInterval);
    mqttConnectOptions.setCleanSession(false);
    mqttConnectOptions.setSocketFactory(this.sslSocketFactory);
    try {
      mqttClient = new MqttClient(this.mqttBrokerUri, this.clientId, this.mqttClientPersistence);
    } catch (MqttException e) {
      throw new PortalDeployerClientException(e.getMessage(), e);
    }
    mqttClient.setCallback(this);
  }

  public void stopClient() {
    if (mqttClient.isConnected()) {
      try {
        mqttClient.disconnect();
        logger.log(Level.INFO, String.format("Successfully disconnected from Broker: %s", mqttBrokerUri));
      } catch (MqttException e) {
        logger.log(Level.SEVERE, "Unable to disconnect client", e);
      }
    }
  }

  public void startClient() {
    try {
      mqttClient.connect(mqttConnectOptions);
      logger.log(Level.INFO, String.format("Successfully connected to Broker: %s", mqttBrokerUri));
      mqttClient.subscribe(topic, 1);
      logger.log(Level.INFO, String.format("Subscribing to Topic: %s", topic));
    } catch (MqttException e) {
      logger.log(Level.SEVERE, "Exception thrown in startMqttClientThread", e);
    }
  }

  public void sendMessage(String message) {
    if (mqttClient.isConnected()) {
      try {
        MqttMessage mqttMessage = new MqttMessage(("test" + new Random().nextInt()).getBytes());
        mqttMessage.setQos(1);
        mqttMessage.setRetained(false);
        //mqttClient.publish("apim/d790dc77-fad8-11e6-9a60-0242ac11000f/deploy", mqttMessage);
        mqttClient.publish("moo", mqttMessage);
      } catch (MqttException e) {
        logger.log(Level.SEVERE, "Exception thrown in startMqttClientThread", e);
      }
    } else {
      logger.log(Level.WARNING, "Can't send message since client isn't connected");
    }
  }

  @Override
  public void connectionLost(Throwable cause) {
    logger.log(Level.WARNING, "Exception thrown in startMqttClientThread", cause);
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    logger.log(Level.INFO, String.format("Topic: %s, Message: %s", topic, new String(message.getPayload())));
    //mqttClient.publish(topic + "/received", new MqttMessage("message recieved".getBytes()));
    //consume message, get bundle, post bundle, put postback
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    //logger.log(Level.INFO, String.format("Topic: %s, Message: %s", topic, new String(message.getPayload())));
  }

  public String getMqttBrokerUri() {
    return mqttBrokerUri;
  }

  public void setMqttBrokerUri(String mqttBrokerUri) {
    this.mqttBrokerUri = mqttBrokerUri;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  @Override
  public String toString() {
    return "PortalDeployerClient{" + "mqttBrokerUri='" + mqttBrokerUri + '\'' + ", clientId='" + clientId + '\'' + "," +
            "" + " topic='" + topic + '\'' + '}';
  }
}
