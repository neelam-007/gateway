package com.l7tech.external.assertions.portaldeployer.server.client;

import static org.mockito.Mockito.*;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author chemi11, 2017-10-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalDeployerClientTest {

  private String mqttBrokerUri = "test";
  private String clientId = "clientId";
  private String topic = "topic";
  private int connectionTimeout = 30;
  private int keepAliveInterval = 60;
  @Mock
  private SSLSocketFactory sslSocketFactory;
  @Mock
  private MqttAsyncClient mqttAsyncClient;

  private PortalDeployerClient portalDeployerClient;

  @Before
  public void beforePortalDeployerClientTest() throws Exception {
    portalDeployerClient = new PortalDeployerClient(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, sslSocketFactory, mqttAsyncClient);
    Whitebox.setInternalState(portalDeployerClient, "sleepIntervalOnReconnect", 0);
  }

  @Test
  public void start() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(false);
    portalDeployerClient.start();
    verify(mqttAsyncClient, times(1)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  @Test
  public void stop() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    portalDeployerClient.stop();
    verify(mqttAsyncClient, times(1)).disconnect(any(Object.class), any(IMqttActionListener.class));
  }

  @Test
  public void start_alreadyConnected() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    portalDeployerClient.start();
    verify(mqttAsyncClient, times(0)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  @Test
  public void start_alreadyDisconnected() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(false);
    portalDeployerClient.stop();
    verify(mqttAsyncClient, times(0)).disconnect(any(Object.class), any(IMqttActionListener.class));
  }

  @Test
  public void connectionLost_reconnect() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    // set state to RUNNING;
    portalDeployerClient.start();
    when(mqttAsyncClient.isConnected()).thenReturn(false);
    portalDeployerClient.connectionLost(new Throwable());
    verify(mqttAsyncClient, times(1)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  @Test
  public void connectionLost_notRunningNoReconnect() throws Exception {
    portalDeployerClient.connectionLost(new Throwable());
    verify(mqttAsyncClient, times(0)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  @Test(expected = PortalDeployerClientException.class)
  public void start_connectExceptionFailure() throws Exception {
    when(mqttAsyncClient.connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class))).thenThrow(new MqttException(0));
    portalDeployerClient.start();
  }

}