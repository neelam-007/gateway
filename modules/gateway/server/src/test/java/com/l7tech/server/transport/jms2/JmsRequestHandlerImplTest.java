package com.l7tech.server.transport.jms2;

import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.transport.jms.JmsMessageTestUtility;
import com.l7tech.server.transport.jms.JmsBag;
import com.l7tech.server.transport.jms.JmsRuntimeException;
import com.l7tech.server.transport.jms.TextMessageStub;
import com.l7tech.server.util.EventChannel;
import com.l7tech.util.Config;
import com.l7tech.xml.SoapFaultLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import javax.jms.Session;
import java.util.Properties;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JmsRequestHandlerImplTest {
    private JmsRequestHandlerImpl handler;
    private Config config;
    private Properties connectionProperties;
    private TextMessageStub jmsRequest;
    private TextMessageStub jmsResponse;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private MessageProcessor messageProcessor;
    @Mock
    private StashManagerFactory stashManagerFactory;
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
    private StashManager stashManager;

    @Before
    public void setup() throws Exception {
        config = new ServerConfigStub();
        when(applicationContext.getBean("serverConfig", Config.class)).thenReturn(config);
        when(applicationContext.getBean("messageProcessor", MessageProcessor.class)).thenReturn(messageProcessor);
        when(applicationContext.getBean("stashManagerFactory", StashManagerFactory.class)).thenReturn(stashManagerFactory);
        when(applicationContext.getBean("clusterMaster", ClusterMaster.class)).thenReturn(clusterMaster);
        handler = new JmsRequestHandlerImpl(applicationContext);
        connectionProperties = new Properties();
        jmsRequest = new TextMessageStub();
        jmsRequest.setText("test");
        JmsMessageTestUtility.setDefaultHeaders(jmsRequest);
        jmsResponse = new TextMessageStub();
    }

    /**
     * Ensure that any JMS headers on the JMS Request are grabbed and set on the Policy Enforcement Context's jmsRequest JMS knob.
     */
    @Test
    public void onMessageSetsJmsHeadersOnRequest() throws Exception {
        when(endpointConfig.isQueue()).thenReturn(true);
        when(endpointConfig.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getRequestMaxSize()).thenReturn(2621440L);
        when(endpointConfig.getConnection()).thenReturn(jmsConnection);
        when(jmsConnection.properties()).thenReturn(connectionProperties);
        when(jmsBag.getSession()).thenReturn(session);
        when(session.createTextMessage()).thenReturn(jmsResponse);
        when(stashManagerFactory.createStashManager()).thenReturn(stashManager);
        when(messageProcessor.processMessage(any(PolicyEnforcementContext.class))).thenAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                final PolicyEnforcementContext policyContext = (PolicyEnforcementContext) invocation.getArguments()[0];
                final SoapFaultLevel faultLevel = new SoapFaultLevel();
                // no jmsResponse needed
                faultLevel.setLevel(SoapFaultLevel.DROP_CONNECTION);
                policyContext.setFaultlevel(faultLevel);
                JmsMessageTestUtility.assertDefaultHeadersPresent(policyContext.getRequest().getJmsKnob());
                return AssertionStatus.NONE;
            }
        });

        handler.onMessage(endpointConfig, jmsBag, false, jmsRequest);
    }

    @Test(expected = JmsRuntimeException.class)
    public void onMessageExceptionRetrievingJmsHeaders() throws Exception {
        jmsRequest.setThrowExceptionForHeaders(true);
        when(endpointConfig.isQueue()).thenReturn(true);
        when(endpointConfig.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getRequestMaxSize()).thenReturn(2621440L);
        when(endpointConfig.getConnection()).thenReturn(jmsConnection);
        when(jmsConnection.properties()).thenReturn(connectionProperties);

        handler.onMessage(endpointConfig, jmsBag, false, jmsRequest);

        verify(messageProcessor, never()).processMessage(any(PolicyEnforcementContext.class));
        verify(jmsBag, never()).getSession();
        verify(stashManagerFactory, never()).createStashManager();
        verify(session, never()).createTextMessage();
    }
}
