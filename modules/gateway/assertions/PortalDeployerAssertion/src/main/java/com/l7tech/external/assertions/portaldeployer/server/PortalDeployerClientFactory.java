package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.external.assertions.portaldeployer.server.client.MessageProcessor;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientBuilder;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author chemi11, 2017-10-26
 */
public class PortalDeployerClientFactory {

  private PortalDeployerClientBuilder builder;

  public PortalDeployerClientFactory(PortalDeployerClientBuilder builder) {
    this.builder = builder;
  }

  public PortalDeployerClient getClient(PortalDeployerClientConfigurationManager configurationManager,
                                        SSLSocketFactory sslSocketFactory,
                                        MessageProcessor messageProcessor) throws
          PortalDeployerClientException {
    return builder
            .setMqttBrokerUri(String.format("%s://%s:%s/",
                    configurationManager.getBrokerProtocol(),
                    configurationManager.getBrokerHost(),
                    configurationManager.getBrokerPort()))
            .setClientId(String.format("%s_%s_%s",
                    configurationManager.getTenantId(),
                    configurationManager.getTenantGatewayUuid(),
                    configurationManager.getUniqueClientId()))
            .setTopic(String.format(configurationManager.getPortalDeployerTopic(),
                    configurationManager.getTenantId(),
                    configurationManager.getTenantGatewayUuid()))
            .setConnectionTimeout(configurationManager.getBrokerConnectionTimeout())
            .setKeepAliveInterval(configurationManager.getBrokerKeepAlive())
            .setCleanSession(configurationManager.getBrokerCleanSession())
            .setSslSocketFactory(sslSocketFactory)
            .setMessageProcessor(messageProcessor)
            .createPortalDeployerClient();
  }
}
