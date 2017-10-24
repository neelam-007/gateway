package com.l7tech.external.assertions.portaldeployer.server.client;

/**
 * @author raqri01, 2017-10-20
 */
public class Message {
  private String entity;
  private String identifier;
  private String event;
  private String sourceLocation;
  private String sourceOperation;
  private String targetLocation;
  private String targetOperation;
  private String targetContentType;
  private String callbackLocation;
  private String callbackOperation;
  private String callbackContentType;
  private String messageId;

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getEvent() {
    return event;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public String getSourceLocation() {
    return sourceLocation;
  }

  public void setSourceLocation(String sourceLocation) {
    this.sourceLocation = sourceLocation;
  }

  public String getSourceOperation() {
    return sourceOperation;
  }

  public void setSourceOperation(String sourceOperation) {
    this.sourceOperation = sourceOperation;
  }

  public String getTargetLocation() {
    return targetLocation;
  }

  public void setTargetLocation(String targetLocation) {
    this.targetLocation = targetLocation;
  }

  public String getTargetOperation() {
    return targetOperation;
  }

  public void setTargetOperation(String targetOperation) {
    this.targetOperation = targetOperation;
  }

  public String getTargetContentType() {
    return targetContentType;
  }

  public void setTargetContentType(String targetContentType) {
    this.targetContentType = targetContentType;
  }

  public String getCallbackLocation() {
    return callbackLocation;
  }

  public void setCallbackLocation(String callbackLocation) {
    this.callbackLocation = callbackLocation;
  }

  public String getCallbackOperation() {
    return callbackOperation;
  }

  public void setCallbackOperation(String callbackOperation) {
    this.callbackOperation = callbackOperation;
  }

  public String getCallbackContentType() {
    return callbackContentType;
  }

  public void setCallbackContentType(String callbackContentType) {
    this.callbackContentType = callbackContentType;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }
}
