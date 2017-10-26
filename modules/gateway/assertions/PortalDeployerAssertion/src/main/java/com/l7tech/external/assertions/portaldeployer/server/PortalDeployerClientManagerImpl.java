package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import org.springframework.context.ApplicationContext;

/**
 * @author chemi11, 2017-10-24
 */
public class PortalDeployerClientManagerImpl implements PortalDeployerClientManager {

  private int connectTimeout = 60;
  private int keepAliveInterval = 30;

  private PortalDeployerClient portalDeployerClient;
  private PortalDeployerClientConfigurationManager configurationManager;
  private PortalDeployerSslConfigurationManager sslConfigurationManager;
  private PortalDeployerClientFactory portalDeployerClientFactory;

  PortalDeployerClientManagerImpl(final ApplicationContext context) {
    this.configurationManager = new PortalDeployerClientConfigurationManagerImpl(context);
    this.sslConfigurationManager = new PortalDeployerSslConfigurationManagerImpl(context);
    this.portalDeployerClientFactory = new PortalDeployerClientFactory();
  }

  // Used for testing
  PortalDeployerClientManagerImpl(PortalDeployerSslConfigurationManager portalDeployerSslConfigurationManager,
                                  PortalDeployerClientConfigurationManager portalDeployerClientConfigurationManager,
                                  PortalDeployerClientFactory portalDeployerClientFactory) {
    this.configurationManager = portalDeployerClientConfigurationManager;
    this.sslConfigurationManager = portalDeployerSslConfigurationManager;
    this.portalDeployerClientFactory = portalDeployerClientFactory;
  }

  private synchronized PortalDeployerClient getClient() throws PortalDeployerConfigurationException, PortalDeployerClientException {
    if (portalDeployerClient == null) {
      portalDeployerClient = portalDeployerClientFactory.getClient(
          String.format("wss://%s/", configurationManager.getBrokerHost()),
          String.format("%s_%s_%s", configurationManager.getTenantId(), configurationManager.getTenantGatewayUuid(), "1"),
          String.format("%s/api/cmd/deploy/tenantGatewayUuid/%s", configurationManager.getTenantId(), configurationManager.getTenantGatewayUuid()),
          // TODO(chemi11) update to use config manager
          connectTimeout,
          keepAliveInterval,
          sslConfigurationManager.getSniEnabledSocketFactory(configurationManager.getBrokerHost())
      );
    }
    return portalDeployerClient;
  }

  public void start() throws PortalDeployerClientException, PortalDeployerConfigurationException {
    //TODO: implement builder and fix client ids/topics to pull from somewhere
    getClient().start();
  }

  public void stop() throws PortalDeployerClientException, PortalDeployerConfigurationException {
    getClient().stop();
    portalDeployerClient = null;
  }
}
