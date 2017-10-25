package com.l7tech.external.assertions.portaldeployer.server.client;

/**
 * Support variables that are customizable in the Message payload
 *
 * @author raqri01, 2017-10-24
 */
public class VariablesConfig {
  private String ingressHost;
  private String brokerHost;

  public String getIngressHost() {
    return ingressHost;
  }

  public void setIngressHost(String ingressHost) {
    this.ingressHost = ingressHost;
  }

  public String getBrokerHost() {
    return brokerHost;
  }

  public void setBrokerHost(String brokerHost) {
    this.brokerHost = brokerHost;
  }
}
