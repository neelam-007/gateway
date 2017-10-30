package com.l7tech.external.assertions.portaldeployer.server.client;

import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManager;

/**
 * @author raqri01, 2017-10-27
 */
public class TestConfig implements PortalDeployerClientConfigurationManager {
  private String ingressHost;

  public void setIngressHost(String ingressHost) {
    this.ingressHost = ingressHost;
  }

  @Override
  public String getBrokerHost() {
    return null;
  }

  @Override
  public String getIngressHost() {
    return ingressHost;
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
  public boolean isPortalDeployerEnabled() {
    return false;
  }

  @Override
  public int getConnectTimeout() {
    return 0;
  }

  @Override
  public int getKeepAliveInterval() {
    return 0;
  }
}