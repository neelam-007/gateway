package com.l7tech.external.assertions.portaldeployer.server.client;

import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestResponse;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtil;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author raqri01, 2017-10-23
 */
@RunWith(MockitoJUnitRunner.class)
public class MessageProcessorTest {
  private static final Logger log = Logger.getLogger(MessageProcessorTest.class.getName());

  @Mock
  SSLSocketFactory sslSocketFactory;
  @Mock
  RequestUtil requestUtil;

  @Test
  public void testProcess() throws Exception {
    VariablesConfig config = new VariablesConfig();
    config.setIngressHost("elih4");
    MessageProcessor mp = new MessageProcessor(sslSocketFactory, config);
    mp.setRequestUtil(requestUtil);
    //build message
    ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    Messages testMessages = new Messages();
    Message message1 = new Message();
    String sourceUrl = "https://{INGRESS_HOST}:8443/source";
    String callbackUrl = "https://{INGRESS_HOST}:8443/callback";
    message1.setSourceLocation(sourceUrl);
    message1.setCallbackLocation(callbackUrl);
    List<Message> messageList = new ArrayList<>();
    messageList.add(message1);
    testMessages.setMessages(messageList);
    String mqttMessagePayload = mapper.writeValueAsString(testMessages);
    CallbackDto callback = new CallbackDto();
    callback.setLastTimeDeployed(1);
    callback.setMessage("a successfull operation");
    callback.setStatus("200");
    String callbackBody = mapper.writeValueAsString(callback);
    //the message
    MqttMessage message = new MqttMessage(mqttMessagePayload.getBytes());
    //mock events
    RequestResponse sourceResponse = new RequestResponse(200, "an awesome restman bundle");
    RequestResponse targetResponse = new RequestResponse(200, "a successfull operation");
    RequestResponse callbackResponse = new RequestResponse(200, "a callback response");
    when(requestUtil.processRequest(eq(mp.processVariablesConfig(sourceUrl)), any(), any(), any(), any(), eq(MessageProcessor.SOURCE_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(sourceResponse);
    when(requestUtil.processRequest(eq(mp.processVariablesConfig(MessageProcessor.TARGET_LOCATION_DEFAULT)), any(), any(), eq(sourceResponse.getBody()), eq(MessageProcessor.TARGET_CONTENT_TYPE_DEFAULT), eq(MessageProcessor.TARGET_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(targetResponse);
    when(requestUtil.processRequest(eq(mp.processVariablesConfig(callbackUrl)), any(), any(), eq(callbackBody), eq(MessageProcessor.CALLBACK_CONTENT_TYPE_DEFAULT), eq(MessageProcessor.CALLBACK_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(callbackResponse);

    //process message
    mp.process(message);

    verify(requestUtil, times(3)).processRequest(any(), any(), any(), any(), any(), any(), any());

  }

  @Test
  public void testProcessString() throws Exception {
    VariablesConfig config = new VariablesConfig();
    config.setIngressHost("elih4");
    MessageProcessor mp = new MessageProcessor(sslSocketFactory, config);
    mp.setRequestUtil(requestUtil);
    String payload = "{\"messages\":[{\"sourceLocation\":\"https://{INGRESS_HOST}:8443/source\",\"callbackLocation\":\"https://{INGRESS_HOST}:8443/callback\",\"messageId\":\"123\"}]}";
    //the message
    MqttMessage message = new MqttMessage(payload.getBytes());
    //mock events
    RequestResponse sourceResponse = new RequestResponse(200, "<restman><body/></restman>");
    RequestResponse targetResponse = new RequestResponse(200, "<restman><result/></restman>");
    RequestResponse callbackResponse = new RequestResponse(200, "a callback response");
    when(requestUtil.processRequest(eq("https://elih4:8443/source"), any(), any(), any(), any(), eq(MessageProcessor.SOURCE_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(sourceResponse);
    when(requestUtil.processRequest(eq(mp.processVariablesConfig(MessageProcessor.TARGET_LOCATION_DEFAULT)), any(), any(), eq("<restman><body/></restman>"), eq(MessageProcessor.TARGET_CONTENT_TYPE_DEFAULT), eq(MessageProcessor.TARGET_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(targetResponse);
    when(requestUtil.processRequest(eq("https://elih4:8443/callback"), any(), any(), contains("{\"status\":\"200\""), eq(MessageProcessor.CALLBACK_CONTENT_TYPE_DEFAULT), eq(MessageProcessor.CALLBACK_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(callbackResponse);
    //process message
    mp.process(message);
    verify(requestUtil, times(3)).processRequest(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testProcessBase64String() throws Exception {
    VariablesConfig config = new VariablesConfig();
    config.setIngressHost("elih4");
    MessageProcessor mp = new MessageProcessor(sslSocketFactory, config);
    mp.setRequestUtil(requestUtil);
    byte[] base64message = "<restman><body/></restman>".getBytes("UTF-8");
    String encoded = Base64.getEncoder().encodeToString(base64message);
    String payload = "{\"messages\":[{\"sourceLocation\":\"" + encoded + "\",\"sourceOperation\":\"base64\",\"callbackLocation\":\"https://{INGRESS_HOST}:8443/callback\",\"messageId\":\"456\"}]}";
    //the message
    MqttMessage message = new MqttMessage(payload.getBytes());
    //mock events
    RequestResponse sourceResponse = new RequestResponse(200, "");
    RequestResponse targetResponse = new RequestResponse(200, "<restman><result/></restman>");
    RequestResponse callbackResponse = new RequestResponse(200, "a callback response");
    when(requestUtil.processRequest(eq(mp.processVariablesConfig(MessageProcessor.TARGET_LOCATION_DEFAULT)), any(), any(), eq("<restman><body/></restman>"), eq(MessageProcessor.TARGET_CONTENT_TYPE_DEFAULT), eq(MessageProcessor.TARGET_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(targetResponse);
    when(requestUtil.processRequest(eq("https://elih4:8443/callback"), any(), any(), contains("{\"status\":\"200\""), eq(MessageProcessor.CALLBACK_CONTENT_TYPE_DEFAULT), eq(MessageProcessor.CALLBACK_OPERATION_DEFAULT), eq(sslSocketFactory))).thenReturn(callbackResponse);
    //process message
    mp.process(message);
    verify(requestUtil, times(2)).processRequest(any(), any(), any(), any(), any(), any(), any());
  }

}
