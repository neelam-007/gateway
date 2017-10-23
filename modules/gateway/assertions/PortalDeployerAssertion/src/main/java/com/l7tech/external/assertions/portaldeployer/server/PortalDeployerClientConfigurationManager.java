package com.l7tech.external.assertions.portaldeployer.server;

/**
 * Manages the client configuration of the Portal Deployer.
 */
public interface PortalDeployerClientConfigurationManager {

  String getBrokerHost();

  String getTenantId();

  String getTenantGatewayUuid();

  boolean isPortalDeployerEnabled();
}
