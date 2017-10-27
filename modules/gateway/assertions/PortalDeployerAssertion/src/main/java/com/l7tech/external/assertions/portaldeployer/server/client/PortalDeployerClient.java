package com.l7tech.external.assertions.portaldeployer.server.client;

import com.l7tech.external.assertions.portaldeployer.ExponentialIntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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
  private MqttAsyncClient mqttClient;
  private MqttClientPersistence mqttClientPersistence;
  private MqttConnectOptions mqttConnectOptions;
  private String mqttBrokerUri;
  private String clientId;
  private String topic;
  private int connectionTimeout;
  private int keepAliveInterval;

  private ExponentialIntSupplier sleepIntervalSupplier;

  private int qosLevel = 1;

  // boolean used to stop client if it is stuck connecting to the broker
  private Boolean isRunning = false;
  // How long to sleep in seconds
  private int sleepIntervalOnReconnect = 30;

  public PortalDeployerClient(String mqttBrokerUri, String clientId, String topic, int connectionTimeout, int keepAliveInterval, SSLSocketFactory sslSocketFactory) throws
          PortalDeployerClientException {
    initialize(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, sslSocketFactory);
    try {
      mqttClient = new MqttAsyncClient(this.mqttBrokerUri, this.clientId, this.mqttClientPersistence);
    } catch (MqttException e) {
      throw new PortalDeployerClientException(e.getMessage(), e);
    }
    mqttClient.setCallback(this);
  }

  // For testing
  PortalDeployerClient(String mqttBrokerUri, String clientId, String topic, int connectionTimeout, int keepAliveInterval, SSLSocketFactory sslSocketFactory, MqttAsyncClient mqttAsyncClient) {
    initialize(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, sslSocketFactory);
    mqttClient = mqttAsyncClient;
    mqttClient.setCallback(this);
  }

  private void initialize(String mqttBrokerUri, String clientId, String topic, int connectionTimeout, int keepAliveInterval, SSLSocketFactory sslSocketFactory) {
    logger.log(Level.INFO, String.format("mqttBrokerUri [%s], " + "clientId [%s], " + "topic [%s]", mqttBrokerUri, clientId, topic));
    this.sslSocketFactory = sslSocketFactory;
    this.mqttBrokerUri = mqttBrokerUri;
    this.clientId = clientId;
    this.topic = topic;
    this.connectionTimeout = connectionTimeout;
    this.keepAliveInterval = keepAliveInterval;

    mqttClientPersistence = new MemoryPersistence();
    mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setConnectionTimeout(this.connectionTimeout);
    mqttConnectOptions.setKeepAliveInterval(this.keepAliveInterval);
    mqttConnectOptions.setCleanSession(false);
    mqttConnectOptions.setSocketFactory(this.sslSocketFactory);

    // This will return increasingly larger sleep intervals,
    // so a short connection blip will recover fast,
    // but not hammer the server if it is a larger disconnect window
    sleepIntervalSupplier = new ExponentialIntSupplier(0, sleepIntervalOnReconnect);
  }

  public void start() throws PortalDeployerClientException {
    isRunning = true;
    if(!mqttClient.isConnected()) {
      connect();
    }
  }

  public void stop() {
    isRunning = false;
    if (mqttClient.isConnected()) {
      disconnect();
    }
  }

  private void connect() throws PortalDeployerClientException {
    // Only attempt reconnect if client is still running
    if(isRunning) {
      try {
        mqttClient.connect(mqttConnectOptions, null, new ConnectCallback());
      } catch (MqttException e) {
        throw new PortalDeployerClientException(e.getMessage(), e);
      }
    }
  }

  private void subscribe() throws MqttException {
    mqttClient.subscribe(topic, qosLevel, null, new SubscribeCallback());
  }

  private void disconnect() {
    try {
      mqttClient.disconnect(null, new DisconnectCallback());
    } catch (MqttException e) {
      logger.log(Level.SEVERE, "Unable to disconnect client", e);
    }
  }

  private void reconnect() {
    sleep(sleepIntervalSupplier.getAsInt());
    logger.log(Level.INFO, String.format("Attempting to reconnect to broker %s after connection lost", mqttBrokerUri));
    try {
      connect();
    } catch (PortalDeployerClientException e) {
      logger.log(Level.SEVERE, "Failed to reconnect client after connection lost", e);
    }
  }

  @Override
  public void connectionLost(Throwable cause) {
    logger.log(Level.WARNING, String.format("Connection to broker %s was lost", mqttBrokerUri), cause);
    reconnect();
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    logger.log(Level.INFO, String.format("Message Arrived - Topic: %s, Message: %s", topic, new String(message.getPayload())));
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    logger.log(Level.INFO, String.format("Delivery Complete - Topic: %s", topic));
  }

  @Override
  public String toString() {
    return String.format("PortalDeployerClient{running='%s', mqttBrokerUri='%s', clientId='%s', topic='%s'}", isRunning, mqttBrokerUri, clientId, topic);
  }

  private void sleep(int interval) {
    try {
      Thread.sleep(interval);
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "thread interrupted", e);
      Thread.currentThread().interrupt();
    }
  }

  class SubscribeCallback implements IMqttActionListener {
    @Override
    public void onSuccess(IMqttToken iMqttToken) {
      logger.log(Level.INFO, String.format("Successfully subscribed to topic: %s", topic));
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
      // TODO(chemi11): Should we do anything on failure to subscribe? try and subscribe again?
      logger.log(Level.SEVERE, String.format("Failed to subscribe to topic: %s", topic), throwable);
    }
  }

  class ConnectCallback implements IMqttActionListener {
    @Override
    public void onSuccess(IMqttToken iMqttToken) {
      // Successfully connected, reset the sleep interval supplier to start at 0 again
      sleepIntervalSupplier.reset();
      logger.log(Level.INFO, String.format("Successfully connected to Broker: %s", mqttBrokerUri));
      try {
        logger.log(Level.INFO, String.format("Subscribing to Topic: %s", topic));
        subscribe();
      } catch (MqttException e) {
        logger.log(Level.SEVERE, String.format("Failed to subscribe to topic: %s", topic), e);
      }
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
      logger.log(Level.SEVERE, String.format("Failed connecting to Broker: %s", mqttBrokerUri), throwable);
      reconnect();
    }
  }

  class DisconnectCallback implements IMqttActionListener {
    @Override
    public void onSuccess(IMqttToken iMqttToken) {
      logger.log(Level.INFO, String.format("Successfully disconnected from Broker: %s", mqttBrokerUri));
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
      logger.log(Level.SEVERE, "Failure while disconnecting client", throwable);
    }
  }
}