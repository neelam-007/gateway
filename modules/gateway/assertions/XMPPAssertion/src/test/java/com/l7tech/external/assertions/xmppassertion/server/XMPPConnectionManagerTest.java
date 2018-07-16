package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.test.BugId;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * User: rseminoff
 * Date: 23/05/12
 *
 * This tests the methods in the XMPPConnectionManager class, which is the heavy lifter of this assertion.
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class XMPPConnectionManagerTest {

    private static XMPPConnectionManager xmppConnectionManager;

    private static XMPPClassHelperImpl xmppClassHelper;

    @BeforeClass
    public static void initialize() throws Exception {
        xmppClassHelper = mock(XMPPClassHelperImpl.class);

        EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager = mock(EntityManager.class);
        StashManagerFactory stashManagerFactory = mock(StashManagerFactory.class);
        MessageProcessor messageProcessor = mock(MessageProcessor.class);
        SsgKeyStoreManager keyStoreManager = mock(SsgKeyStoreManager.class);
        TrustManager trustManager = mock(TrustManager.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        DefaultKey defaultKey = mock(DefaultKey.class);
        FirewallRulesManager firewallRulesManager = mock(FirewallRulesManager.class);
        XMPPClassHelperFactory xmppClassHelperFactory = mock(XMPPClassHelperFactory.class);

        XMPPClassHelperFactory.setInstance(xmppClassHelperFactory);
        when(xmppClassHelperFactory.createClassHelper()).thenReturn(xmppClassHelper);

        XMPPConnectionEntity theXmpp = new XMPPConnectionEntity();
        theXmpp.setGoid(new Goid(1L, 0L));
        theXmpp.setEnabled(true);
        theXmpp.setInbound(true);
        theXmpp.setName("XMPP1");
        theXmpp.setHostname("Hostname1");
        theXmpp.setBindAddress("BindAddress1");
        when(entityManager.findAll()).thenReturn(Collections.singletonList(theXmpp));

        XMPPConnectionManager.createConnectionManager(
                entityManager,
                stashManagerFactory,
                messageProcessor,
                keyStoreManager,
                trustManager,
                secureRandom,
                defaultKey,
                firewallRulesManager);
        xmppConnectionManager = XMPPConnectionManager.getInstance();

        NioSocketAcceptor nioSocketAcceptor = new NioSocketAcceptor();
        when(xmppClassHelper.createNioSocketAcceptor()).thenReturn(nioSocketAcceptor);

        xmppConnectionManager.start();
        verify(xmppClassHelper, times(1)).nioSocketAcceptorSetReuseAddress(eq(nioSocketAcceptor), eq(true));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        xmppConnectionManager.stop();
        xmppConnectionManager = null;
        XMPPConnectionManager.destroyInstance();
    }

    @BugId("DE306944")
    @Test
    public void testConnectionAdd() throws Exception {
        XMPPConnectionEntity theXmpp = new XMPPConnectionEntity();
        theXmpp.setGoid(new Goid(2L, 0L));
        theXmpp.setEnabled(true);
        theXmpp.setInbound(true);
        theXmpp.setName("XMPP2");
        theXmpp.setHostname("Hostname2");
        theXmpp.setBindAddress("BindAddress2");

        NioSocketAcceptor nioSocketAcceptor = new NioSocketAcceptor();
        when(xmppClassHelper.createNioSocketAcceptor()).thenReturn(nioSocketAcceptor);

        xmppConnectionManager.connectionAdded(theXmpp);
        verify(xmppClassHelper, times(1)).nioSocketAcceptorSetReuseAddress(eq(nioSocketAcceptor), eq(true));
    }

    @BugId("DE306944")
    @Test
    public void testConnectionUpdate() throws Exception {

        // Update "XMPP1" entity created in the initialize() method.
        XMPPConnectionEntity theXmpp = new XMPPConnectionEntity();
        theXmpp.setGoid(new Goid(1L, 0L));
        theXmpp.setEnabled(true);
        theXmpp.setInbound(true);
        theXmpp.setName("XMPP1");
        theXmpp.setHostname("Hostname1-Updated");
        theXmpp.setBindAddress("Hostname1-Updated");

        NioSocketAcceptor nioSocketAcceptor = new NioSocketAcceptor();
        when(xmppClassHelper.createNioSocketAcceptor()).thenReturn(nioSocketAcceptor);

        xmppConnectionManager.connectionUpdated(theXmpp);
        verify(xmppClassHelper, times(1)).nioSocketAcceptorSetReuseAddress(eq(nioSocketAcceptor), eq(true));
    }
}
