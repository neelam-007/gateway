package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorGetSessionListAssertion;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader.*;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 07/02/14
 * Time: 9:52 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({NioSocketConnectorWrapper.class, OutboundIoHandlerAdapterWrapper.class, ProtocolCodecFilterWrapper.class})
public class ServerExtensibleSocketConnectorGetSessionListAssertionTest {

    private static SocketConnectorManager socketConnectorManager = null;
    private static NioSocketConnectorWrapper connectorWrapper = null;
    private static Goid serviceGoid = Goid.DEFAULT_GOID;

    private ApplicationContext appContext = null;
    private PolicyEnforcementContext peCtx = null;

    @BeforeClass
    public static void initialize() throws FindException, ExtensibleSocketConnectorClassHelperNotInitializedException, ExtensibleSocketConnectorMinaClassException {
        //setup the socket connector
        EntityManager entityManager = mock(EntityManager.class);
        when(entityManager.findAll()).thenReturn(new ArrayList<ExtensibleSocketConnectorEntity>());

        ClusterPropertyManager clusterPropertyManager = mock(ClusterPropertyManager.class);
        SsgKeyStoreManager ssgKeyStoreManager = mock(SsgKeyStoreManager.class);
        TrustManager trustManager = mock(TrustManager.class);
        SecureRandom secureRandom = mock(SecureRandom.class);
        StashManagerFactory stashManagerFactory = mock(StashManagerFactory.class);
        MessageProcessor messageProcessor = mock(MessageProcessor.class);
        DefaultKey defaultKey = mock(DefaultKey.class);
        FirewallRulesManager firewallRulesManager = mock(FirewallRulesManager.class);

        SocketConnectorManager.createConnectionManager(entityManager, clusterPropertyManager, ssgKeyStoreManager,
                trustManager, secureRandom, stashManagerFactory, messageProcessor, defaultKey, firewallRulesManager);
        socketConnectorManager = SocketConnectorManager.getInstance();
        socketConnectorManager.start();

        //create mock mina wrapper classes
        PowerMockito.mockStatic(ProtocolCodecFilterWrapper.class);
        when(ProtocolCodecFilterWrapper.create(any(Object.class))).thenReturn(null);

        DefaultIoFilterChainBuilderWrapper ioFilterChainWrapper = mock(DefaultIoFilterChainBuilderWrapper.class);

        OutboundIoHandlerAdapterWrapper outboundIoHandlerAdapterWrapper = mock(OutboundIoHandlerAdapterWrapper.class);
        when(outboundIoHandlerAdapterWrapper.getIoHandler()).thenReturn(null);
        PowerMockito.mockStatic(OutboundIoHandlerAdapterWrapper.class);
        when(OutboundIoHandlerAdapterWrapper.create()).thenReturn(outboundIoHandlerAdapterWrapper);

        connectorWrapper = mock(NioSocketConnectorWrapper.class);
        when(connectorWrapper.getFilterChain()).thenReturn(ioFilterChainWrapper);
        PowerMockito.mockStatic(NioSocketConnectorWrapper.class);
        when(NioSocketConnectorWrapper.create()).thenReturn(connectorWrapper);

        //create entity data and add to socketConnetorManager
        ExtensibleSocketConnectorEntity entity = new ExtensibleSocketConnectorEntity();
        entity.setGoid(serviceGoid);
        entity.setIn(false);
        socketConnectorManager.connectionUpdated(entity);
    }

    @AfterClass
    public static void tearDown() {
        socketConnectorManager.stop();
    }

    @Before
    public void setUp() {
        appContext = mock(ApplicationContext.class);

        Message request = new Message();
        Message response = new Message();
        peCtx = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    /**
     * Test that the assertion returns a list of live sessions for a connection
     *
     * @throws Exception
     */
    @Test
    public void testGetSessionIdList() throws Exception {
        //setup data
        //setup output variable name
        String outputVariable = "output";

        //create mock session data
        Map<Long, IoSessionWrapper> sessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        sessionWrapperMap.put(1001L, null);
        sessionWrapperMap.put(1002L, null);
        sessionWrapperMap.put(1003L, null);
        when(connectorWrapper.getManagedSessions()).thenReturn(sessionWrapperMap);

        //setup assertion
        ExtensibleSocketConnectorGetSessionListAssertion assertion = new ExtensibleSocketConnectorGetSessionListAssertion();
        assertion.setTargetVariable(outputVariable);
        assertion.setSocketConnectorGoid(serviceGoid);

        ServerExtensibleSocketConnectorGetSessionListAssertion serverListAssertion =
                new ServerExtensibleSocketConnectorGetSessionListAssertion(assertion, appContext);

        //execute test
        AssertionStatus status = serverListAssertion.checkRequest(peCtx);
        List<String> sessionIdList = (List<String>) peCtx.getVariable(outputVariable);

        //test data
        assertEquals(AssertionStatus.NONE, status); //check that the assertion did not fail
        assertNotNull(sessionIdList); // check that a list was returned
        // check that the list contains the same number of entries as our mock session data
        assertEquals(sessionWrapperMap.size(), sessionIdList.size());

        // check that the lsit contains the same keys as the mock session data
        Set<Long> keySet = sessionWrapperMap.keySet();
        for (Long key : keySet) {
            assertTrue(sessionIdList.contains(key.toString()));
        }
    }

    /**
     * Test that an empty list is returned when a connection has no live sessions.
     *
     * @throws Exception
     */
    @Test
    public void testGetSessionidListEmptyList() throws Exception {
        //setup data
        //setup output variable name
        String outputVariable = "output";

        //create mock session data
        Map<Long, IoSessionWrapper> sessionWrapperMap = new HashMap<Long, IoSessionWrapper>();
        when(connectorWrapper.getManagedSessions()).thenReturn(sessionWrapperMap);

        //setup assertion
        ExtensibleSocketConnectorGetSessionListAssertion assertion = new ExtensibleSocketConnectorGetSessionListAssertion();
        assertion.setTargetVariable(outputVariable);
        assertion.setSocketConnectorGoid(serviceGoid);

        ServerExtensibleSocketConnectorGetSessionListAssertion serverListAssertion =
                new ServerExtensibleSocketConnectorGetSessionListAssertion(assertion, appContext);

        //execute test
        AssertionStatus status = serverListAssertion.checkRequest(peCtx);
        List<String> sessionIdList = (List<String>) peCtx.getVariable(outputVariable);
        assertEquals(AssertionStatus.NONE, status); // check that the assertion did not fail
        assertNotNull(sessionIdList); // check that a list was returned
        assertTrue(sessionIdList.isEmpty()); // check that the list was empty
    }

    /**
     * Test that assertion fails when a Goid for a nonexistent connection is used.
     *
     * @throws Exception
     */
    @Test
    public void testGetSessionidListNoConnectionFound() throws Exception {
        //setup data
        //setup output variable name
        String outputVariable = "output";

        //setup assertion
        ExtensibleSocketConnectorGetSessionListAssertion assertion = new ExtensibleSocketConnectorGetSessionListAssertion();
        assertion.setTargetVariable(outputVariable);
        assertion.setSocketConnectorGoid(new Goid(1, 2)); // pass in Goid for nonexistent connection

        ServerExtensibleSocketConnectorGetSessionListAssertion serverListAssertion =
                new ServerExtensibleSocketConnectorGetSessionListAssertion(assertion, appContext);

        //execute test
        AssertionStatus status = serverListAssertion.checkRequest(peCtx);
        assertEquals(AssertionStatus.FAILED, status);  // check that the assertion failed
    }
}
