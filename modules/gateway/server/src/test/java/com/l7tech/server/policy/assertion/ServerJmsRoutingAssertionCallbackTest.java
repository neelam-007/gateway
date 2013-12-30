package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsOutboundMessageType;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import com.l7tech.policy.assertion.JmsMessagePropertyRuleSet;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsMessageTestUtility;
import com.l7tech.server.transport.jms.TextMessageStub;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsResourceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.jms.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServerJmsRoutingAssertionCallbackTest{
    private ServerJmsRoutingAssertion serverAssertion;
    private JmsRoutingAssertion assertion;
    private ServerJmsRoutingAssertion.JmsRoutingCallback callback;
    private PolicyEnforcementContext policyContext;
    private Message request;
    private Message response;
    private Connection connection;
    private JmsEndpoint endpoint;
    private ServerConfig serverConfig;
    private TextMessageStub jmsRequest;
    private TextMessageStub jmsResponse;
    @Mock
    private Queue inbound;
    @Mock
    private Queue outbound;
    @Mock
    private JmsEndpointConfig endpointConfig;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private QueueSession session;
    @Mock
    private JmsResourceManager.JndiContextProvider contextProvider;
    @Mock
    private ApplicationEventProxy applicationEventProxy;
    @Mock
    private DefaultKey defaultKey;
    @Mock
    private TemporaryQueue tempQueue;
    @Mock
    private SsgKeyEntry keyEntry;
    @Mock
    private QueueSender queueSender;
    @Mock
    private QueueReceiver queueReceiver;

    @Before
    public void setup() throws Exception {
        serverConfig = new ServerConfigStub();
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("defaultKey", DefaultKey.class)).thenReturn(defaultKey);
        when(applicationContext.getBean("serverConfig", ServerConfig.class)).thenReturn(serverConfig);
        when(defaultKey.getSslInfo()).thenReturn(keyEntry);
        assertion = new JmsRoutingAssertion();
        final JmsMessagePropertyRuleSet ruleSet = new JmsMessagePropertyRuleSet(true, new JmsMessagePropertyRule[0]);
        assertion.setRequestJmsMessagePropertyRuleSet(ruleSet);
        assertion.setResponseJmsMessagePropertyRuleSet(ruleSet);
        serverAssertion = new ServerJmsRoutingAssertion(assertion, applicationContext);
        // ensure reply is expected
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
        callback = serverAssertion.new JmsRoutingCallback(policyContext, endpointConfig, new Destination[]{inbound});
        endpoint = new JmsEndpoint();
        endpoint.setOutboundMessageType(JmsOutboundMessageType.ALWAYS_TEXT);
        jmsResponse = new TextMessageStub();
        jmsRequest = new TextMessageStub();
        jmsResponse.setText("<jmsResponse>success</jmsResponse>");
        JmsMessageTestUtility.setDefaultHeaders(jmsResponse);
        when(endpointConfig.getEndpoint()).thenReturn(endpoint);
        when(session.createTextMessage(anyString())).thenReturn(jmsRequest);
        when(session.createTemporaryQueue()).thenReturn(tempQueue);
        when(session.createSender(outbound)).thenReturn(queueSender);
        when(session.createReceiver(inbound, null)).thenReturn(queueReceiver);
        when(queueReceiver.receive(anyLong())).thenReturn(jmsResponse);
    }

    @Test
    public void callbackSetsJmsHeadersOnResponse() throws JMSException {
        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);
        callback.doWork(bag, contextProvider);

        assertEquals(RoutingStatus.ROUTED, policyContext.getRoutingStatus());
        JmsMessageTestUtility.assertDefaultHeadersPresent(policyContext.getResponse().getJmsKnob());
    }

    @Test
    public void callbackErrorSettingJmsHeadersOnResponse() throws JMSException {
        jmsResponse.setThrowExceptionForHeaders(true);
        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        assertTrue(callback.getException() instanceof JMSException);
    }
}
