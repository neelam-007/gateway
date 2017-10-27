package com.l7tech.external.assertions.portaldeployer.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManager;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestResponse;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtil;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtilImpl;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocketFactory;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Process incoming mqtt  message
 *
 * @author raqri01, 2017-10-19
 */
public class MessageProcessor {
  private static final Logger logger = Logger.getLogger(MessageProcessor.class.getName());
  private SSLSocketFactory sslSocketFactory;
  private final ObjectMapper mapper;
  private RequestUtil requestUtil;
  private PortalDeployerClientConfigurationManager configurationManager;
  public static String SOURCE_OPERATION_DEFAULT = "GET";
  public static String TARGET_OPERATION_DEFAULT = "PUT";
  public static String TARGET_LOCATION_DEFAULT = "https://localhost:8443/restman/1.0/bundle";
  public static String TARGET_CONTENT_TYPE_DEFAULT = "application/xml";
  public static String CALLBACK_OPERATION_DEFAULT = "PUT";
  public static String CALLBACK_CONTENT_TYPE_DEFAULT = "application/json";

  @Deprecated
  public MessageProcessor(SSLSocketFactory sslSocketFactory) {
    this(sslSocketFactory, null);
  }

  public MessageProcessor(SSLSocketFactory sslSocketFactory, PortalDeployerClientConfigurationManager configurationManager) {
    this.sslSocketFactory = sslSocketFactory;
    mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    this.configurationManager = configurationManager;
  }

  public RequestUtil getRequestUtil() {
    if (requestUtil == null) {
      requestUtil = new RequestUtilImpl();
    }
    return requestUtil;
  }

  public void setRequestUtil(RequestUtil requestUtil) {
    this.requestUtil = requestUtil;
  }

  /**
   * Process MQTT Message
   *
   * @param mqttMessage
   * @return
   */
  public boolean process(MqttMessage mqttMessage) {
    boolean processed_successfully = true;
    try {
      byte[] payload = mqttMessage.getPayload();
      //validate as json by converting it
      final Messages messages = mapper.readValue(payload, Messages.class);
      if (messages != null && messages.getMessages() != null && messages.getMessages().size() > 0) {
        for (Message message : messages.getMessages()) {
          boolean success = performAction(message);
          if (!success) {
            logger.log(Level.INFO, String.format("failed processing messageId %s ", message.getMessageId()));
            //false if at least one failed
            processed_successfully = false;
          }
        }
      } else {
        logger.log(Level.INFO, "No message to process");
        processed_successfully = false;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "There was en error processing messages", e);
      processed_successfully = false;
    }
    return processed_successfully;
  }

  /**
   * Perform action base on the Message
   *
   * @param message
   * @return
   */
  public boolean performAction(Message message) {
    boolean result = false;
    try {
      String sourceLocation = message.getSourceLocation();
      String sourceOperation = message.getSourceOperation();
      String targetLocation = message.getTargetLocation();
      String targetOperation = message.getTargetOperation();
      String targetContentType = message.getTargetContentType();
      String callbackLocation = message.getCallbackLocation();
      String callbackOperation = message.getCallbackOperation();
      String callbackContentType = message.getCallbackContentType();
      //perform validation
      StringBuffer errorMessages = new StringBuffer();
      if (isEmpty(sourceLocation)) {
        errorMessages.append("sourceLocation is empty.");
      }
      if (isEmpty(callbackLocation)) {
        errorMessages.append("callbackLocation is empty.");
      }
      if (errorMessages.length() > 0) {
        logger.log(Level.INFO, "Error(s): " + errorMessages.toString());
      }
      //process defaults
      if (isEmpty(sourceOperation)) {
        sourceOperation = SOURCE_OPERATION_DEFAULT;
      }
      if (isEmpty(targetLocation)) {
        targetLocation = TARGET_LOCATION_DEFAULT;
      }
      if (isEmpty(targetContentType)) {
        targetContentType = TARGET_CONTENT_TYPE_DEFAULT;
      }
      if (isEmpty(targetOperation)) {
        targetOperation = TARGET_OPERATION_DEFAULT;
      }
      if (isEmpty(callbackOperation)) {
        callbackOperation = CALLBACK_OPERATION_DEFAULT;
      }
      if (isEmpty(callbackContentType)) {
        callbackContentType = CALLBACK_CONTENT_TYPE_DEFAULT;
      }
      sourceLocation = processVariablesConfig(sourceLocation);
      targetLocation = processVariablesConfig(targetLocation);
      callbackLocation = processVariablesConfig(callbackLocation);

      RequestResponse sourceResponse = null, targetResponse = null, callbackResponse = null;
      //get payload from SOURCE
      if (!isEmpty(sourceLocation) && sourceLocation.toLowerCase().startsWith("http")) {
        sourceResponse = getRequestUtil().processRequest(sourceLocation, null, null, null, null, sourceOperation, sslSocketFactory);
        logger.log(Level.FINE, String.format("source response code %s", sourceResponse.getCode()));

      } else if (!isEmpty(sourceOperation) && sourceOperation.equals("base64")) {
        byte[] decoded = Base64.getDecoder().decode(sourceLocation);
        String payload = new String(decoded, StandardCharsets.UTF_8);
        sourceResponse = new RequestResponse(200, payload);
      } else {
        logger.log(Level.FINE, "unsupported Location type");
      }
      if (sourceResponse != null) {
        targetResponse = getRequestUtil().processRequest(targetLocation, null, null, sourceResponse.getBody(), targetContentType, targetOperation, sslSocketFactory);
        CallbackDto callback = new CallbackDto();
        callback.setLastTimeDeployed(System.currentTimeMillis());
        callback.setMessage(targetResponse.getBody());
        callback.setStatus(String.valueOf(targetResponse.getCode()));
        String callbackBody = mapper.writeValueAsString(callback);
        callbackResponse = getRequestUtil().processRequest(callbackLocation, null, null, callbackBody, callbackContentType, callbackOperation, sslSocketFactory);
        if (callbackResponse != null && (callbackResponse.getCode() >= 200 && callbackResponse.getCode() < 300)) {
          result = true;
        }
        logger.log(Level.FINE, String.format("target response code %s, callback response code %s, callback body %s", targetResponse.getCode(), callbackResponse != null ? callbackResponse.getCode() : "was null", callbackResponse != null ? callbackResponse.getBody() : "was null"));
      }

    } catch (Exception e)

    {
      logger.log(Level.SEVERE, "There was performAction for message ", e);
    }
    return result;
  }

  protected String processVariablesConfig(String value) {
    if (!isEmpty(value) && configurationManager != null) {
      String tmp = value;
      if (configurationManager.getIngressHost() != null)
        tmp = tmp.replace("{INGRESS_HOST}", configurationManager.getIngressHost());
      if (configurationManager.getBrokerHost() != null)
        tmp = tmp.replace("{BROKER_HOST}", configurationManager.getBrokerHost());
      return tmp;
    }
    return value;
  }

  private static boolean isEmpty(String value) {
    return value == null || value.trim().length() <= 0;
  }

}
