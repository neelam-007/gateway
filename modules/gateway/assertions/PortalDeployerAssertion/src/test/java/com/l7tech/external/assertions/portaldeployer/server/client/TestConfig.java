package com.l7tech.external.assertions.portaldeployer.server.client;

import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManager;
import java.util.HashMap;
import java.util.Map;

/**
 * @author raqri01, 2017-10-27
 */
public class TestConfig implements PortalDeployerClientConfigurationManager {
  private String ingressHost;
  private Map<String, String> targetLocations = new HashMap();
  private Map<String, String> callbackLocations = new HashMap();

  public void setIngressHost(String ingressHost) {
    this.ingressHost = ingressHost;
  }

  public void setTargetLocation(String entity, String targetLocation) {
    targetLocations.put(entity, targetLocation);
  }

  public void setCallbackLocation(String entity, String callbackLocation) {
    callbackLocations.put(entity, callbackLocation);
  }

  @Override
  public String getBrokerHost() {
    return null;
  }

  @Override
  public String getIngressPort() {
    return "443";
  }

  @Override
  public String getIngressHost() {
    return ingressHost;
  }

  @Override
  public String getBrokerPort() {
    return "443";
  }

  @Override
  public String getBrokerProtocol() {
    return "wss";
  }

  @Override
  public int getBrokerKeepAlive() {
    return 0;
  }

  @Override
  public int getBrokerConnectionTimeout() {
    return 0;
  }

  @Override
  public boolean getBrokerCleanSession() {
    return false;
  }

  @Override
  public String getTenantId() {
    return null;
  }

  @Override
  public String getTenantGatewayUuid() {
    return null;
  }

  @Override
  public String getTargetLocation(String entity) {
    return targetLocations.get(entity);
  }

  @Override
  public String getCallbackLocation(String entity) {
    return callbackLocations.get(entity);
  }

  @Override
  public boolean isPortalDeployerEnabled() {
    return false;
  }
}