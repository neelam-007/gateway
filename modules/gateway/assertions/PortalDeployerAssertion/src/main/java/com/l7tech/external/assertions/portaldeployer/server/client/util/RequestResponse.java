package com.l7tech.external.assertions.portaldeployer.server.client.util;

/**
 * @author raqri01, 2017-10-24
 */
public class RequestResponse {
  private int code;
  private String body;

  public RequestResponse(int responseCode, String responseBody) {
    this.code = responseCode;
    this.body = responseBody;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String toString() {
    return "******************\n" + "ResponseCode: " + this.code + "\n" + "ResponseBody:\n" + this.body + "\n******************\n";
  }
}
