package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.external.assertions.portaldeployer.server.client.MessageProcessor;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author chemi11, 2017-10-26
 */
public class PortalDeployerClientFactory {

  public PortalDeployerClient getClient(PortalDeployerClientConfigurationManager configurationManager, SSLSocketFactory sslSocketFactory, MessageProcessor messageProcessor) throws PortalDeployerClientException {
    return new PortalDeployerClient(
        String.format("wss://%s/", configurationManager.getBrokerHost()),
        String.format("%s_%s_%s", configurationManager.getTenantId(), configurationManager.getTenantGatewayUuid(), "1"),
        String.format("%s/api/cmd/deploy/tenantGatewayUuid/%s", configurationManager.getTenantId(), configurationManager.getTenantGatewayUuid()),
        // TODO(chemi11) update to use config manager
        configurationManager.getConnectTimeout(),
        configurationManager.getKeepAliveInterval(),
        sslSocketFactory,
        messageProcessor
    );
  }
}
