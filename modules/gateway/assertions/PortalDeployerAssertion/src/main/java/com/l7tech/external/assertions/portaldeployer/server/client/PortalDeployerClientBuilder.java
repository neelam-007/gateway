package com.l7tech.external.assertions.portaldeployer.server.client;

import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttClient;

public class PortalDeployerClientBuilder {
  private String mqttBrokerUri;
  private String clientId;
  private String topic;
  private int connectionTimeout;
  private int keepAliveInterval;
  private boolean isCleanSession;
  private SSLSocketFactory sslSocketFactory;
  private MessageProcessor messageProcessor;

  public PortalDeployerClientBuilder setMqttBrokerUri(String mqttBrokerUri) {
    this.mqttBrokerUri = mqttBrokerUri;
    return this;
  }

  public PortalDeployerClientBuilder setClientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  public PortalDeployerClientBuilder setTopic(String topic) {
    this.topic = topic;
    return this;
  }

  public PortalDeployerClientBuilder setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    return this;
  }

  public PortalDeployerClientBuilder setKeepAliveInterval(int keepAliveInterval) {
    this.keepAliveInterval = keepAliveInterval;
    return this;
  }

  public PortalDeployerClientBuilder setCleanSession(boolean cleanSession) {
    this.isCleanSession = cleanSession;
    return this;
  }

  public PortalDeployerClientBuilder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
    return this;
  }

  public PortalDeployerClientBuilder setMessageProcessor(MessageProcessor messageProcessor) {
    this.messageProcessor = messageProcessor;
    return this;
  }

  public PortalDeployerClient createPortalDeployerClient() throws PortalDeployerClientException {
    return new PortalDeployerClient(mqttBrokerUri,
            clientId,
            topic,
            connectionTimeout,
            keepAliveInterval,
            isCleanSession,
            sslSocketFactory,
            messageProcessor);
  }
}