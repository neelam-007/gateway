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
  private String successCallbackLocation;
  private String successCallbackOperation;
  private String successCallbackContentType;
  private String errorCallbackLocation;
  private String errorCallbackOperation;
  private String errorCallbackContentType;
  private String messageId;
  private String successStatus;
  private String errorStatus;

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

  public String getSuccessCallbackLocation() {
    return successCallbackLocation;
  }

  public void setSuccessCallbackLocation(String successCallbackLocation) {
    this.successCallbackLocation = successCallbackLocation;
  }

  public String getSuccessCallbackOperation() {
    return successCallbackOperation;
  }

  public void setSuccessCallbackOperation(String successCallbackOperation) {
    this.successCallbackOperation = successCallbackOperation;
  }

  public String getSuccessCallbackContentType() {
    return successCallbackContentType;
  }

  public void setSuccessCallbackContentType(String successCallbackContentType) {
    this.successCallbackContentType = successCallbackContentType;
  }

  public String getErrorCallbackLocation() {
    return errorCallbackLocation;
  }

  public void setErrorCallbackLocation(String errorCallbackLocation) {
    this.errorCallbackLocation = errorCallbackLocation;
  }

  public String getErrorCallbackOperation() {
    return errorCallbackOperation;
  }

  public void setErrorCallbackOperation(String errorCallbackOperation) {
    this.errorCallbackOperation = errorCallbackOperation;
  }

  public String getErrorCallbackContentType() {
    return errorCallbackContentType;
  }

  public void setErrorCallbackContentType(String errorCallbackContentType) {
    this.errorCallbackContentType = errorCallbackContentType;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getSuccessStatus() {
    return successStatus;
  }

  public void setSuccessStatus(String successStatus) {
    this.successStatus = successStatus;
  }

  public String getErrorStatus() {
    return errorStatus;
  }

  public void setErrorStatus(String errorStatus) {
    this.errorStatus = errorStatus;
  }
}
