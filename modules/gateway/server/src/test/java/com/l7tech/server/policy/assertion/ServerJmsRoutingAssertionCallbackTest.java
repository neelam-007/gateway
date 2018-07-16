package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Messages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsOutboundMessageType;
import com.l7tech.gateway.common.transport.jms.JmsReplyType;
import com.l7tech.message.*;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.JmsMessagePropertyRule;
import com.l7tech.policy.assertion.JmsMessagePropertyRuleSet;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsMessageTestUtility;
import com.l7tech.server.transport.jms.JmsUtil;
import com.l7tech.server.transport.jms.TextMessageStub;
import com.l7tech.server.transport.jms2.JmsEndpointConfig;
import com.l7tech.server.transport.jms2.JmsResourceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_HEADER;
import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerJmsRoutingAssertionCallbackTest{
    private static final String JMS_PROP_JMSX_GROUP_ID = "JMSXGroupID";
    private static final String JMS_PROP_JMSX_GROUP_SEQ = "JMSXGroupSeq";
    private static final String JMS_PROP_JMSX_DELIVERY_COUNT  = "JMSXDeliveryCount";
    private static final String JMS_PROP_VALUE_DELIVERY_COUNT = "111";
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
    private TestAudit testAudit;

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
        testAudit = new TestAudit();
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
        request = new Message();
        response = new Message();
        policyContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
        callback = serverAssertion.new JmsRoutingCallback(policyContext, endpointConfig, new Destination[]{inbound});
        endpoint = new JmsEndpoint();
        endpoint.setOutboundMessageType(JmsOutboundMessageType.ALWAYS_TEXT);
        jmsResponse = new TextMessageStub();
        jmsRequest = spy(new TextMessageStub());
        jmsResponse.setText("<jmsResponse>success</jmsResponse>");
        JmsMessageTestUtility.setDefaultHeaders(jmsResponse);
        when(endpointConfig.getEndpoint()).thenReturn(endpoint);
        when(session.createTextMessage(anyString())).thenReturn(jmsRequest);
        when(session.createTemporaryQueue()).thenReturn(tempQueue);
        when(session.createSender(outbound)).thenReturn(queueSender);
        when(session.createReceiver(inbound, null)).thenReturn(queueReceiver);
        when(queueReceiver.receive(anyLong())).thenReturn(jmsResponse);
        injectDependencies(serverAssertion);
    }

    @Test
    public void callbackGetsJmsPropertiesFromHeadersKnobOnRequest() throws JMSException {
        // add test properties to Request message HeadersKnob
        final List<Pair<String, String>> testProperties =
                Arrays.asList(new Pair<>("prop1", "val1"), new Pair<>("prop2", "val2"));

        HeadersKnob requestHeadersKnob = new HeadersKnobSupport();

        for (Pair<String, String> property : testProperties) {
            requestHeadersKnob.addHeader(property.getKey(), property.getValue(), HEADER_TYPE_JMS_PROPERTY);
        }

        request.attachKnob(HeadersKnob.class, requestHeadersKnob);

        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        List<String> jmsRequestPropertyNames = Collections.list(jmsRequest.getPropertyNames());

        assertEquals(testProperties.size(), jmsRequestPropertyNames.size());

        // ensure each property added to the headers knob was set on jmsRequest message
        for (Pair<String, String> testProperty : testProperties) {
            assertTrue(jmsRequestPropertyNames.contains(testProperty.getKey()));
            assertEquals(testProperty.getValue(), jmsRequest.getObjectProperty(testProperty.getKey()));
        }
    }

    @Test
    public void callbackGetsJmsHeadersFromHeadersKnobOnRequest() throws JMSException {
        // add test properties to Request message HeadersKnob
        final List<Pair<String, String>> testProperties =
                Arrays.asList(new Pair<>(JmsUtil.JMS_CORRELATION_ID, "1234"), new Pair<>(JmsUtil.JMS_DELIVERY_MODE, "1"));

        HeadersKnob requestHeadersKnob = new HeadersKnobSupport();

        for (Pair<String, String> property : testProperties) {
            requestHeadersKnob.addHeader(property.getKey(), property.getValue(), HEADER_TYPE_JMS_HEADER);
        }

        request.attachKnob(HeadersKnob.class, requestHeadersKnob);

        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        assertEquals("1234", jmsRequest.getJMSCorrelationID());
        assertEquals(1, jmsRequest.getJMSDeliveryMode());
        assertFalse(jmsRequest.getPropertyNames().hasMoreElements());
    }

    @Test
    public void callbackSetsJmsPropertiesInJmsKnobAndHeadersKnobOnResponse() throws JMSException {
        // add test properties to jmsResponse message
        final List<Pair<String, String>> testProperties =
                Arrays.asList(new Pair<>("prop1", "val1"), new Pair<>("prop2", "val2"));

        for (Pair<String, String> property : testProperties) {
            jmsResponse.setObjectProperty(property.getKey(), property.getValue());
        }

        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        JmsKnob responseJmsKnob = response.getKnob(JmsKnob.class);
        assertNotNull(responseJmsKnob);

        HeadersKnob responseHeadersKnob = response.getKnob(HeadersKnob.class);
        assertNotNull(responseHeadersKnob);

        Map<String, Object> jmsKnobPropertiesMap = responseJmsKnob.getJmsMsgPropMap();
        Collection<Header> headersKnobJmsPropertyHeaders = responseHeadersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY);

        assertEquals(testProperties.size(), jmsKnobPropertiesMap.size());
        assertEquals(testProperties.size(), headersKnobJmsPropertyHeaders.size());

        for (Pair<String, String> testProperty : testProperties) {
            List<String> values = Arrays.asList(responseHeadersKnob.getHeaderValues(testProperty.getKey()));
            assertTrue(values.contains(testProperty.getValue()));
            assertEquals(jmsKnobPropertiesMap.get(testProperty.getKey()), testProperty.getValue());
        }
    }

    @Test
    public void callbackSetsJmsHeadersOnResponse() throws JMSException {
        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);
        callback.doWork(bag, contextProvider);

        assertEquals(RoutingStatus.ROUTED, policyContext.getRoutingStatus());
        assertEquals(10, policyContext.getResponse().getJmsKnob().getHeaderNames().length);
        JmsMessageTestUtility.assertDefaultHeadersPresent(policyContext.getResponse().getJmsKnob());
    }

    @Test
    public void checkCallbackClosesTemporaryQueue() throws JMSException {
        JmsEndpointConfig mockEndpointConfig = mock(JmsEndpointConfig.class);
        JmsEndpoint mockEndPoint = new JmsEndpoint();
        mockEndPoint.setReplyType(JmsReplyType.AUTOMATIC);
        mockEndPoint.setOutboundMessageType(JmsOutboundMessageType.ALWAYS_TEXT);
        when(mockEndpointConfig.getEndpoint()).thenReturn(mockEndPoint);
        when(mockEndpointConfig.getReplyType()).thenReturn(JmsReplyType.AUTOMATIC);
        final TemporaryQueue mockTempQueue = mock(TemporaryQueue.class);
        when(mockTempQueue.getQueueName()).thenReturn("mockTempQueue");
        TextMessage jmsRequest = new TextMessageStub() {
            @Override
            public Destination getJMSReplyTo() throws JMSException {
                return mockTempQueue;
            }
        };
        when(session.createTextMessage(anyString())).thenReturn(jmsRequest);
        when(session.createReceiver(any(Queue.class), anyString())).thenReturn(queueReceiver);
        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);
        callback = serverAssertion.new JmsRoutingCallback(policyContext, mockEndpointConfig, new Destination[1]);
        callback.doWork(bag, contextProvider);
        verify(mockTempQueue, times(1)).delete();
    }

    @Test
    public void callbackErrorSettingJmsHeadersOnResponse() throws JMSException {
        jmsResponse.setThrowExceptionForHeaders(true);
        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        //noinspection ThrowableResultOfMethodCallIgnored
        assertTrue(callback.getException() instanceof JMSException);
    }

    @Test
    public void callbackGetsJmsPropertiesStartingWithJMSXAndJMS_FromHeadersKnobOnRequest() throws JMSException {
        // add test properties to Request message HeadersKnob
        final List<Pair<String, String>> testProperties =
                Arrays.asList(new Pair<>(JMS_PROP_JMSX_GROUP_ID, "val1"), new Pair<>(JMS_PROP_JMSX_GROUP_SEQ, "111"));
        setRules(testProperties, false);
        HeadersKnob requestHeadersKnob = new HeadersKnobSupport();

        for (Pair<String, String> property : testProperties) {
            requestHeadersKnob.addHeader(property.getKey(), property.getValue(), HEADER_TYPE_JMS_PROPERTY);
        }

        request.attachKnob(HeadersKnob.class, requestHeadersKnob);

        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        List<String> jmsRequestPropertyNames = Collections.list(jmsRequest.getPropertyNames());

        assertEquals(testProperties.size(), jmsRequestPropertyNames.size());

        // ensure each property added to the headers knob was set on jmsRequest message
        for (Pair<String, String> testProperty : testProperties) {
            assertTrue(jmsRequestPropertyNames.contains(testProperty.getKey()));
            assertEquals(testProperty.getValue(), jmsRequest.getObjectProperty(testProperty.getKey()));
        }
        executeTestsForAudits(5,
                AssertionMessages.JMS_ROUTING_REQUEST_ROUTED,
                AssertionMessages.JMS_ROUTING_GETTING_RESPONSE,
                AssertionMessages.JMS_ROUTING_GOT_RESPONSE,
                AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE,
                AssertionMessages.JMS_ROUTING_REQUEST_WITH_AUTOMATIC);
        testAudit.reset();
    }

    @Test
    public void callbackGetsJmsPropertiesStartingWithJMSXAndJMS_FromHeadersKnobOnRequest_WithContextVar() throws JMSException, NoSuchVariableException {
        // add test properties to Request message HeadersKnob
        final List<Pair<String, String>> testProperties =
                Arrays.asList(new Pair<>(JMS_PROP_JMSX_GROUP_ID, "val1"), new Pair<>(JMS_PROP_JMSX_GROUP_SEQ, "111"));
        setRules(testProperties, true);

        final HeadersKnob requestHeadersKnob = new HeadersKnobSupport();

        for (final Pair<String, String> property : testProperties) {
            requestHeadersKnob.addHeader(property.getKey(), property.getValue(), HEADER_TYPE_JMS_PROPERTY);
        }

        request.attachKnob(HeadersKnob.class, requestHeadersKnob);

        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);

        callback.doWork(bag, contextProvider);

        List<String> jmsRequestPropertyNames = Collections.list(jmsRequest.getPropertyNames());

        assertEquals(testProperties.size(), jmsRequestPropertyNames.size());

        // ensure each property added to the headers knob was set on jmsRequest message
        for (final Pair<String, String> testProperty : testProperties) {
            assertTrue(jmsRequestPropertyNames.contains(testProperty.getKey()));
            assertEquals(testProperty.getValue(), jmsRequest.getObjectProperty(testProperty.getKey()));
        }
        executeTestsForAudits(5,
                AssertionMessages.JMS_ROUTING_REQUEST_ROUTED,
                AssertionMessages.JMS_ROUTING_GETTING_RESPONSE,
                AssertionMessages.JMS_ROUTING_GOT_RESPONSE,
                AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE,
                AssertionMessages.JMS_ROUTING_REQUEST_WITH_AUTOMATIC);
        testAudit.reset();
    }

    @Test
    public void callbackGetsJmsPropertiesStartingWithJMSXAndJMS_FromHeadersKnobOnRequest_WithAudit() throws JMSException {
        // add test properties to Request message HeadersKnob, keep group seq in the end..
        final List<Pair<String, String>> testProperties =
                Arrays.asList(new Pair<>(JMS_PROP_JMSX_DELIVERY_COUNT, JMS_PROP_VALUE_DELIVERY_COUNT), new Pair<>(JMS_PROP_JMSX_GROUP_SEQ, null));
        setRules(testProperties, false);
        final HeadersKnob requestHeadersKnob = new HeadersKnobSupport();

        for (final Pair<String, String> property : testProperties) {
            requestHeadersKnob.addHeader(property.getKey(), property.getValue(), HEADER_TYPE_JMS_PROPERTY);
        }

        request.attachKnob(HeadersKnob.class, requestHeadersKnob);

        JmsBag bag = new JmsBag(null, null, connection, session, null, queueSender, null);
        doThrow(new NumberFormatException("For input string: \"\"")).when(jmsRequest).setObjectProperty(JMS_PROP_JMSX_GROUP_SEQ, null);

        callback.doWork(bag, contextProvider);

        List<String> jmsRequestPropertyNames = Collections.list(jmsRequest.getPropertyNames());

        assertEquals(testProperties.size() - 1, jmsRequestPropertyNames.size());

        // ensure each property added to the headers knob was set on jmsRequest message
        for (int counter = 0; counter < testProperties.size() - 1; counter++) {
            Pair<String, String> testProperty = testProperties.get(counter);
            assertTrue(jmsRequestPropertyNames.contains(testProperty.getKey()));
            assertEquals(testProperty.getValue(), jmsRequest.getObjectProperty(testProperty.getKey()));
        }
        executeTestsForAudits(6,
                AssertionMessages.JMS_ROUTING_REQUEST_ROUTED,
                AssertionMessages.JMS_ROUTING_GETTING_RESPONSE,
                AssertionMessages.JMS_ROUTING_GOT_RESPONSE,
                AssertionMessages.JMS_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE,
                AssertionMessages.JMS_ROUTING_REQUEST_WITH_AUTOMATIC,
                AssertionMessages.JMS_ROUTING_NOT_SETTABLE_JMS_PROPERTY);
        testAudit.reset();
    }

    private void setRules(List<Pair<String, String>> testProperties, boolean useContextVariable) {
        List<JmsMessagePropertyRule> rules = new ArrayList<>();
        for (final Pair<String, String> property : testProperties) {
            final JmsMessagePropertyRule rule = new JmsMessagePropertyRule();
            rule.setName(property.getKey());
            if(useContextVariable) {
                rule.setCustomPattern("${request.jms.property." + property.getKey() + "}");
                rule.setPassThru(false);
            } else {
                rule.setPassThru(true);
            }

            rules.add(rule);
        }

        final JmsMessagePropertyRuleSet ruleSet = new JmsMessagePropertyRuleSet(false, rules.toArray(new JmsMessagePropertyRule[testProperties.size()]));
        assertion.setRequestJmsMessagePropertyRuleSet(ruleSet);
    }

    private void injectDependencies(ServerJmsRoutingAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                        .unmodifiableMap()
        );
    }

    private void executeTestsForAudits(int numberOfAudits, Messages.M... messages) {
        assertEquals(numberOfAudits, testAudit.getAuditCount());
        if (numberOfAudits > 0 && messages.length > 0) {
            for (final Messages.M message : messages) {
                assertEquals(Boolean.TRUE, testAudit.isAuditPresent(message));
            }
        }
    }
}
