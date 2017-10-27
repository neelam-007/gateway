package com.l7tech.external.assertions.portaldeployer.server.client;

/**
 * Support variables that are customizable in the Message payload
 *
 * @author raqri01, 2017-10-24
 */
public class VariablesConfig {
  private String ingressHost;
  private String brokerHost;
  private String topic;
  private String targetLocation;//extended target location
  private String callbackLocation;//extended target location
  private String tenantId;
  private String tenantGatewayUuid;

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

  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getTargetLocation() {
    return targetLocation;
  }

  public void setTargetLocation(String targetLocation) {
    this.targetLocation = targetLocation;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTenantGatewayUuid() {
    return tenantGatewayUuid;
  }

  public void setTenantGatewayUuid(String tenantGatewayUuid) {
    this.tenantGatewayUuid = tenantGatewayUuid;
  }
}
