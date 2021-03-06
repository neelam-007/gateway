package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.MQRFH;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.mqnative.*;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.HasHeaders;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.ServerAddHeaderAssertion;
import com.l7tech.server.policy.variable.MessageSelector;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.server.transport.SsgActiveConnectorManagerStub;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.MockInjector;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static com.ibm.mq.constants.CMQC.*;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.buildMqNativeKnob;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ServerMqNativeRoutingAssertionTest {

    private static final String REQUEST_QUEUE = "REQUESTQUEUE";
    private static final String REPLY_QUEUE = "REPLYQUEUE";

    private MqNativeRoutingAssertion assertion;

    private PolicyEnforcementContext context;
    private ServerMqNativeRoutingAssertion fixture;
    private GenericApplicationContext applicationContext;
    private MqNativeResourceManager resourceManagerSpy;
    // static final is needed for requestQ to workaround a potential bug in Mockito where it is operating on a different
    // copy inside the mocked method MQQueue.put().
    private static final Stack requestQ = new Stack();
    private static final Logger logger = Logger.getLogger(ServerMqNativeRoutingAssertionTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);

    @Mock
    private MQQueueManager mqQueueManager;
    @Mock
    private MQQueue requestQueue;
    @Mock
    private MQQueue replyQueue;

    @Before
    public void init() throws Exception {
        //Prepare the ServerMqNativeRoutingAssertion
        applicationContext = new GenericApplicationContext();
        RootBeanDefinition config = new RootBeanDefinition(ServerConfigStub.class);
        //config.addQualifier(new AutowireCandidateQualifier(Inject.class));
        applicationContext.registerBeanDefinition("config", config);

        assertion = new MqNativeRoutingAssertion();
        ConstructorArgumentValues assertionConstructors = new ConstructorArgumentValues();
        assertionConstructors.addGenericArgumentValue(assertion);
        assertionConstructors.addGenericArgumentValue(applicationContext);
        RootBeanDefinition serverMqNativeRoutingAssertion = new RootBeanDefinition(ServerMqNativeRoutingAssertion.class, assertionConstructors, null);
        serverMqNativeRoutingAssertion.addQualifier(new AutowireCandidateQualifier(Inject.class));

        AnnotationConfigUtils.registerAnnotationConfigProcessors(applicationContext);
        applicationContext.registerBeanDefinition("auditFactory", new RootBeanDefinition(LoggingAudit.LoggingAuditFactory.class));
        applicationContext.registerBeanDefinition("messageProcessingEventChannel", new RootBeanDefinition(EventChannel.class));
        applicationContext.registerBeanDefinition("injector", new RootBeanDefinition(MockInjector.class));
        applicationContext.registerBeanDefinition("stashManagerFactory", new RootBeanDefinition(TestStashManagerFactory.class));
        applicationContext.registerBeanDefinition("securePasswordManager", new RootBeanDefinition(SecurePasswordManagerStub.class));
        applicationContext.registerBeanDefinition("ssgActiveConnectorManager", new RootBeanDefinition(SsgActiveConnectorManagerStub.class));
        applicationContext.registerBeanDefinition("serverMqNativeRoutingAssertion", serverMqNativeRoutingAssertion);
        applicationContext.refresh();

        fixture = (ServerMqNativeRoutingAssertion) applicationContext.getBean("serverMqNativeRoutingAssertion");

        //Mock the MQQueue
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                requestQ.push(invocation.getArguments()[0]);
                return null;
            }
        }).when(requestQueue).put(any(MQMessage.class), any(MQPutMessageOptions.class));

        //Mock the MQQueue
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                return REPLY_QUEUE;
            }
        }).when(replyQueue).getName();

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws IOException, MQException {
                MQMessage replyMessage = (MQMessage) invocation.getArguments()[0];
                replyMessage.format = MQFMT_RF_HEADER_2;
                replyMessage.seek(0);
                MQRFH2 rfh2 = new MQRFH2();
                rfh2.setFieldValue("folder", "rfh2ReplyField1", "rfh2ReplyValue1");

                MQHeaderList headerList = new MQHeaderList();
                headerList.add(rfh2);
                headerList.write(replyMessage);

                replyMessage.setStringProperty("folder.propertyReplyField1", "propertyReplyValue1");
                return replyMessage;

            }
        }).when(replyQueue).get(any(MQMessage.class), any(MQGetMessageOptions.class));

        //Mock the MQQueueManager
        when(mqQueueManager.accessQueue(any(String.class), any(int.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                String queueName = (String) invocationOnMock.getArguments()[0];
                if (REQUEST_QUEUE.equals(queueName)) {
                    return requestQueue;
                }
                if (REPLY_QUEUE.equals(queueName)) {
                    return replyQueue;
                }
                return null;
            }
        });

        MqNativeResourceManager resourceManager = MqNativeResourceManager.getInstance((Config) applicationContext.getBean("config"), new ApplicationEventProxy());

        final PoolableCachedConnectionFactory poolableCachedConnectionFactory = new PoolableCachedConnectionFactory(mqQueueManager);
        final PoolableCachedConnectionFactory poolableCachedConnectionFactorySpy = spy(poolableCachedConnectionFactory);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws MQException {
                MqNativeCachedConnectionPool.CachedConnection newConnection =
                        new MqNativeCachedConnectionPool.CachedConnection(mqQueueManager, "TEST", 1);
                return newConnection;
            }
        }).when(poolableCachedConnectionFactorySpy).makeObject();

        resourceManagerSpy = spy(resourceManager);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws MQException {
                final MqNativeEndpointConfig mqCfg = (MqNativeEndpointConfig) invocation.getArguments()[0];
                final MqNativeEndpointConfig.MqNativeEndpointKey key = mqCfg.getMqEndpointKey();

                try {
                    final MqNativeCachedConnectionPool newConn = new MqNativeCachedConnectionPool();
                    poolableCachedConnectionFactorySpy.setMqNativeEndpointConfig(mqCfg);

                    GenericObjectPool<MqNativeCachedConnectionPool.CachedConnection> cachedConnections =
                            new GenericObjectPool<>(poolableCachedConnectionFactorySpy,
                                    mqCfg.getConnectionPoolMaxActive(), GenericObjectPool.WHEN_EXHAUSTED_BLOCK,
                                    mqCfg.getConnectionPoolMaxWait(), mqCfg.getConnectionPoolMaxIdle());

                    newConn.setCachedConnections(cachedConnections);

                    newConn.ref(); // referenced by caller

                    // server config controlled connection pool props -- may not need this
                    if (resourceManagerSpy.getCacheConfigReference().get().getMaximumSize() > 0) {
                        newConn.ref(); // referenced from cache

                        // replace connection if the endpoint already exists
                        final MqNativeCachedConnectionPool existingConn = resourceManagerSpy.getConnectionHolder().put(key, newConn);
                        if (existingConn != null) {
                            existingConn.unRef(); // clear cache reference
                        }
                    }

                    return newConn;
                } catch (Throwable me) {
                    throw new MqNativeRuntimeException(me);
                }
            }
        }).when(resourceManagerSpy).newConnectionPool(any(MqNativeEndpointConfig.class));

        fixture.setMqNativeResourceManager(resourceManagerSpy);

        SsgActiveConnector connector = new SsgActiveConnector();
        connector.setProperty(PROPERTIES_KEY_IS_INBOUND, "false");
        connector.setEnabled(true);
        connector.setType(ACTIVE_CONNECTOR_TYPE_MQ_NATIVE);
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, REQUEST_QUEUE);
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, REPLY_QUEUE);
        connector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, MqNativeReplyType.REPLY_SPECIFIED_QUEUE.name());

        SsgActiveConnectorManager connectorManager = (SsgActiveConnectorManager) applicationContext.getBean("ssgActiveConnectorManager");
        connectorManager.save(connector);

        assertion.setSsgActiveConnectorGoid(connector.getGoid());

        MessageSelector.registerSelector(MqNativeRoutingAssertion.MQ,
                new MqNativeModuleLoadListener.MqNativeHeaderSelector(MqNativeRoutingAssertion.MQ + ".", true,
                        Arrays.<Class<? extends HasHeaders>>asList(MqNativeKnob.class)));
    }

    @After
    public void tearDown() throws Exception {
        requestQ.clear();
    }

    private PolicyEnforcementContext makeContext(MQMessage mqMessage) throws IOException, MQDataException, MQException {

        Pair<byte[], byte[]> mqRequestHeaderPayload = MqNativeUtils.parseHeaderPayload(mqMessage);
        InputStream requestStream = new ByteArrayInputStream(mqRequestHeaderPayload.right);

        Message request = new Message();
        request.initialize(((StashManagerFactory) applicationContext.getBean("stashManagerFactory")).createStashManager(),
                ContentTypeHeader.parseValue(ContentTypeHeader.XML_DEFAULT.getFullValue()), requestStream, Integer.MAX_VALUE);

        MqMessageProxy mqMessageProxy = new MqMessageProxy(mqMessage);
        MqNativeKnob mqNativeKnob = buildMqNativeKnob(null, mqMessageProxy);
        request.attachKnob(mqNativeKnob, MqNativeKnob.class);
        Message response = new Message();
        response.attachKnob(buildMqNativeKnob(new MqMessageProxy(new MQMessage())), true, MqNativeKnob.class);
        PolicyEnforcementContext c = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        return c;
    }

    private void addHeader(TargetMessageType targetMessageType, String name, String value) throws IOException, PolicyAssertionException {

        AddHeaderAssertion addHeaderAssertion = new AddHeaderAssertion();
        addHeaderAssertion.setTarget(targetMessageType);
        addHeaderAssertion.setHeaderName(name);
        addHeaderAssertion.setHeaderValue(value);
        ServerAddHeaderAssertion serverAddHeaderAssertion = new ServerAddHeaderAssertion(addHeaderAssertion);
        serverAddHeaderAssertion.checkRequest(context);

    }


    @Test
    public void testPassThrough() throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy original = new MqMessageProxy(mqMessage);
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(original.getPrimaryHeaderValue("folder.rfh2Field1"), routed.getPrimaryHeaderValue("folder.rfh2Field1"));
        assertEquals(original.getMessageProperty("folder.propertyField1"), routed.getMessageProperty("folder.propertyField1"));
        assertEquals(original.getMessageDescriptor("expiry"), routed.getMessageDescriptor("expiry"));

    }

    /**
     * Test message without header and properties
     * @throws Exception
     */
    @Test
    public void testPassThroughSimpleMessage() throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy original = new MqMessageProxy(mqMessage);
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(original.getHeaders().size(), routed.getHeaders().size());
        assertEquals(original.getMessageProperties().size(), original.getMessageProperties().size());


    }

    @Test
    public void testOverrideRequestMessageDescriptor() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        context.setVariable("contextVariableName", "mqnative.md.applicationOriginData");
        context.setVariable("contextVariableValue", "applicationOriginDataValue");

        addHeader(TargetMessageType.REQUEST, "mqnative.md.expiry", "1234");
        addHeader(TargetMessageType.REQUEST, "mqnative.md.version", "2");
        addHeader(TargetMessageType.REQUEST, "mqnative.md.accountingToken", HexUtils.encodeBase64("accountingToken".getBytes()));
        addHeader(TargetMessageType.REQUEST, "mqnative.md.applicationIdData", "applicationIdData");
        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals("1234", String.valueOf(routed.getMessageDescriptor("expiry")));
        assertEquals("2", String.valueOf(routed.getMessageDescriptor("version")));
        assertEquals("accountingToken", new String((byte[]) routed.getMessageDescriptor("accountingToken")));
        assertEquals("applicationIdData", routed.getMessageDescriptor("applicationIdData"));
        assertEquals(context.getVariable("contextVariableValue"), routed.getMessageDescriptor("applicationOriginData"));
    }

    @Test
    public void testOverrideRequestMessageProperty() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        context.setVariable("contextVariableName", "mqnative.property.folder.propertyField3");
        context.setVariable("contextVariableValue", "propertyValue3");

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        addHeader(TargetMessageType.REQUEST, "mqnative.property.folder.propertyField1", "overridedPropertyValue1");
        addHeader(TargetMessageType.REQUEST, "mqnative.property.folder.propertyField2", "propertyValue2");
        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals("overridedPropertyValue1", routed.getMessageProperty("folder.propertyField1"));
        assertEquals("propertyValue2", routed.getMessageProperty("folder.propertyField2"));
        assertEquals(context.getVariable("contextVariableValue"), routed.getMessageProperty("folder.propertyField3"));
    }

    @Test
    public void testOverrideRequestMessageHeader() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        context.setVariable("contextVariableName", "mqnative.additionalheader.folder.rfh2Field3");
        context.setVariable("contextVariableValue", "rfh2Value3");

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        addHeader(TargetMessageType.REQUEST, "mqnative.additionalheader.folder.rfh2Field1", "overridedRfh2Value1");
        addHeader(TargetMessageType.REQUEST, "mqnative.additionalheader.folder.rfh2Field2", "rfh2Field2");
        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals("overridedRfh2Value1", routed.getPrimaryHeaderValue("folder.rfh2Field1"));
        assertEquals("rfh2Field2", routed.getPrimaryHeaderValue("folder.rfh2Field2"));
        assertEquals(context.getVariable("contextVariableValue"), routed.getPrimaryHeaderValue("folder.rfh2Field3"));
    }

    @Test
    public void testRequestHeaderTypeOriginalHeaderWithMQRFH2() throws Exception {
        testRequestHeaderTypeOriginalHeader(createMqMessage(), MQRFH2.class);

    }

    @Test
    public void testRequestHeaderTypeOriginalHeaderWithMQRFH1() throws Exception {
        testRequestHeaderTypeOriginalHeader(createMQRFH1MqMessage(), MQRFH.class);

    }

    public void testRequestHeaderTypeOriginalHeader(MQMessage message, Class type) throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        assertion.setRequestMqHeaderType(MqNativeMessageHeaderType.ORIGINAL);

        context = makeContext(message);
        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        routedMessage.seek(0);
        MQHeaderList headerList = new MQHeaderList(routedMessage);
        assertTrue(type.isAssignableFrom(headerList.get(0).getClass()));

    }

    @Test
    public void testRequestHeaderTypeMQRFH2WithMQRFH2() throws Exception {
        testRequestHeaderTypeMQRFH2(createMqMessage());
    }

    @Test
    public void testRequestHeaderTypeMQRFH2WithMQRFH1() throws Exception {
        testRequestHeaderTypeMQRFH2(createMQRFH1MqMessage());
    }

    public void testRequestHeaderTypeMQRFH2(MQMessage message) throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        assertion.setRequestMqHeaderType(MqNativeMessageHeaderType.MQRFH2);

        context = makeContext(message);
        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        routedMessage.seek(0);
        MQHeaderList headerList = new MQHeaderList(routedMessage);
        assertTrue(headerList.get(0) instanceof MQRFH2);

    }

    @Test
    public void testRequestHeaderTypeMQRFH1WithMQRFH2() throws Exception {
        testRequestHeaderTypeMQRFH1(createMqMessage());
    }

    @Test
    public void testRequestHeaderTypeMQRFH1WithMQRFH1() throws Exception {
        testRequestHeaderTypeMQRFH1(createMQRFH1MqMessage());
    }

    public void testRequestHeaderTypeMQRFH1(MQMessage message) throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        assertion.setRequestMqHeaderType(MqNativeMessageHeaderType.MQRFH1);

        context = makeContext(message);
        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        routedMessage.seek(0);
        MQHeaderList headerList = new MQHeaderList(routedMessage);
        assertTrue(headerList.get(0) instanceof MQRFH);

    }

    @Test
    public void testOverrideRequestMessageDescriptorWithInvalidField() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        context.setVariable("contextVariableName", "mqnative.md.invalid");
        context.setVariable("contextVariableValue", "invalid");

        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(null, routed.getMessageDescriptor("invalid"));
    }

    @Test
    public void testOverrideRequestWithInvalidVariable() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(null, routed.getMessageDescriptor("invalid"));
    }

    @Test
    public void testOverrideRequestMessageDescriptorWithInvalidData() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        context.setVariable("contextVariableName", "mqnative.md.expiry");
        context.setVariable("contextVariableValue", "InvalidData");

        Map<String, String> map = new HashMap<String, String>();
        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");
        assertion.setRequestMessageAdvancedProperties(map);

        AssertionStatus status = fixture.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testOverrideRequestMessageDescriptorWithInvalidDateFormat() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        context.setVariable("contextVariableName", "mqnative.md.putDateTime");
        context.setVariable("contextVariableValue", "InvalidData");

        addHeader(TargetMessageType.REQUEST, "${contextVariableName}", "${contextVariableValue}");

        AssertionStatus status = fixture.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testRemoveRequestMessageHeaderMQRFH2() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        addHeader(TargetMessageType.REQUEST,"mqnative.additionalheader.folder.rfh2Field1", "");
        assertion.setRequestMqHeaderType(MqNativeMessageHeaderType.ORIGINAL);

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(null, routed.getPrimaryHeaderValue("folder.rfh2Field1"));
    }

    @Test
    public void testRemoveRequestMessageHeaderMQRFH1() throws Exception {
        MQMessage mqMessage = createMQRFH1MqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        addHeader(TargetMessageType.REQUEST, "mqnative.additionalheader.rfhField1", "");
        assertion.setRequestMqHeaderType(MqNativeMessageHeaderType.ORIGINAL);

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(null, routed.getPrimaryHeaderValue("rfhField1"));
    }

    @Test
    public void testRemoveMessageProperty() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        addHeader(TargetMessageType.REQUEST, "mqnative.property.folder.propertyField1", "");

        fixture.checkRequest(context);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(null, routed.getMessageProperty("folder.propertyField1"));
    }

    @Test
    public void testMultiValuedHeaderOverride() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageHeaders(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);
        assertion.setRequestCopyPropertyToHeader(true);

        fixture.checkRequest(context);
        MqMessageProxy original = new MqMessageProxy(mqMessage);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(original.getMessageProperty("folder.propertyField1"), routed.getPrimaryHeaderValue("folder.propertyField1"));
        assertEquals(original.getMessageProperty("folder.propertyField2"), routed.getPrimaryHeaderValue("folder.propertyField2"));
    }

    @Test
    public void testMultiValuedPropertyOverride() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);

        assertion.setRequestCopyHeaderToProperty(true);

        fixture.checkRequest(context);
        MqMessageProxy original = new MqMessageProxy(mqMessage);
        MQMessage routedMessage = (MQMessage) requestQ.pop();
        MqMessageProxy routed = new MqMessageProxy(routedMessage);

        assertEquals(original.getPrimaryHeaderValue("folder.rfh2Field1"), routed.getMessageProperty("folder.rfh2Field1"));
    }


    @Test
    public void testPassThroughResponse() throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setResponseMqNativeMessagePropertyRuleSet(ruleSet);

        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        fixture.checkRequest(context);
        MqNativeKnob mqNativeKnob = context.getResponse().getKnob(MqNativeKnob.class);
        MqMessageProxy replyMessage = (MqMessageProxy) mqNativeKnob.getMessage();
        assertEquals("rfh2ReplyValue1", replyMessage.getPrimaryHeaderValue("folder.rfh2ReplyField1"));
        assertEquals("propertyReplyValue1", replyMessage.getMessageProperty("folder.propertyReplyField1"));
    }

    @Test
    public void testOverrideResponseMessageDescriptor() throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setResponseMqNativeMessagePropertyRuleSet(ruleSet);

        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        addHeader(TargetMessageType.RESPONSE, "mqnative.md.expiry", "1234");
        fixture.checkRequest(context);
        MqNativeKnob mqNativeKnob = context.getResponse().getKnob(MqNativeKnob.class);
        MqMessageProxy replyMessage = (MqMessageProxy) mqNativeKnob.getMessage();
        assertEquals("1234", String.valueOf(replyMessage.getMessageDescriptor("expiry")));
    }

    @Test
    public void testOverrideResponseMessageProperties() throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setResponseMqNativeMessagePropertyRuleSet(ruleSet);

        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        addHeader(TargetMessageType.RESPONSE, "mqnative.property.folder.propertyReplyField2", "propertyReplyValue2");
        fixture.checkRequest(context);
        MqNativeKnob mqNativeKnob = context.getResponse().getKnob(MqNativeKnob.class);
        MqMessageProxy replyMessage = (MqMessageProxy) mqNativeKnob.getMessage();
        assertEquals("propertyReplyValue2", String.valueOf(replyMessage.getMessageProperty("folder.propertyReplyField2")));
    }

    @Test
    public void testOverrideResponseHeader() throws Exception {
        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setResponseMqNativeMessagePropertyRuleSet(ruleSet);

        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        addHeader(TargetMessageType.RESPONSE, "mqnative.additionalheader.folder.rfh2ReplyField2", "rfh2ReplyValue2");
        fixture.checkRequest(context);
        MqNativeKnob mqNativeKnob = context.getResponse().getKnob(MqNativeKnob.class);
        MqMessageProxy replyMessage = (MqMessageProxy) mqNativeKnob.getMessage();
        assertEquals("rfh2ReplyValue2", String.valueOf(replyMessage.getPrimaryHeaderValue("folder.rfh2ReplyField2")));
    }

    @BugId("SSG-8680")
    @Test
    public void mqExceptionWarningCompletionStatus() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        final MqNativeResourceManager resourceManager = MqNativeResourceManager.getInstance((Config) applicationContext.getBean("config"), new ApplicationEventProxy());
        final MqNativeResourceManager resourceManagerSpy = spy(resourceManager);
        doThrow(new MQException("testMessage", "testMessageId", MQConstants.MQRC_PRIORITY_EXCEEDS_MAXIMUM, MQConstants.MQCC_WARNING)).when(resourceManagerSpy).doWithMqResources(any(MqNativeEndpointConfig.class), any(MqNativeResourceManager.MqTaskCallback.class));
        fixture.setMqNativeResourceManager(resourceManagerSpy);

        assertEquals(AssertionStatus.NONE, fixture.checkRequest(context));
        assertEquals(MQConstants.MQCC_WARNING, context.getVariable("mq.completion.code"));
        assertEquals(MQConstants.MQRC_PRIORITY_EXCEEDS_MAXIMUM, context.getVariable("mq.reason.code"));
        // should not retry
        verify(resourceManagerSpy, times(1)).doWithMqResources(any(MqNativeEndpointConfig.class), any(MqNativeResourceManager.MqTaskCallback.class));
    }

    @Test
    public void mqExceptionFailedCompletionStatus() throws Exception {
        MQMessage mqMessage = createMqMessage();
        context = makeContext(mqMessage);
        final MqNativeResourceManager resourceManager = MqNativeResourceManager.getInstance((Config) applicationContext.getBean("config"), new ApplicationEventProxy());
        final MqNativeResourceManager resourceManagerSpy = spy(resourceManager);
        doThrow(new MQException("testMessage", "testMessageId", MQConstants.MQRC_PRIORITY_ERROR , MQConstants.MQCC_FAILED)).when(resourceManagerSpy).doWithMqResources(any(MqNativeEndpointConfig.class), any(MqNativeResourceManager.MqTaskCallback.class));
        fixture.setMqNativeResourceManager(resourceManagerSpy);

        assertEquals(AssertionStatus.SERVER_ERROR, fixture.checkRequest(context));
        assertEquals(MQConstants.MQCC_FAILED, context.getVariable("mq.completion.code"));
        assertEquals(MQConstants.MQRC_PRIORITY_ERROR, context.getVariable("mq.reason.code"));
        // should have retried
        verify(resourceManagerSpy, times(5)).doWithMqResources(any(MqNativeEndpointConfig.class), any(MqNativeResourceManager.MqTaskCallback.class));
    }

    private MQMessage createMqMessage() throws IOException, MQException {
        MQMessage mqMessage = new MQMessage();
        mqMessage.format = MQFMT_RF_HEADER_2;
        MQRFH2 rfh2 = new MQRFH2();
        rfh2.setFieldValue("folder", "rfh2Field1", "rhf2Value1");

        MQHeaderList headerList = new MQHeaderList();
        headerList.add(rfh2);
        headerList.write(mqMessage);

        mqMessage.setStringProperty("folder.propertyField1", "propertyValue1");
        mqMessage.setStringProperty("folder.propertyField2", "propertyValue2");

        // set data
        String output = "Written to requestQ by MQ v7.1 client (" + new Date().toString() + ")";

        mqMessage.write(output.getBytes());
        return mqMessage;
    }

    private MQMessage createMQRFH1MqMessage() throws IOException, MQException {
        MQMessage mqMessage = new MQMessage();
        mqMessage.format = MQFMT_RF_HEADER_1;
        MQRFH rfh = new MQRFH();
        rfh.addNameValuePair("rfhField1", "rhfValue1");

        MQHeaderList headerList = new MQHeaderList();
        headerList.add(rfh);
        headerList.write(mqMessage);

        mqMessage.setStringProperty("folder.propertyField1", "propertyValue1");

        // set data
        String output = "Written to requestQ by MQ v7.1 client (" + new Date().toString() + ")";

        mqMessage.write(output.getBytes());
        return mqMessage;
    }

    private MQMessage createSimpleMessage() throws IOException, MQException {
        MQMessage mqMessage = new MQMessage();

        // set data
        String output = "Written to requestQ by MQ v7.1 client (" + new Date().toString() + ")";

        mqMessage.write(output.getBytes());
        return mqMessage;
    }

    /**
     * Test concurrently sending 20 messages (default # of connections configured) asynchronously.
     *
     * In the main thread (while the other threads are processing due to a sleep call),
     * assert that the GenericObjectPool.mayCreate returns false - since the pool is currently in use.
     *
     * @throws Exception
     */
    @Test
    public void testConnectionPoolingWithMaxConnectionsAndSimpleMessages() throws Exception {
        // setup a stubbed version of task callback
        final AtomicReference<MqNativeEndpointConfig.MqNativeEndpointKey> key = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(MqNativeConstants.DEFAULT_MQ_NATIVE_CONNECTION_POOL_MAX_ACTIVE);
        final MqNativeResourceManager.MqTaskCallback mqrc = new MqNativeResourceManager.MqTaskCallback() {
            @Override
            public void doWork(MQQueueManager queueManager) throws MQException {
                // this callback just sleeps and "pretends" to do some work
                try {
                    latch.countDown();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        doAnswer(invocation -> {
            final MqNativeEndpointConfig mqCfg = (MqNativeEndpointConfig) invocation.getArguments()[0];
            key.set(mqCfg.getMqEndpointKey());

            MQPoolToken token = MQEnvironment.addConnectionPoolToken();
            MqNativeCachedConnectionPool.CachedConnection borrowedConnection = null;
            MqNativeCachedConnectionPool cachedConnectionPool = null;
            try {
                cachedConnectionPool = resourceManagerSpy.getConnectionHolder().get(key.get());
                if (cachedConnectionPool == null || !cachedConnectionPool.ref()) {
                    cachedConnectionPool = null;

                    synchronized (key.toString().intern()) { // prevent concurrent creation for a key
                        cachedConnectionPool = resourceManagerSpy.getConnectionHolder().get(key.get()); // see if someone else created it
                        if (cachedConnectionPool == null || !cachedConnectionPool.ref()) {
                            cachedConnectionPool = null;
                            cachedConnectionPool = resourceManagerSpy.newConnectionPool(mqCfg);
                        }
                    }
                }

                borrowedConnection = cachedConnectionPool.getCachedConnections().borrowObject();
                mqrc.doWork(borrowedConnection.getQueueManager());
            } catch (MQException e) {
                throw e;
            } catch (Exception e) {
                throw new MqNativeRuntimeException(e);
            } finally {
                MQEnvironment.removeConnectionPoolToken(token);
                if (borrowedConnection != null) {
                    try {
                        cachedConnectionPool.getCachedConnections().returnObject(borrowedConnection);
                    } catch (Exception e) {
                        //swallow
                    }
                }
                if (cachedConnectionPool != null) cachedConnectionPool.unRef();
            }
            return null;
        }).when(resourceManagerSpy).doWithMqResources(any(MqNativeEndpointConfig.class), any(MqNativeResourceManager.MqTaskCallback.class));

        MqNativeMessagePropertyRuleSet ruleSet = new MqNativeMessagePropertyRuleSet();
        ruleSet.setPassThroughHeaders(true);
        ruleSet.setPassThroughMqMessageHeaders(true);
        ruleSet.setPassThroughMqMessageProperties(true);
        assertion.setRequestMqNativeMessagePropertyRuleSet(ruleSet);


        Collection<Future> futures = new ArrayList<>();

        final ExecutorService pool = Executors.newFixedThreadPool(MqNativeConstants.DEFAULT_MQ_NATIVE_CONNECTION_POOL_MAX_ACTIVE);
        for (int i = 0; i < MqNativeConstants.DEFAULT_MQ_NATIVE_CONNECTION_POOL_MAX_ACTIVE; i++) {
            futures.add(pool.submit(() -> {
                try {
                    MQMessage mqMessage = createSimpleMessage();
                    context = makeContext(mqMessage);
                    AssertionStatus status = fixture.checkRequest(context);

                    return status != null && status == AssertionStatus.NONE;
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return Boolean.FALSE;
            }));
        }
        latch.await();
        final MqNativeCachedConnectionPool mqNativeCachedConnectionPool = resourceManagerSpy.getConnectionHolder().get(key.get());
        assertEquals(mqNativeCachedConnectionPool.getCachedConnections().getNumIdle(),0);

    }

    @Test
    public void testPutDirectionOpenOptionsDefault() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<Integer> openOptionsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mqQueueManager, times(1)).accessQueue(eq(REQUEST_QUEUE), openOptionsCaptor.capture());
        assertEquals(QUEUE_OPEN_OPTIONS_OUTBOUND_PUT, openOptionsCaptor.getValue().intValue());
    }

    @Test
    public void testPutDirectionOpenOptionsClusterWideProperty() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();
        ServerConfigStub config = (ServerConfigStub) applicationContext.getBean("config");
        config.putProperty(MQ_ROUTING_PUT_OPEN_OPTIONS_PROPERTY, "7654321");

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<Integer> openOptionsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mqQueueManager, times(1)).accessQueue(eq(REQUEST_QUEUE), openOptionsCaptor.capture());
        assertEquals(7654321, openOptionsCaptor.getValue().intValue());
    }

    @Test
    public void testPutDirectionOpenOptionsOverwritten() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        context.setVariable("customizedOptions", 1234567);
        assertion.setOpenOptionsUsed(true);
        assertion.setOpenOptions("${customizedOptions}");
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<Integer> openOptionsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mqQueueManager, times(1)).accessQueue(eq(REQUEST_QUEUE), openOptionsCaptor.capture());
        assertEquals(1234567, openOptionsCaptor.getValue().intValue());
    }

    @Test
    public void testGetDirectionOpenOptionsDefault() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        assertion.setPutToQueue(false);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<Integer> openOptionsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mqQueueManager, times(1)).accessQueue(eq(REQUEST_QUEUE), openOptionsCaptor.capture());
        assertEquals(QUEUE_OPEN_OPTIONS_OUTBOUND_GET, openOptionsCaptor.getValue().intValue());
    }

    @Test
    public void testGetDirectionOpenOptionsClusterWideProperty() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();
        ServerConfigStub config = (ServerConfigStub) applicationContext.getBean("config");
        config.putProperty(MQ_ROUTING_GET_OPEN_OPTIONS_PROPERTY, "7654321");

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        assertion.setPutToQueue(false);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<Integer> openOptionsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mqQueueManager, times(1)).accessQueue(eq(REQUEST_QUEUE), openOptionsCaptor.capture());
        assertEquals(7654321, openOptionsCaptor.getValue().intValue());
    }

    @Test
    public void testGetDirectionOpenOptionsOverwritten() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();;

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        context.setVariable("customizedOptions", 1234567);
        assertion.setPutToQueue(false);
        assertion.setOpenOptionsUsed(true);
        assertion.setOpenOptions("${customizedOptions}");
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<Integer> openOptionsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mqQueueManager, times(1)).accessQueue(eq(REQUEST_QUEUE), openOptionsCaptor.capture());
        assertEquals(1234567, openOptionsCaptor.getValue().intValue());
    }

    @Test
    public void testPutMessageOptionsDefault() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(requestQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(MQPMO_NO_SYNCPOINT, pmoCaptor.getValue().options);
    }

    @Test
    public void testPutMessageOptionsClusterWideProperty() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();
        ServerConfigStub config = (ServerConfigStub) applicationContext.getBean("config");
        config.putProperty(MQ_ROUTING_PUT_MESSAGE_OPTIONS_PROPERTY, "7654321");

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(requestQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(7654321, pmoCaptor.getValue().options);
    }

    @Test
    public void testPutMessageOptionsOverwritten() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        context.setVariable("customizedOptions", 1234567);
        assertion.setMessageOptionsUsed(true);
        assertion.setMessageOptions("${customizedOptions}");
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(requestQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(1234567, pmoCaptor.getValue().options);
    }

    @Test
    public void testGetMessageOptionsDefault() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        assertion.setPutToQueue(false);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQGetMessageOptions> gmoCaptor = ArgumentCaptor.forClass(MQGetMessageOptions.class);
        verify(requestQueue, times(1)).get(any(MQMessage.class), gmoCaptor.capture());
        assertEquals(MQGMO_WAIT | MQGMO_NO_SYNCPOINT | MQGMO_CONVERT, gmoCaptor.getValue().options);
    }

    @Test
    public void testGetMessageOptionsClusterWideProperty() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();
        ServerConfigStub config = (ServerConfigStub) applicationContext.getBean("config");
        config.putProperty(MQ_ROUTING_GET_MESSAGE_OPTIONS_PROPERTY, "7654321");

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        assertion.setPutToQueue(false);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQGetMessageOptions> gmoCaptor = ArgumentCaptor.forClass(MQGetMessageOptions.class);
        verify(requestQueue, times(1)).get(any(MQMessage.class), gmoCaptor.capture());
        assertEquals(7654321, gmoCaptor.getValue().options);
    }

    @Test
    public void testGetMessageOptionsOverwritten() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        context.setVariable("customizedOptions", 1234567);
        assertion.setPutToQueue(false);
        assertion.setMessageOptionsUsed(true);
        assertion.setMessageOptions("${customizedOptions}");
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQGetMessageOptions> gmoCaptor = ArgumentCaptor.forClass(MQGetMessageOptions.class);
        verify(requestQueue, times(1)).get(any(MQMessage.class), gmoCaptor.capture());
        assertEquals(1234567 | MQGMO_NO_SYNCPOINT, gmoCaptor.getValue().options);
    }

    @Test
    public void testReplyQueueGetMessageOptionsDefault() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQGetMessageOptions> gmoCaptor = ArgumentCaptor.forClass(MQGetMessageOptions.class);
        verify(replyQueue, times(1)).get(any(MQMessage.class), gmoCaptor.capture());
        assertEquals(MQGMO_WAIT | MQGMO_NO_SYNCPOINT | MQGMO_CONVERT, gmoCaptor.getValue().options);
    }

    @Test
    public void testReplyQueueGetMessageOptionsClusterWideProperty() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();
        ServerConfigStub config = (ServerConfigStub) applicationContext.getBean("config");
        config.putProperty(MQ_LISTENER_OUTBOUND_REPLY_QUEUE_GET_MESSAGE_OPTIONS_PROPERTY, "7654321");

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQGetMessageOptions> gmoCaptor = ArgumentCaptor.forClass(MQGetMessageOptions.class);
        verify(replyQueue, times(1)).get(any(MQMessage.class), gmoCaptor.capture());
        assertEquals(7654321, gmoCaptor.getValue().options);
    }

    @Test
    public void testReplyQueueGetMessageOptionsOverwritten() throws Exception {
        resourceManagerSpy.getConnectionHolder().clear();

        SsgActiveConnectorManager connectorManager = (SsgActiveConnectorManager) applicationContext.getBean("ssgActiveConnectorManager");
        SsgActiveConnector connector = connectorManager.findByPrimaryKey(assertion.getSsgActiveConnectorId());
        //noinspection ConstantConditions
        connector.setProperty(MqNativeEndpointConfig.PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_REPLY_QUEUE_GET_MESSAGE_OPTIONS_USED, Boolean.toString(true));
        connector.setProperty(MqNativeEndpointConfig.PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_REPLY_QUEUE_GET_MESSAGE_OPTIONS, "1234567");

        MQMessage mqMessage = createSimpleMessage();
        context = makeContext(mqMessage);
        AssertionStatus status = fixture.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        ArgumentCaptor<MQGetMessageOptions> gmoCaptor = ArgumentCaptor.forClass(MQGetMessageOptions.class);
        verify(replyQueue, times(1)).get(any(MQMessage.class), gmoCaptor.capture());
        assertEquals(1234567, gmoCaptor.getValue().options);
    }
}
