package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorReferenceFactory;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayState;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.TrustManager;

import java.security.SecureRandom;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SocketConnectorManager.class,ExtensibleSocketConnectorEntityManagerServerSupport.class})
public class ExtensibleSocketConnectorModuleLoadListenerTest {

    @Mock
    ApplicationContext context;

    @Mock
    ClusterPropertyManager clusterPropertyManager;

    @Mock
    SsgKeyStoreManager ssgKeyStoreManager;

    @Mock
    TrustManager trustManager;

    @Mock
    SecureRandom secureRandom;

    @Mock
    StashManagerFactory stashManagerFactory;

    @Mock
    MessageProcessor messageProcessor;

    @Mock
    DefaultKey defaultKey;

    @Mock
    FirewallRulesManager firewallRulesManager;

    @Mock
    GatewayState gatewayState;

    @Mock
    PolicyExporterImporterManager policyExporterImporterManager;

    @Mock
    ApplicationEventProxy applicationEventProxy;

    @Mock
    GenericEntityManager genericEntityManager;

    @Mock
    SocketConnectorManager socketConnectorManager;

    @Mock
    ExtensibleSocketConnectorEntityManagerServerSupport serverSupport;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(context.getBean("clusterPropertyManager", ClusterPropertyManager.class)).thenReturn(clusterPropertyManager);
        when(context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class)).thenReturn(ssgKeyStoreManager);
        when(context.getBean("routingTrustManager", TrustManager.class)).thenReturn(trustManager);
        when(context.getBean("secureRandom", SecureRandom.class)).thenReturn(secureRandom);
        when(context.getBean("stashManagerFacntory", StashManagerFactory.class)).thenReturn(stashManagerFactory);
        when(context.getBean("messageProcessor", MessageProcessor.class)).thenReturn(messageProcessor);
        when(context.getBean("defaultKey", DefaultKey.class)).thenReturn(defaultKey);
        when(context.getBean("ssgFirewallManager", FirewallRulesManager.class)).thenReturn(firewallRulesManager);
        when(context.getBean("gatewayState", GatewayState.class)).thenReturn(gatewayState);
        when(context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class)).thenReturn(policyExporterImporterManager);
        when(context.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(context.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);

        PowerMockito.mockStatic(SocketConnectorManager.class);
        when(SocketConnectorManager.getInstance()).thenReturn(socketConnectorManager);
        PowerMockito.mockStatic(ExtensibleSocketConnectorEntityManagerServerSupport.class);
        when(ExtensibleSocketConnectorEntityManagerServerSupport.getInstance(any(ApplicationContext.class))).thenReturn(serverSupport);
    }

    @After
    public void cleanUp() {
        ExtensibleSocketConnectorModuleLoadListener.onModuleUnloaded();
    }

    @Test
    public void whenGatewayNotReadyForMessagesExpectSocketConnectorNotStartedOnModuleLoad() {
        when(gatewayState.isReadyForMessages()).thenReturn(Boolean.FALSE);

        ExtensibleSocketConnectorModuleLoadListener.onModuleLoaded(context);

        verify(socketConnectorManager, never()).start();
    }

    @Test
    public void whenGatewayAlreadyReadyForMessagesExpectSocketConnectorStartedOnModuleLoad() {
        when(gatewayState.isReadyForMessages()).thenReturn(Boolean.TRUE);

        ExtensibleSocketConnectorModuleLoadListener.onModuleLoaded(context);

        verify(socketConnectorManager, times(1)).start();
    }

    @Test
    public void whenOnModuleLoadExpectExternalReferenceFactoryRegistered() {
        ExtensibleSocketConnectorModuleLoadListener.onModuleLoaded(context);

        verify(policyExporterImporterManager, times(1)).register(any(ExtensibleSocketConnectorReferenceFactory.class));
    }

    @Test
    public void whenOnModuleUnloadExpectCleanupHappened() {
        when(genericEntityManager.isRegistered(ExtensibleSocketConnectorEntity.class.getName())).thenReturn(Boolean.FALSE);
        ExtensibleSocketConnectorModuleLoadListener.onModuleLoaded(context);

        ExtensibleSocketConnectorModuleLoadListener.onModuleUnloaded();
        verify(applicationEventProxy, times(1)).removeApplicationListener(any(ApplicationListener.class));
        verify(socketConnectorManager, times(1)).stop();
        verify(genericEntityManager, times(1)).unRegisterClass(eq(ExtensibleSocketConnectorEntity.class.getName()));
    }
}
