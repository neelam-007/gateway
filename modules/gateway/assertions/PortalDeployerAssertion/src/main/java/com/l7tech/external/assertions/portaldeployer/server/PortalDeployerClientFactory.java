package com.l7tech.external.assertions.portaldeployer.server;

import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClientException;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author chemi11, 2017-10-26
 */
public class PortalDeployerClientFactory {

  public PortalDeployerClient getClient(String mqttBrokerUri,
                                        String clientId,
                                        String topic,
                                        int connectionTimeout,
                                        int keepAliveInterval,
                                        SSLSocketFactory sslSocketFactory) throws PortalDeployerClientException {
    return new PortalDeployerClient(mqttBrokerUri, clientId, topic, connectionTimeout, keepAliveInterval, sslSocketFactory);
  }
}
