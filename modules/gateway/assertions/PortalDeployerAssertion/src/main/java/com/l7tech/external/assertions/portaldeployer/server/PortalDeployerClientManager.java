package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;

/**
 * @author chemi11, 2017-10-24
 */
public interface PortalDeployerClientManager {
  void stop() throws PortalDeployerClientException, PortalDeployerConfigurationException;
  void start() throws PortalDeployerClientException, PortalDeployerConfigurationException;
}
