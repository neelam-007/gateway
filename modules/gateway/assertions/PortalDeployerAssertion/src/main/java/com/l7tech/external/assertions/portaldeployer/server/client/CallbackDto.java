package com.l7tech.external.assertions.portaldeployer.server.client;

/**
 * Represents Portal callback message
 *
 * @author raqri01, 2017-10-26
 */
public class CallbackDto {
  private String status;
  private String message;

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
