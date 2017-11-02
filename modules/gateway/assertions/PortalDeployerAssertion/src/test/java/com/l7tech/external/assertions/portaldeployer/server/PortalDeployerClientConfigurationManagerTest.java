package com.l7tech.external.assertions.portaldeployer.server;

import static com.l7tech.external.assertions.portaldeployer.server.PortalDeployerClientConfigurationManagerImpl.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

/**
 * Created by BAGJO04 on 2017-10-25.
 */
@RunWith(MockitoJUnitRunner.class)
public class PortalDeployerClientConfigurationManagerTest {
  @Mock
  private ApplicationContext context;
  @Mock
  private ClusterPropertyManager clusterPropertyManager;
  private PortalDeployerClientConfigurationManager portalDeployerClientConfigurationManager;

  @Before
  public void setup() {
    when(context.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);

    portalDeployerClientConfigurationManager = new PortalDeployerClientConfigurationManagerImpl(context);
  }

  @Test
  public void whenBrokerTimeoutInvalid_returnDefaultValue() throws FindException {
    when(clusterPropertyManager.getProperty(PD_BROKER_CONNECTION_TIMEOUT_CP)).thenReturn("abc");
    assertEquals(portalDeployerClientConfigurationManager.getBrokerConnectionTimeout(), PD_BROKER_CONNECTION_TIMEOUT_DEFAULT);
  }

  @Test
  public void whenBrokerKeepAliveInvalid_returnDefaultValue() throws FindException {
    when(clusterPropertyManager.getProperty(PD_BROKER_KEEP_ALIVE_CP)).thenReturn("abc");
    assertEquals(portalDeployerClientConfigurationManager.getBrokerKeepAlive(), PD_BROKER_KEEP_ALIVE_DEFAULT);
  }

  @Test
  public void whenCleanSessionInvalid_returnDefaultValue() throws FindException {
    when(clusterPropertyManager.getProperty(PD_BROKER_CLEAN_SESSION_CP)).thenReturn("abc");
    assertEquals(portalDeployerClientConfigurationManager.getBrokerCleanSession(), PD_BROKER_CLEAN_SESSION_DEFAULT);
  }

  @Test
  public void whenBrokerProtocolNull_returnDefaultValue() throws FindException {
    when(clusterPropertyManager.getProperty(PD_BROKER_PROTOCOL_CP)).thenReturn(null);
    assertEquals(portalDeployerClientConfigurationManager.getBrokerProtocol(), PD_BROKER_PROTOCOL_DEFAULT);
  }

  @Test
  public void test_getBrokerHost() throws Exception {

    portalDeployerClientConfigurationManager.getBrokerHost();

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_BROKER_HOST_CP));
  }

  @Test
  public void test_getBrokerHost_NotFoundReturnsNull() throws Exception {
    when(clusterPropertyManager.getProperty(anyString())).thenThrow(FindException.class);

    assertNull(portalDeployerClientConfigurationManager.getBrokerHost());

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_BROKER_HOST_CP));
  }

  @Test
  public void test_getTenantId() throws Exception {
    portalDeployerClientConfigurationManager.getTenantId();

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_TENANT_ID_CP));
  }

  @Test
  public void test_getTenantGatewayUuid() throws Exception {
    portalDeployerClientConfigurationManager.getTenantGatewayUuid();

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_TSSG_UUID_CP));
  }

  @Test
  public void test_isPortalDeployerEnabled() throws Exception {
    portalDeployerClientConfigurationManager.isPortalDeployerEnabled();

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_ENABLED_CP));
  }

  @Test
  public void test_getTargetLocation() throws Exception {
    portalDeployerClientConfigurationManager.getTargetLocation(null);

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_DEPLOY_TARGET_LOCATION));
  }

  @Test
  public void test_getCallbackLocation() throws Exception {
    portalDeployerClientConfigurationManager.getSuccessCallbackLocation(null);

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_DEPLOY_SUCCESS_CALLBACK_LOCATION));
  }

  @Test
  public void test_getTargetLocationWithEntity() throws Exception {
    portalDeployerClientConfigurationManager.getTargetLocation("API");

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_DEPLOY_TARGET_LOCATION + ".api"));
  }

  @Test
  public void test_getCallbackLocationWithEntity() throws Exception {
    portalDeployerClientConfigurationManager.getErrorCallbackLocation("API");

    verify(clusterPropertyManager, times(1)).
            getProperty(eq(PortalDeployerClientConfigurationManagerImpl.PD_DEPLOY_ERROR_CALLBACK_LOCATION + ".api"));
  }
}
