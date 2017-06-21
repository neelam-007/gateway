package com.l7tech.external.assertions.quickstarttemplate.server;

import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartEncapsulatedAssertionLocator;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartJsonServiceInstaller;
import com.l7tech.external.assertions.quickstarttemplate.server.policy.QuickStartServiceBuilder;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.GatewayState;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.Functions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 */
@RunWith(MockitoJUnitRunner.class)
public class QuickStartAssertionModuleLifecycleTest {

    @Mock
    private ApplicationContext appContext;

    @Mock
    private QuickStartEncapsulatedAssertionLocator assertionLocator;

    @Mock
    private QuickStartServiceBuilder serviceBuilder;

    @Mock
    private QuickStartJsonServiceInstaller jsonServiceInstaller;

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testScalerClusterProperty_scalerEnabled_GatewayState() throws Exception {
        doMock(() -> "true", true);
        Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
    }

    @Test
    public void testScalerClusterProperty_scalerEnabled_ApplicationListener() throws Exception {
        doMock(() -> "true", false);
        Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
    }

    @Test
    public void testScalerClusterProperty_scalerDisabled_GatewayState() throws Exception {
        doMock(() -> "false", true);
        Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
    }

    @Test
    public void testScalerClusterProperty_scalerDisabled_ApplicationListener() throws Exception {
        doMock(() -> "false", false);
        Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
    }

    @Test
    public void testScalerClusterProperty_missing_GatewayState() throws Exception {
        doMock(() -> null, true);
        if (QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_CLUSTER_PROPERTY_IS_MISSING) {
            Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
        } else {
            Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
        }
    }

    @Test
    public void testScalerClusterProperty_missing_ApplicationListener() throws Exception {
        doMock(() -> null, false);
        if (QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_CLUSTER_PROPERTY_IS_MISSING) {
            Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
        } else {
            Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
        }
    }

    @Test
    public void testScalerClusterProperty_FindException_GatewayState() throws Exception {
        doMock(() -> { throw new FindException("my exception"); }, true);
        if (QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY) {
            Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
        } else {
            Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
        }
    }

    @Test
    public void testScalerClusterProperty_FindException_ApplicationListener() throws Exception {
        doMock(() -> { throw new FindException("my exception"); }, false);
        if (QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY) {
            Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
        } else {
            Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
        }
    }

    @Test
    public void testScalerClusterProperty_RuntimeException_GatewayState() throws Exception {
        doMock(() -> { throw new RuntimeException("my runtime exception"); }, true);
        if (QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY) {
            Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
        } else {
            Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
        }
    }

    @Test
    public void testScalerClusterProperty_RuntimeException_ApplicationListener() throws Exception {
        doMock(() -> { throw new RuntimeException("my runtime exception"); }, false);
        if (QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_DEFAULT_VALUE_WHEN_ERROR_OCCURS_WHILE_READING_CLUSTER_PROPERTY) {
            Mockito.verify(jsonServiceInstaller, Mockito.never()).installJsonServices();
        } else {
            Mockito.verify(jsonServiceInstaller, Mockito.times(1)).installJsonServices();
        }
    }

    private void doMock(final Functions.NullaryThrows<String, ? extends Throwable> scalerEnabledFunctor, final boolean gatewayIsReadyForMessagesOnBoot) throws Exception {
        final ClusterPropertyManager clusterPropertyManager = Mockito.mock(ClusterPropertyManager.class);
        doMockAppContextAndReturn(appContext, clusterPropertyManager, "clusterPropertyManager", ClusterPropertyManager.class);
        Mockito.doAnswer(invocation -> scalerEnabledFunctor.call()).when(clusterPropertyManager).getProperty(QuickStartAssertionModuleLifecycle.QUICKSTART_SCALER_ENABLED_PROPERTY);

        final GatewayState gatewayState = Mockito.mock(GatewayState.class);
        Mockito.doReturn(gatewayIsReadyForMessagesOnBoot).when(gatewayState).isReadyForMessages();
        doMockAppContextAndReturn(appContext, gatewayState, "gatewayState", GatewayState.class);

        final ApplicationEventProxy applicationEventProxy = new ApplicationEventProxy();
        doMockAppContextAndReturn(appContext, applicationEventProxy, "applicationEventProxy", ApplicationEventProxy.class);

        Mockito.doNothing().when(jsonServiceInstaller).installJsonServices();
        new QuickStartAssertionModuleLifecycle(appContext, assertionLocator, serviceBuilder, jsonServiceInstaller);
        final ApplicationEvent event = Mockito.mock(ReadyForMessages.class);
        applicationEventProxy.onApplicationEvent(event);
    }

    private static <O> void doMockAppContextAndReturn(final ApplicationContext appContext, final O retObj, final String name, final Class<O> objClass) throws Exception {
        Mockito.doReturn(retObj).when(appContext).getBean(name);
        Mockito.doReturn(retObj).when(appContext).getBean(name, objClass);
    }
}