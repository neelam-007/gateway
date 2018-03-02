package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
import com.l7tech.server.transport.jms.JmsPropertyMapper;
import com.l7tech.server.transport.jms2.JmsConnectionMaxWaitException;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsResourceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerJmsRoutingAssertionMockedTest {
    private static final Goid ENDOINT_GOID = new Goid(0, 1);
    private static final Goid CONNECTION_GOID = new Goid(1, 1);
    private ServerJmsRoutingAssertion serverAssertion;
    private JmsRoutingAssertion assertion;
    private PolicyEnforcementContext context;
    private JmsEndpoint endpoint;
    private JmsConnection connection;
    private Message request;
    private Message response;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ApplicationEventProxy applicationEventProxy;
    private ServerConfig serverConfig;
    @Mock
    private JmsEndpointManager jmsEndpointManager;
    @Mock
    private JmsConnectionManager jmsConnectionManager;
    @Mock
    private StashManagerFactory stashManagerFactory;
    @Mock
    private JmsPropertyMapper jmsPropertyMapper;
    @Mock
    private JmsResourceManager jmsResourceManager;
    @Mock
    private SignerInfo senderVouchesSignerInfo;
    @Mock
    private DefaultKey defaultKey;

    @Before
    public void setup() throws Exception {
        serverConfig = new ServerConfigStub();
        connection = new JmsConnection();
        endpoint = new JmsEndpoint();
        endpoint.setConnectionGoid(CONNECTION_GOID);
        endpoint.setName("test");
        assertion = new JmsRoutingAssertion();
        assertion.setEndpointOid(ENDOINT_GOID);
        request = new Message();
        request.initialize(XmlUtil.parse("<test></test>"));
        response = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("serverConfig", ServerConfig.class)).thenReturn(serverConfig);
        when(applicationContext.getBean("jmsEndpointManager")).thenReturn(jmsEndpointManager);
        when(applicationContext.getBean("jmsConnectionManager")).thenReturn(jmsConnectionManager);
        when(applicationContext.getBean("stashManagerFactory", StashManagerFactory.class)).thenReturn(stashManagerFactory);
        when(applicationContext.getBean("jmsPropertyMapper", JmsPropertyMapper.class)).thenReturn(jmsPropertyMapper);
        when(applicationContext.getBean("jmsResourceManager", JmsResourceManager.class)).thenReturn(jmsResourceManager);
        when(applicationContext.getBean("defaultKey", DefaultKey.class)).thenReturn(defaultKey);
        when(jmsEndpointManager.findByPrimaryKey(ENDOINT_GOID)).thenReturn(endpoint);
        when(jmsConnectionManager.findByPrimaryKey(CONNECTION_GOID)).thenReturn(connection);
        serverAssertion = new ServerJmsRoutingAssertion(assertion, applicationContext);
    }

    @Test
    public void checkRequestNoError() throws Exception {
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(context));
        verify(jmsResourceManager).doWithJmsResources(any(JmsEndpointConfig.class), any(ServerJmsRoutingAssertion.JmsRoutingCallback.class));
    }

    @Test
    public void checkRequestNoSuchElementExceptionRetries() throws Exception {
        doThrow(new JmsConnectionMaxWaitException()).when(jmsResourceManager).doWithJmsResources(any(JmsEndpointConfig.class), any(ServerJmsRoutingAssertion.JmsRoutingCallback.class));
        assertEquals(AssertionStatus.FAILED, serverAssertion.checkRequest(context));
        verify(jmsResourceManager, times(5)).doWithJmsResources(any(JmsEndpointConfig.class), any(ServerJmsRoutingAssertion.JmsRoutingCallback.class));
    }


}
