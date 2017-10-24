package com.l7tech.external.assertions.portaldeployer.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestResponse;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtil;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtilImpl;
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
  public static String SOURCE_OPERATION_DEFAULT = "GET";
  public static String TARGET_OPERATION_DEFAULT = "PUT";
  public static String TARGET_LOCATION_DEFAULT = "https://localhost:8443/restman/1.0/bundle";
  public static String TARGET_CONTENT_TYPE_DEFAULT = "application/xml";
  public static String CALLBACK_OPERATION_DEFAULT = "PUT";
  public static String CALLBACK_CONTENT_TYPE_DEFAULT = "application/json";

  public MessageProcessor(SSLSocketFactory sslSocketFactory) {
    this.sslSocketFactory = sslSocketFactory;
    mapper = new com.fasterxml.jackson.databind.ObjectMapper();
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
    boolean processed_successfully = false;
    try {
      byte[] payload = mqttMessage.getPayload();
      //validate as json by converting it
      final Messages messages = mapper.readValue(payload, Messages.class);
      if (messages != null) {
        for (Message message : messages.getMessages()) {
          boolean success = performAction(message);
          if (!success) {
            logger.log(Level.INFO, "failed proessing messageId %s ", message.getMessageId());
          }
        }
      } else {
        logger.log(Level.INFO, "No message to process");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "There was en error processing messages", e);
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
      RequestResponse sourceResponse, targetResponse, callbackResponse;
      //get payload from SOURCE
      if (sourceLocation.toLowerCase().startsWith("http")) {
        sourceResponse = getRequestUtil().processRequest(sourceLocation, null, null, null, null, sourceOperation, sslSocketFactory);
        targetResponse = getRequestUtil().processRequest(targetLocation, null, null, sourceResponse.getBody(), targetContentType, targetOperation, sslSocketFactory);
        callbackResponse = getRequestUtil().processRequest(callbackLocation, null, null, targetResponse.getBody(), callbackContentType, callbackOperation, sslSocketFactory);
        logger.log(Level.FINE, "Callback response code %s " + callbackResponse.getCode());
        logger.log(Level.FINE, "Callback response body %s " + callbackResponse.getBody());
      }

    } catch (Exception e) {
      logger.log(Level.SEVERE, "There was performAction for message ", e);
    }
    return true;
  }

  private static boolean isEmpty(String value) {
    return value == null || value.trim().length() <= 0;
  }

}
