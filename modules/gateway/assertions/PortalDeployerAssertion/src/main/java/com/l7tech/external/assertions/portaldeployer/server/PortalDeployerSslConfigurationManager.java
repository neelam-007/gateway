package com.l7tech.external.assertions.portaldeployer.server;

import javax.net.ssl.SSLSocketFactory;

/**
 * Manages the SSL configuration for the Portal Deployer and provides methods to extract the SSL configuration for
 * use by other components. The Portal Deployer is expected to communicate with two other systems using mutual auth
 * over SSL:
 *  1. A message broker that publishes deployment events
 *  2. A TSSG with restman.
 *
 *  This class provides access to the SSL components to support mutual auth with the above systems.
 */
public interface PortalDeployerSslConfigurationManager {

  /**
   * Returns an SNI enabled socket factory using the host specified. The socket factory is configured to use mutual auth
   * to talk to the specified host.
   * @param sniHostname The SNI hostname to specify as when an SSL connection is established with the host.
   * @return an SNI enabled socket factory using the host specified.
   * @throws PortalDeployerConfigurationException if any errors are encountered configuring the socket factory.
   */
  SSLSocketFactory getSniEnabledSocketFactory(String sniHostname) throws PortalDeployerConfigurationException;
}
