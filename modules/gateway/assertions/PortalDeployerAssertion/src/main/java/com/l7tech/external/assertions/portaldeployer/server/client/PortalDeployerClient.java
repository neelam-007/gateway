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
 * PortalDeployerClient is an MQTT client that connects to a specific broker and subscribes to a specific topic. It
 * supports both secure and insecure MQTT protocols (tcp, tls, ws, wss) leveraging the provided
 * {@link SSLSocketFactory}. This client implements {@link MqttCallback} to process incoming messages and disconnects.
 * Disconnects are handled automatically causing the client to reconnect. Message processing is delegated to the
 * provided {@link MessageProcessor}.
 *
 * This client currently doesn't publish any messages.
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
  private boolean isCleanSession;
  private MessageProcessor messageProcessor;
  private final Object messageProcessorLock = new Object();

  private ExponentialIntSupplier sleepIntervalSupplier;

  private int qosLevel = 1;

  // boolean used to stop client if it is stuck connecting to the broker
  private volatile Boolean isRunning = false;
  // How long to sleep in seconds
  private int sleepIntervalOnReconnect = 30;

  public PortalDeployerClient(String mqttBrokerUri,
                              String clientId,
                              String topic,
                              int connectionTimeout,
                              int keepAliveInterval,
                              boolean isCleanSession,
                              SSLSocketFactory sslSocketFactory,
                              MessageProcessor messageProcessor)
          throws PortalDeployerClientException {
    initialize(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, isCleanSession,
            sslSocketFactory, messageProcessor);
    try {
      mqttClient = new MqttAsyncClient(this.mqttBrokerUri, this.clientId, this.mqttClientPersistence);
    } catch (MqttException e) {
      throw new PortalDeployerClientException(e.getMessage(), e);
    }
    mqttClient.setCallback(this);
    logger.log(Level.INFO, String.format("Initialized with config: [%s]", this));
  }

  // For testing
  PortalDeployerClient(String mqttBrokerUri,
                       String clientId,
                       String topic,
                       int connectionTimeout,
                       int keepAliveInterval,
                       boolean isCleanSession,
                       SSLSocketFactory sslSocketFactory,
                       MqttAsyncClient mqttAsyncClient,
                       MessageProcessor messageProcessor) {
    initialize(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, isCleanSession,
            sslSocketFactory, messageProcessor);
    mqttClient = mqttAsyncClient;
    mqttClient.setCallback(this);
  }

  private void initialize(String mqttBrokerUri,
                          String clientId,
                          String topic,
                          int connectionTimeout,
                          int keepAliveInterval,
                          boolean isCleanSession,
                          SSLSocketFactory sslSocketFactory,
                          MessageProcessor messageProcessor) {
    this.sslSocketFactory = sslSocketFactory;
    this.mqttBrokerUri = mqttBrokerUri;
    this.clientId = clientId;
    this.topic = topic;
    this.connectionTimeout = connectionTimeout;
    this.keepAliveInterval = keepAliveInterval;
    this.isCleanSession = isCleanSession;
    this.messageProcessor = messageProcessor;

    mqttClientPersistence = new MemoryPersistence();
    mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setConnectionTimeout(this.connectionTimeout);
    mqttConnectOptions.setKeepAliveInterval(this.keepAliveInterval);
    mqttConnectOptions.setCleanSession(this.isCleanSession);
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
    if (isRunning) {
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
      logger.log(Level.SEVERE, String.format("Unable to disconnect client from broker [%s]: [%s]", mqttBrokerUri,
              e.getMessage()));
    }
  }

  private void reconnect() throws PortalDeployerClientException {
    if (isRunning) {
      sleep(sleepIntervalSupplier.getAsInt());
      logger.log(Level.INFO, String.format("Attempting to reconnect to broker [%s]", mqttBrokerUri));
      try {
        mqttClient.reconnect();
      } catch (MqttException e) {
        logger.log(Level.SEVERE, String.format("Failed to reconnect client after connection lost: %s", e.getMessage()));
        throw new PortalDeployerClientException(e.getMessage(), e);
      }
    }
  }

  @Override
  public void connectionLost(Throwable cause) {
    logger.log(Level.WARNING, String.format("Connection to broker [%s] was lost: [%s]", mqttBrokerUri, cause.getMessage()));
    try {
      reconnect();
    } catch (PortalDeployerClientException e) {
      logger.log(Level.SEVERE, String.format("Failed to reconnect client after connection lost: %s", e.getMessage()));
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    logger.log(Level.INFO, String.format("Message Arrived - ID: %s, Topic: %s", message.getId(), topic));

    //prevent concurrent message processing calls that may cause failures
    synchronized (messageProcessorLock) {
      if (this.messageProcessor.process(message)) {
        logger.log(Level.INFO, String.format("Successfully processed message %s", message.getId()));
      } else {
        logger.log(Level.INFO, String.format("Failed to process message %s", message.getId()));
      }
    }
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {
    logger.log(Level.INFO, String.format("Delivery Complete - Topic: %s", topic));
  }

  @Override
  public String toString() {
    return "PortalDeployerClient{" + "mqttBrokerUri='" + mqttBrokerUri + '\'' + ", clientId='" + clientId + '\'' + "," +
            " topic='" + topic + '\'' + ", connectionTimeout=" + connectionTimeout + ", keepAliveInterval=" +
            keepAliveInterval + ", isCleanSession=" + isCleanSession + ", qosLevel=" + qosLevel + ", isRunning=" +
            isRunning + ", sleepIntervalOnReconnect=" + sleepIntervalOnReconnect + '}';
  }

  private void sleep(int interval) {
    try {
      Thread.sleep(interval);
    } catch (InterruptedException e) {
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
      logger.log(Level.SEVERE, String.format("Failed to subscribe to topic [%s], cause [%s]", topic, throwable
              .getMessage()));
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
        logger.log(Level.SEVERE, String.format("Failed to subscribe to topic: [%s], cause: [%s]", topic, e.getMessage()));
      }
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
      logger.log(Level.SEVERE, String.format("Failed connecting to Broker: %s", mqttBrokerUri), throwable);
      try {
        reconnect();
      } catch (PortalDeployerClientException e) {
        logger.log(Level.SEVERE, "Failed to reconnect client after connection lost: [%s]", e.getMessage());
      }
    }
  }

  class DisconnectCallback implements IMqttActionListener {
    @Override
    public void onSuccess(IMqttToken iMqttToken) {
      try {
        mqttClient.close();
        logger.log(Level.INFO, String.format("Successfully disconnected from Broker [%s] and closed client",
                mqttBrokerUri));
      } catch (MqttException e) {
        logger.log(Level.WARNING, String.format("Exception trying to close mqttClient: [%s]", e.getMessage()));
      }
    }

    @Override
    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
      logger.log(Level.SEVERE, String.format("Failure while disconnecting client: [%s]", throwable.getMessage()));
    }
  }
}