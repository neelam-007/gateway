package com.l7tech.external.assertions.portaldeployer.server.client;

import static org.mockito.Mockito.*;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
  private boolean isCleanSession = false;
  @Mock
  private SSLSocketFactory sslSocketFactory;
  @Mock
  private MqttAsyncClient mqttAsyncClient;
  @Mock
  private MessageProcessor messageProcessor;

  private PortalDeployerClient portalDeployerClient;

  @Before
  public void beforePortalDeployerClientTest() throws Exception {
    portalDeployerClient = new PortalDeployerClient(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, isCleanSession, sslSocketFactory, mqttAsyncClient, messageProcessor);
  }

  /**
   * Test the start method will connect to the broker if isConnected is false
   * @throws Exception
   */
  @Test
  public void start() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(false);
    portalDeployerClient.start();
    verify(mqttAsyncClient, times(1)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  /**
   * Test the stop method will disconnect from the broker if isConnected is true
   * @throws Exception
   */
  @Test
  public void stop() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    portalDeployerClient.stop();
    verify(mqttAsyncClient, times(1)).disconnect(isNull(), any(IMqttActionListener.class));
  }

  /**
   * Test the start method will not connect to the broker if isConnected is true
   * @throws Exception
   */
  @Test
  public void start_alreadyConnected() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    portalDeployerClient.start();
    verify(mqttAsyncClient, times(0)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  /**
   * Test the start method will not disconnect from the broker if isConnected is false
   * @throws Exception
   */
  @Test
  public void start_alreadyDisconnected() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(false);
    portalDeployerClient.stop();
    verify(mqttAsyncClient, times(0)).disconnect(any(Object.class), any(IMqttActionListener.class));
  }

  /**
   * Test the connectionLost method will connect to the broker if the client is running
   * @throws Exception
   */
  @Test
  public void connectionLost_reconnect() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true).thenReturn(false);
    // set isRunning to true;
    portalDeployerClient.start();
    portalDeployerClient.connectionLost(new Throwable());
    verify(mqttAsyncClient, times(1)).reconnect();
  }

  /**
   * Test the connectionLost method will not connect to the broker if the client is not running
   * @throws Exception
   */
  @Test
  public void connectionLost_notRunningNoReconnect() throws Exception {
    portalDeployerClient.connectionLost(new Throwable());
    verify(mqttAsyncClient, times(0)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  /**
   * Test the connect method will throw a PortalDeployerException if it fails to connect
   * @throws Exception
   */
  @Test(expected = PortalDeployerClientException.class)
  public void start_connectExceptionFailure() throws Exception {
    when(mqttAsyncClient.connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class))).thenThrow(new MqttException(0));
    portalDeployerClient.start();
  }

  /**
   * Test the messageArrived method process the message successfully
   * @throws Exception
   */
  @Test
  public void messageArrived_Success() throws Exception {
    MqttMessage mockMqttMessage = mock(MqttMessage.class);
    when(mockMqttMessage.getPayload()).thenReturn("payload".getBytes());
    when(messageProcessor.process(any(MqttMessage.class))).thenReturn(true);
    portalDeployerClient.messageArrived(null, mockMqttMessage);
    verify(messageProcessor, times(1)).process(any(MqttMessage.class));
  }

  /**
   * Test the messageArrived method will do nothing besides log
   * @throws Exception
   */
  @Test
  public void messageArrived_FailureToProcess() throws Exception {
    MqttMessage mockMqttMessage = mock(MqttMessage.class);
    when(mockMqttMessage.getPayload()).thenReturn("payload".getBytes());
    when(messageProcessor.process(any(MqttMessage.class))).thenReturn(false);
    portalDeployerClient.messageArrived(null, mockMqttMessage);
    verify(messageProcessor, times(1)).process(any(MqttMessage.class));
  }

  /**
   * Test the deliveryComplete method will do nothing besides log
   * @throws Exception
   */
  @Test
  public void deliveryComplete_Success() throws Exception {
    portalDeployerClient.deliveryComplete(null);
  }

  /**
   * Test the toString method will do nothing besides log
   * @throws Exception
   */
  @Test
  public void toString_Success() throws Exception {
    portalDeployerClient.toString();
  }

  /**
   * Test the connectionLost method will do nothing if a MqttException is thrown
   * @throws Exception
   */
  @Test
  public void connectionLost_connectException() throws Exception {
    when(mqttAsyncClient.connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class))).thenThrow(new MqttException(401));
    // only a log message is printed
    portalDeployerClient.connectionLost(new Throwable());
  }

  /**
   * Test the stop method will do nothing besides log if a MqttException is thrown
   * @throws Exception
   */
  @Test
  public void stop_disconnectException() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    when(mqttAsyncClient.disconnect(any(Object.class), any(IMqttActionListener.class))).thenThrow(new MqttException(401));
    // only a log message is printed
    portalDeployerClient.stop();
  }

  /**
   * Test the connectCallback method will subscribe on success
   * @throws Exception
   */
  @Test
  public void connectCallback_Success() throws Exception {
    PortalDeployerClient.ConnectCallback connectCallback = portalDeployerClient.new ConnectCallback();
    connectCallback.onSuccess(null);
    verify(mqttAsyncClient, times(1)).subscribe(matches(topic), any(Integer.class), any(Object.class), any(PortalDeployerClient.SubscribeCallback.class));
  }

  /**
   * Test the connectCallback onFailure method will do nothing if connect throws a PortalDeployerException
   * @throws Exception
   */
  @Test
  public void connectCallback_FailedReconnectException() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    portalDeployerClient.start();
    when(mqttAsyncClient.connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class))).thenThrow(new MqttException(0));
    PortalDeployerClient.ConnectCallback connectCallback = portalDeployerClient.new ConnectCallback();
    connectCallback.onFailure(null, new Throwable());
  }

  /**
   * Test the connectCallback method will try and reconnect if it fails initially
   * @throws Exception
   */
  @Test
  public void connectCallback_FailureReconnect() throws Exception {
    when(mqttAsyncClient.isConnected()).thenReturn(true);
    // set isRunning to true
    portalDeployerClient.start();
    PortalDeployerClient.ConnectCallback connectCallback = portalDeployerClient.new ConnectCallback();
    connectCallback.onFailure(null, new Throwable());
    verify(mqttAsyncClient, times(1)).reconnect();
  }

  /**
   * Test the connectCallback method will not reconnect if the client is not running if connect fails
   * @throws Exception
   */
  @Test
  public void connectCallback_FailureReconnectNotRunning() throws Exception {
    PortalDeployerClient.ConnectCallback connectCallback = portalDeployerClient.new ConnectCallback();
    connectCallback.onFailure(null, new Throwable());
    verify(mqttAsyncClient, times(0)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  /**
   * Test the connectCallback method will do nothing if subscribe throws an exception
   * @throws Exception
   */
  @Test
  public void connectCallback_SuccessFailedToSubscribe() throws Exception {
    PortalDeployerClient.ConnectCallback connectCallback = portalDeployerClient.new ConnectCallback();
    when(mqttAsyncClient.subscribe(matches(topic), any(Integer.class), any(Object.class), any(PortalDeployerClient.SubscribeCallback.class))).thenThrow(new MqttException(500));
    connectCallback.onSuccess(null);
    verify(mqttAsyncClient, times(0)).connect(any(MqttConnectOptions.class), any(Object.class), any(IMqttActionListener.class));
  }

  /**
   * Test the subscribeCallback method will do nothing on success besides log
   * @throws Exception
   */
  @Test
  public void subscribeCallback_Success() throws Exception {
    PortalDeployerClient.SubscribeCallback subscribeCallback = portalDeployerClient.new SubscribeCallback();
    // all it does is log
    subscribeCallback.onSuccess(null);
  }

  /**
   * Test the subscribeCallback method will do nothing on failure besides log
   * @throws Exception
   */
  @Test
  public void subscribeCallback_Failure() throws Exception {
    PortalDeployerClient.SubscribeCallback subscribeCallback = portalDeployerClient.new SubscribeCallback();
    // all it does is log
    subscribeCallback.onFailure(null, new Throwable());
  }

  /**
   * Test the disconnect method calls close.
   * @throws Exception
   */
  @Test
  public void disconnectCallback_Success() throws Exception {
    PortalDeployerClient.DisconnectCallback disconnectCallback = portalDeployerClient.new DisconnectCallback();
    disconnectCallback.onSuccess(null);
    verify(mqttAsyncClient, times(1)).close();
  }

  /**
   * Test the disconnect method will do nothing on failure besides log
   * @throws Exception
   */
  @Test
  public void disconnectCallback_Failure() throws Exception {
    PortalDeployerClient.DisconnectCallback disconnectCallback = portalDeployerClient.new DisconnectCallback();
    // all it does is log
    disconnectCallback.onFailure(null, new Throwable());
  }
}