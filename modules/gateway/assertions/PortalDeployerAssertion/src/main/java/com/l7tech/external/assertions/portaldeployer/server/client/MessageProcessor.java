package com.l7tech.external.assertions.portaldeployer.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManager;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestResponse;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtil;
import com.l7tech.external.assertions.portaldeployer.server.client.util.RequestUtilImpl;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
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
  public static String ERROR_DEFAULT = "ERROR";
  public static String SUCCESS_DEFAULT = "DEPLOYED";

  public MessageProcessor(SSLSocketFactory sslSocketFactory, PortalDeployerClientConfigurationManager configurationManager) {
    this.sslSocketFactory = sslSocketFactory;
    mapper = new ObjectMapper();
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
          processed_successfully = performAction(message);
          if (!processed_successfully) {
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
    RequestResponse sourceResponse;
    try {
      // validate message and set defaults
      Message validatedMessage = validateAndSetDefaults(message);

      //get payload from SOURCE
      sourceResponse = performSourceRequest(validatedMessage);
      if (sourceResponse == null)  {
        logger.log(Level.WARNING, "unable to get source location");
        return false;
      }

      // process target location requests
      List<CallbackDetailedDto> targetRequestResults = performTargetRequests(validatedMessage, sourceResponse);

      // perform callback requests
      return performCallbackRequests(validatedMessage, targetRequestResults);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "There was performAction for message ", e);
    }
    return false;
  }

  private Message validateAndSetDefaults(Message message) {
    //perform validation
    StringBuilder errorMessages = new StringBuilder();
    if (isEmpty(message.getSourceLocation())) {
      errorMessages.append("message.getSourceLocation() is empty.");
    }
    if (isEmpty(message.getSuccessCallbackLocation())) {
      errorMessages.append("successCallbackLocation is empty.");
    }
    if (errorMessages.length() > 0) {
      logger.log(Level.INFO, "Error(s): " + errorMessages.toString());
    }
    //process config overrides
    String configTargetLocation = configurationManager.getTargetLocation(message.getEntity());
    String configCallbackLocation = configurationManager.getCallbackLocation(message.getEntity());
    if (!isEmpty(configTargetLocation)) {
      message.setTargetLocation(configTargetLocation);
    }
    if (!isEmpty(configCallbackLocation)) {
      message.setSuccessCallbackLocation(configCallbackLocation);
    }

    // defaults
    if (isEmpty(message.getSourceOperation())) {
      message.setSourceOperation(SOURCE_OPERATION_DEFAULT);
    }
    if (isEmpty(message.getTargetLocation())) {
      message.setTargetLocation(TARGET_LOCATION_DEFAULT);
    }
    if (isEmpty(message.getTargetContentType())) {
      message.setTargetContentType(TARGET_CONTENT_TYPE_DEFAULT);
    }
    if (isEmpty(message.getTargetOperation())) {
      message.setTargetOperation(TARGET_OPERATION_DEFAULT);
    }
    if (isEmpty(message.getSuccessCallbackOperation())) {
      message.setSuccessCallbackOperation(CALLBACK_OPERATION_DEFAULT);
    }
    if (isEmpty(message.getSuccessCallbackContentType())) {
      message.setSuccessCallbackContentType(CALLBACK_CONTENT_TYPE_DEFAULT);
    }
    if (isEmpty(message.getErrorCallbackLocation())) {
      message.setErrorCallbackLocation(message.getSuccessCallbackLocation());
    }
    if (isEmpty(message.getErrorCallbackOperation())) {
      message.setErrorCallbackOperation(CALLBACK_OPERATION_DEFAULT);
    }
    if (isEmpty(message.getErrorCallbackContentType())) {
      message.setErrorCallbackContentType(CALLBACK_CONTENT_TYPE_DEFAULT);
    }
    if (isEmpty(message.getErrorStatus())) {
      message.setErrorStatus(ERROR_DEFAULT);
    }
    if (isEmpty(message.getSuccessStatus())) {
      message.setSuccessStatus(SUCCESS_DEFAULT);
    }
    message.setSourceLocation(processVariablesConfig(message.getSourceLocation()));
    message.setTargetLocation(processVariablesConfig(message.getTargetLocation()));
    message.setSuccessCallbackLocation(processVariablesConfig(message.getSuccessCallbackLocation()));
    message.setErrorCallbackLocation(processVariablesConfig(message.getErrorCallbackLocation()));
    return message;
  }

  private RequestResponse performSourceRequest(Message message) throws Exception {
    RequestResponse resp = null;
    if (!isEmpty(message.getSourceLocation()) && message.getSourceLocation().toLowerCase().startsWith("http")) {
      resp = getRequestUtil().processRequest(message.getSourceLocation(), null, null, null, null, message.getSourceOperation(), sslSocketFactory);
      logger.log(Level.FINE, String.format("source response code %s", resp.getCode()));
    } else if (!isEmpty(message.getSourceOperation()) && message.getSourceOperation().equalsIgnoreCase("base64")) {
      byte[] decoded = Base64.getDecoder().decode(message.getSourceLocation());
      String payload = new String(decoded, StandardCharsets.UTF_8);
      resp = new RequestResponse(200, payload);
    } else if (!isEmpty(message.getSourceOperation()) && message.getSourceOperation().equalsIgnoreCase("noop")) {//no operation
      resp = new RequestResponse(200, "");
    } else {
      logger.log(Level.FINE, "unsupported Location type");
    }
    return resp;
  }

  private List<CallbackDetailedDto> performTargetRequests(Message message, RequestResponse sourceResponse) throws Exception {
    ArrayList<CallbackDetailedDto> callbacks = new ArrayList<>();
    String[] targetLocations = message.getTargetLocation().split(",");
    for (String tlocation : targetLocations) {
      RequestResponse resp = getRequestUtil().processRequest(tlocation, null, null, sourceResponse.getBody(), message.getTargetContentType(), message.getTargetOperation(), sslSocketFactory);
      CallbackDetailedDto callbackDetail = new CallbackDetailedDto();
      callbackDetail.setTargetLocation(tlocation);
      callbackDetail.setMessage(resp.getBody());
      callbackDetail.setStatus(resp.getCode() < 400 ? message.getSuccessStatus() : message.getErrorStatus());
      if(resp.getCode() >= 400) {
        logger.log(Level.INFO, String.format("target request failed with response code %s, body %s", resp.getCode(), resp.getBody()));
      }
      callbacks.add(callbackDetail);
    }
    return callbacks;
  }

  private boolean performCallbackRequests(Message message, List<CallbackDetailedDto> targetRequestResults) throws Exception {
    String[] callbackLocations;
    String contentType;
    String operation;
    boolean success = targetRequestResults.stream().allMatch(c -> c.getStatus().equals(message.getSuccessStatus()));

    if(success) {
      callbackLocations = message.getSuccessCallbackLocation().split(",");
      contentType = message.getSuccessCallbackContentType();
      operation = message.getSuccessCallbackOperation();
    } else {
      callbackLocations = message.getErrorCallbackLocation().split(",");
      contentType = message.getErrorCallbackContentType();
      operation = message.getErrorCallbackOperation();
    }
    // prepare callback request body
    CallbackDto finalCallback = new CallbackDto();
    finalCallback.setStatus(success ? message.getSuccessStatus() : message.getSuccessStatus());
    finalCallback.setMessage(mapper.writeValueAsString(targetRequestResults));
    String body = mapper.writeValueAsString(finalCallback);

    boolean result = true;
    for (String clocation : callbackLocations) {
      RequestResponse resp = getRequestUtil().processRequest(clocation, null, null, body, contentType, operation, sslSocketFactory);
      if (resp.getCode() >= 400) {
        logger.log(Level.INFO, String.format("callback request failed with response code %s, body %s", resp.getCode(), resp.getBody()));
        result = false;
      }
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
