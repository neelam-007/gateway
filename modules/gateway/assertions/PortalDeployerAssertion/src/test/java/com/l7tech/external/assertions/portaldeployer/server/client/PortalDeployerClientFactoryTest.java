package com.l7tech.external.assertions.portaldeployer.server.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManager;
import com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientFactory;
import javax.net.ssl.SSLSocketFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Created by BAGJO04 on 2017-11-08.
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalDeployerClientFactoryTest {

  @Mock
  private PortalDeployerClientBuilder builder;
  @Mock
  private PortalDeployerClientConfigurationManager configurationManager;
  @Mock
  private SSLSocketFactory sslSocketFactory;
  @Mock
  private MessageProcessor messageProcessor;

  private PortalDeployerClientFactory factory;

  @Before
  public void setup() {
    builder = mock(PortalDeployerClientBuilder.class, new SelfReturningAnswer());
    factory = new PortalDeployerClientFactory(builder);
    when(configurationManager.getPortalDeployerTopic()).thenReturn("%s/api/command/+/tenantGatewayUuid/%s");
  }

  @Test
  public void testGetClientUsesConfigFromConfigManager() throws Exception {
    factory.getClient(configurationManager, sslSocketFactory, messageProcessor);

    //verify topic and client id configuration used
    verify(configurationManager, times(1)).getPortalDeployerTopic();
    verify(configurationManager, times(1)).getUniqueClientId();
    //tenantId and tenantGatewayUuid used for both topic and clientId
    verify(configurationManager, times(2)).getTenantId();
    verify(configurationManager, times(2)).getTenantGatewayUuid();

    //verify broker configuration used
    verify(configurationManager, times(1)).getBrokerPort();
    verify(configurationManager, times(1)).getBrokerProtocol();
    verify(configurationManager, times(1)).getBrokerKeepAlive();
    verify(configurationManager, times(1)).getBrokerConnectionTimeout();
    verify(configurationManager, times(1)).getBrokerCleanSession();
    verify(configurationManager, times(1)).getBrokerHost();
  }

  /**
   * Prevents from having every .setBlah call on builder to return the same instance
   */
  class SelfReturningAnswer implements Answer<Object> {

    public Object answer(InvocationOnMock invocation) throws Throwable {
      Object mock = invocation.getMock();
      if (invocation.getMethod().getReturnType().isInstance(mock)) {
        return mock;
      } else {
        return RETURNS_DEFAULTS.answer(invocation);
      }
    }
  }
}
