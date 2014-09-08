package com.l7tech.server.transport.jms2;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.jms.*;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.Config;
import com.l7tech.util.Pair;
import com.l7tech.xml.SoapFaultLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import javax.jms.Destination;
import javax.jms.Session;
import javax.naming.Context;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JmsRequestHandlerImplTest {
    private static final String NEW_CORRELATION_ID = "12345";

    private JmsRequestHandlerImpl handler;
    private Properties connectionProperties;
    private TextMessageStub jmsRequest;
    private TextMessageStub jmsResponse;

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private MessageProcessor messageProcessor;
    @Mock
    private EventChannel eventChannel;
    @Mock
    private ClusterMaster clusterMaster;
    @Mock
    private JmsEndpointConfig endpointConfig;
    @Mock
    private JmsBag jmsBag;
    @Mock
    private JmsConnection jmsConnection;
    @Mock
    private JmsEndpoint endpoint;
    @Mock
    private Session session;
    @Mock
    private Context jndiContext;
    @Mock
    private javax.jms.MessageProducer mockProducer;

    @Before
    public void setup() throws Exception {
        when(applicationContext.getBean("serverConfig", Config.class)).thenReturn(new ServerConfigStub());
        when(applicationContext.getBean("messageProcessor", MessageProcessor.class)).thenReturn(messageProcessor);
        when(applicationContext.getBean("clusterMaster", ClusterMaster.class)).thenReturn(clusterMaster);
        when(applicationContext.getBean("stashManagerFactory", StashManagerFactory.class))
                .thenReturn(TestStashManagerFactory.getInstance());

        handler = spy(new JmsRequestHandlerImpl(applicationContext));
        connectionProperties = new Properties();

        jmsRequest = new TextMessageStub();
        jmsRequest.setText("test");
        JmsMessageTestUtility.setDefaultHeaders(jmsRequest);
        jmsResponse = new TextMessageStub();
    }

    /**
     * Ensure that any JMS properties on the jmsRequest are grabbed and set equally on the Policy Enforcement Context
     * Request message's JmsKnob and HeadersKnob.
     */
    @Test
    public void onMessageSetsJmsPropertiesOnRequest() throws Exception {
        // add properties to jmsRequest message
        final List<Pair<String, String>> properties =
                Arrays.asList(new Pair<>("prop1", "val1"), new Pair<>("prop2", "val2"));

        for (Pair<String, String> property : properties) {
            jmsRequest.setObjectProperty(property.getKey(), property.getValue());
        }

        mockJmsEndpoint();

        when(jmsBag.getSession()).thenReturn(session);
        when(session.createTextMessage()).thenReturn(jmsResponse);

        final AtomicReference<JmsKnob> jmsKnobRef = new AtomicReference<>();
        final AtomicReference<HeadersKnob> headersKnobRef = new AtomicReference<>();

        when(messageProcessor.processMessage(any(PolicyEnforcementContext.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final PolicyEnforcementContext policyContext = (PolicyEnforcementContext) invocation.getArguments()[0];

                setContextSoapFaultLevel(policyContext, SoapFaultLevel.DROP_CONNECTION); // no jmsResponse needed

                jmsKnobRef.set(policyContext.getRequest().getKnob(JmsKnob.class));
                headersKnobRef.set(policyContext.getRequest().getKnob(HeadersKnob.class));

                return AssertionStatus.NONE;
            }
        });

        handler.onMessage(endpointConfig, jmsBag, false, jmsRequest);

        assertNotNull(jmsKnobRef.get());
        assertEquals(10, jmsKnobRef.get().getHeaderNames().length);
        JmsMessageTestUtility.assertDefaultHeadersPresent(jmsKnobRef.get());

        HeadersKnob headersKnob = headersKnobRef.get();
        assertNotNull(headersKnob);

        Collection<Header> jmsPropertyHeaders = headersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY);

        assertEquals(2, jmsPropertyHeaders.size());

        for (Pair<String, String> property : properties) {
            List<String> values = Arrays.asList(headersKnob.getHeaderValues(property.getKey()));
            assertTrue(values.contains(property.getValue()));
        }
    }

    /**
     * Ensure that any JMS Property headers set on the Policy Enforcement Context Response message's HeadersKnob
     * (e.g. in the case of using the Manage Headers/Properties Assertion, or JMS Routing Assertion) are grabbed
     * and set on the jmsResponse.
     */
    @Test
    public void onMessageSetsResponseJmsPropertyHeadersOnJmsResponse() throws Exception {
        Destination replyDestination = new DestinationStub("replyDestination");
        mockJmsEndpoint();
        when(endpointConfig.getResponseDestination(jmsRequest, jndiContext)).thenReturn(replyDestination);

        when(jmsBag.getSession()).thenReturn(session);
        when(session.createTextMessage()).thenReturn(jmsResponse);
        when(jmsBag.getJndiContext()).thenReturn(jndiContext);

        when(messageProcessor.processMessage(any(PolicyEnforcementContext.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final PolicyEnforcementContext policyContext = (PolicyEnforcementContext) invocation.getArguments()[0];

                setContextSoapFaultLevel(policyContext, SoapFaultLevel.GENERIC_FAULT); // no jmsResponse needed

                Message response = policyContext.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.RESPONSE), false);
                response.initialize(ContentTypeHeader.XML_DEFAULT, new byte[0]);

                // add JMS Property Header to Response HeadersKnob
                HeadersKnob headersKnob = new HeadersKnobSupport();
                response.attachKnob(HeadersKnob.class, headersKnob);
                headersKnob.addHeader("prop1", "val1", HEADER_TYPE_JMS_PROPERTY);

                policyContext.setRoutingStatus(RoutingStatus.ROUTED);

                return AssertionStatus.NONE;
            }
        });
        doNothing().when(mockProducer).send(any(javax.jms.Message.class), anyInt(), anyInt(), anyLong());
        when(handler.getMessageProducer(replyDestination, session)).thenReturn(mockProducer);

        handler.onMessage(endpointConfig, jmsBag, false, jmsRequest);

        Enumeration e = jmsResponse.getPropertyNames();
        assertTrue(e.hasMoreElements());

        final String name = (String) e.nextElement();
        assertFalse(e.hasMoreElements());

        final Object value = jmsResponse.getObjectProperty(name);

        assertEquals("prop1", name);
        assertEquals("val1", value);
        assertFalse(e.hasMoreElements());

        assertEquals(JmsMessageTestUtility.DELIVERYMODE, jmsResponse.getJMSDeliveryMode());
        assertEquals(JmsMessageTestUtility.PRIORITY, jmsResponse.getJMSPriority());
        assertEquals(JmsMessageTestUtility.EXPIRATION, jmsResponse.getJMSExpiration());
        assertEquals(JmsMessageTestUtility.CORRELATIONID, jmsResponse.getJMSCorrelationID());
    }

    @Test
    public void onMessageSetsResponseJmsHeadersOnJmsResponse() throws Exception {
        Destination replyDestination = new DestinationStub("replyDestination");
        mockJmsEndpoint();
        when(endpointConfig.getResponseDestination(jmsRequest, jndiContext)).thenReturn(replyDestination);

        when(jmsBag.getSession()).thenReturn(session);
        when(session.createTextMessage()).thenReturn(jmsResponse);
        when(jmsBag.getJndiContext()).thenReturn(jndiContext);

        when(messageProcessor.processMessage(any(PolicyEnforcementContext.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final PolicyEnforcementContext policyContext = (PolicyEnforcementContext) invocation.getArguments()[0];

                setContextSoapFaultLevel(policyContext, SoapFaultLevel.GENERIC_FAULT); // no jmsResponse needed

                Message response = policyContext.getOrCreateTargetMessage(new MessageTargetableSupport(TargetMessageType.RESPONSE), false);
                response.initialize(ContentTypeHeader.XML_DEFAULT, new byte[0]);

                // add JMS Standard Header to Response HeadersKnob
                HeadersKnob headersKnob = new HeadersKnobSupport();
                response.attachKnob(HeadersKnob.class, headersKnob);
                headersKnob.addHeader("JMSCorrelationID", NEW_CORRELATION_ID, HEADER_TYPE_JMS_PROPERTY);

                policyContext.setRoutingStatus(RoutingStatus.ROUTED);

                return AssertionStatus.NONE;
            }
        });
        doNothing().when(mockProducer).send(any(javax.jms.Message.class), anyInt(), anyInt(), anyLong());
        when(handler.getMessageProducer(replyDestination, session)).thenReturn(mockProducer);

        handler.onMessage(endpointConfig, jmsBag, false, jmsRequest);
        //no properties added to the jms response
        Enumeration e = jmsResponse.getPropertyNames();
        assertFalse(e.hasMoreElements());

        assertEquals(NEW_CORRELATION_ID, jmsResponse.getJMSCorrelationID());

        assertEquals(JmsMessageTestUtility.DELIVERYMODE, jmsResponse.getJMSDeliveryMode());
        assertEquals(JmsMessageTestUtility.PRIORITY, jmsResponse.getJMSPriority());
        assertEquals(JmsMessageTestUtility.EXPIRATION, jmsResponse.getJMSExpiration());
    }

    @Test(expected = JmsRuntimeException.class)
    public void onMessageExceptionRetrievingJmsHeaders() throws Exception {
        mockJmsEndpoint();

        jmsRequest.setThrowExceptionForHeaders(true);

        handler.onMessage(endpointConfig, jmsBag, false, jmsRequest); // will fail if JmsRuntimeException not thrown
    }

    private void setContextSoapFaultLevel(PolicyEnforcementContext policyContext, int level) {
        final SoapFaultLevel faultLevel = new SoapFaultLevel();
        faultLevel.setLevel(level);
        policyContext.setFaultlevel(faultLevel);
    }

    private void mockJmsEndpoint() {
        when(endpointConfig.isQueue()).thenReturn(true);
        when(endpointConfig.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getRequestMaxSize()).thenReturn(2621440L);
        when(endpointConfig.getConnection()).thenReturn(jmsConnection);
        when(jmsConnection.properties()).thenReturn(connectionProperties);
    }
}
