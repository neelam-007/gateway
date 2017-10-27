package com.l7tech.external.assertions.portaldeployer.server;

import static org.mockito.Mockito.*;
import com.l7tech.external.assertions.portaldeployer.server.client.MessageProcessor;
import com.l7tech.external.assertions.portaldeployer.server.client.PortalDeployerClient;
import javax.net.ssl.SSLSocketFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author chemi11, 2017-10-26
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalDeployerClientManagerImplTest {

  @Mock
  private PortalDeployerClient portalDeployerClient;
  @Mock
  private PortalDeployerSslConfigurationManager portalDeployerSslConfigurationManager;
  @Mock
  private PortalDeployerClientConfigurationManager portalDeployerClientConfigurationManager;
  @Mock
  private PortalDeployerClientFactory portalDeployerClientFactory;

  private PortalDeployerClientManager portalDeployerClientManager;

  // client configurations
  private String mqttBrokerUri = "test.dev.ca.com";
  private String tenantId = "test";
  private String tenantGatewayUuid = "tenantGatewayUuid";
  @Mock
  private SSLSocketFactory sslSocketFactory;

  @Before
  public void beforePortalDeployerClientManagerImplTest() throws Exception {
    portalDeployerClientManager = new PortalDeployerClientManagerImpl(
        portalDeployerSslConfigurationManager,
        portalDeployerClientConfigurationManager,
        portalDeployerClientFactory
    );
    when(portalDeployerSslConfigurationManager.getSniEnabledSocketFactory(mqttBrokerUri)).thenReturn(sslSocketFactory);
    when(portalDeployerClientConfigurationManager.getBrokerHost()).thenReturn(mqttBrokerUri);
    when(portalDeployerClientConfigurationManager.getTenantId()).thenReturn(tenantId);
    when(portalDeployerClientConfigurationManager.getTenantGatewayUuid()).thenReturn(tenantGatewayUuid);
    when(portalDeployerClientFactory.getClient(
        eq(portalDeployerClientConfigurationManager),
        eq(sslSocketFactory),
        any(MessageProcessor.class)
    )).thenReturn(portalDeployerClient);
  }

  /**
   * Test that a start manager call will start the client
   * @throws Exception
   */
  @Test
  public void start() throws Exception {
    portalDeployerClientManager.start();
    verify(portalDeployerClient, times(1)).start();
  }

  /**
   * Test that a stop manager call will stop the client
   * @throws Exception
   */
  @Test
  public void stop() throws Exception {
    portalDeployerClientManager.stop();
    verify(portalDeployerClient, times(1)).stop();
  }

  /**
   * Test that a if you stop the manager and start it again, a new client is made
   * @throws Exception
   */
  @Test
  public void ensureNewClientInstantiatedAfterStop() throws Exception {
    PortalDeployerClient portalDeployerClient2 = mock(PortalDeployerClient.class);
    when(portalDeployerClientFactory.getClient(
        eq(portalDeployerClientConfigurationManager),
        eq(sslSocketFactory),
        any(MessageProcessor.class)
    )).thenReturn(portalDeployerClient).thenReturn(portalDeployerClient2);
    portalDeployerClientManager.stop();
    portalDeployerClientManager.start();
    verify(portalDeployerClient, times(1)).stop();
    verify(portalDeployerClient2, times(1)).start();
  }

}