package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.external.assertions.portaldeployer.server.client.MessageProcessor;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import org.springframework.context.ApplicationContext;

/**
 * @author chemi11, 2017-10-24
 */
public class PortalDeployerClientManagerImpl implements PortalDeployerClientManager {

  private static PortalDeployerClientManagerImpl instance;

  private PortalDeployerClient portalDeployerClient;
  private PortalDeployerClientConfigurationManager configurationManager;
  private PortalDeployerSslConfigurationManager sslConfigurationManager;
  private PortalDeployerClientFactory portalDeployerClientFactory;

  public static synchronized PortalDeployerClientManager getInstance(final ApplicationContext context) {
    if(instance == null) {
      instance = new PortalDeployerClientManagerImpl(context);
    }
    return instance;
  }

  private PortalDeployerClientManagerImpl(final ApplicationContext context) {
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
          configurationManager,
          sslConfigurationManager.getSniEnabledSocketFactory(configurationManager.getBrokerHost()),
          new MessageProcessor(sslConfigurationManager.getSniEnabledSocketFactory(configurationManager.getIngressHost()), configurationManager)
      );
    }
    return portalDeployerClient;
  }

  public void start() throws PortalDeployerClientException, PortalDeployerConfigurationException {
    //TODO: implement builder and fix client ids/topics to pull from somewhere
    getClient().start();
  }

  public void stop() throws PortalDeployerClientException, PortalDeployerConfigurationException {
    if(portalDeployerClient != null) {
      getClient().stop();
      portalDeployerClient = null;
    }
  }
}
