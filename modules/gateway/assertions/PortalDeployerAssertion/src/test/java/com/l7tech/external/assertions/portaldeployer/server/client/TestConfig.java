package com.l7tech.external.assertions.portaldeployer.server.client;

import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManager;
import java.util.HashMap;
import java.util.Map;

/**
 * @author raqri01, 2017-10-27
 */
public class TestConfig implements PortalDeployerClientConfigurationManager {
  private String ingressHost;
  private String ingressPort = "443";
  private Map<String, String> targetLocations = new HashMap();
  private Map<String, String> successCallbackLocations = new HashMap();
  private Map<String, String> errorCallbackLocations = new HashMap();

  public void setIngressHost(String ingressHost) {
    this.ingressHost = ingressHost;
  }

  public void setIngressPort(String ingressPort) {
    this.ingressPort = ingressPort;
  }

  public void setTargetLocation(String entity, String targetLocation) {
    targetLocations.put(entity, targetLocation);
  }

  public void setSuccessCallbackLocation(String entity, String callbackLocation) {
    successCallbackLocations.put(entity, callbackLocation);
  }

  public void setErrorCallbackLocation(String entity, String callbackLocation) {
    errorCallbackLocations.put(entity, callbackLocation);
  }

  @Override
  public String getBrokerHost() {
    return null;
  }

  @Override
  public String getUniqueClientId() {
    return "1";
  }

  @Override
  public String getIngressPort() {
    return ingressPort;
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
  public String getSuccessCallbackLocation(String entity) {
    return successCallbackLocations.get(entity);
  }

  @Override
  public String getErrorCallbackLocation(String entity) {
    return errorCallbackLocations.get(entity);
  }

  @Override
  public boolean isPortalDeployerEnabled() {
    return false;
  }
}