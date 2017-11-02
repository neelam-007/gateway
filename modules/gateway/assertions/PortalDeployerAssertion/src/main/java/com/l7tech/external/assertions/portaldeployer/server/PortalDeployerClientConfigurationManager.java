package com.l7tech.external.assertions.portaldeployer.server;

/**
 * Manages the client configuration of the Portal Deployer.
 */
public interface PortalDeployerClientConfigurationManager {

  String getUniqueClientId();
  String getIngressPort();
  String getIngressHost();
  String getBrokerPort();
  String getBrokerProtocol();
  int getBrokerKeepAlive();
  int getBrokerConnectionTimeout();
  boolean getBrokerCleanSession();
  String getBrokerHost();
  String getTenantId();
  String getTenantGatewayUuid();
  boolean isPortalDeployerEnabled();
  String getTargetLocation(String entity);
  String getSuccessCallbackLocation(String entity);
  String getErrorCallbackLocation(String entity);
}
