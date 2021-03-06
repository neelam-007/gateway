package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.ibm.mq.headers.MQDataException;
import com.ibm.mq.headers.MQHeaderList;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.external.assertions.mqnative.MqNativeRoutingAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.message.HasHeaders;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.*;
import com.l7tech.server.audit.AuditContextFactoryStub;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.policy.assertion.ServerAddHeaderAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.policy.variable.MessageSelector;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.transport.ListenerException;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.server.MqNativeClient.ClientBag;
import static com.l7tech.external.assertions.mqnative.server.MqNativeClient.MqNativeConnectionListener;
import static com.l7tech.external.assertions.mqnative.server.MqNativeModule.DEFAULT_MESSAGE_MAX_BYTES;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.util.Functions.NullaryThrows;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test the MqNativeModule component without dependency on a live MQ server.
 */
@RunWith(MockitoJUnitRunner.class)
public class MqNativeModuleTest extends AbstractJUnit4SpringContextTests {
    private static final String queueManagerName = "queueManagerName";
    private static final String targetQueueName = "targetQueueName";
    private static final String replyQueueName = "replyQueueName";
    private static final MqNativeReplyType replyType = MqNativeReplyType.REPLY_SPECIFIED_QUEUE;
    private static final String hostName = "hostName";
    private static final int port = 4444;
    private static final String channel = "channel";
    private static final boolean isQueueCredentialRequired = true;
    private static final String userId = "userId";
    private static final Goid securePasswordGoid = new Goid(0,4444);
    private static final String encryptedPassword = "?????????????????";
    private static final char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

    private static final Logger logger = Logger.getLogger(MqNativeModuleTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private MessageProcessor messageProcessor;
    @Mock
    private MqNativeClient mqNativeClient;
    @Mock
    private SecurePassword securePassword;
    @Mock
    private SecurePasswordManager securePasswordManager;
    @Mock
    private SsgActiveConnector ssgActiveConnector;
    @Mock
    private StashManager stashManager;
    @Mock
    private StashManagerFactory stashManagerFactory;
    @Mock
    private ThreadPoolBean threadPoolBean;

    private ServerConfig serverConfig;
    private MQMessage mqMessage;
    private MqNativeModule mqNativeModule;

    @Before
    public void setup() throws IOException {
        serverConfig = ApplicationContexts.getTestApplicationContext().getBean("serverConfig", ServerConfig.class);
        mqMessage = createMqMessage();
        mqNativeModule = createMqNativeModule();
        MessageSelector.registerSelector(MqNativeRoutingAssertion.MQ,
                new MqNativeModuleLoadListener.MqNativeHeaderSelector(MqNativeRoutingAssertion.MQ + ".", true,
                Arrays.<Class<? extends HasHeaders>>asList(MqNativeKnob.class)));

    }

    /**
     * Exercise code that processes a message (MqNativeModule.handleMessageForConnector(...)) without dependency on live MQ server.
     *
     * @throws com.l7tech.external.assertions.mqnative.server.MqNativeException possible exception that could fail this test
     * @throws java.io.IOException possible exception that could fail this test
     * @throws com.l7tech.server.MessageProcessingSuspendedException possible exception that could fail this test
     * @throws com.l7tech.gateway.common.LicenseException possible exception that could fail this test
     * @throws com.l7tech.server.policy.PolicyVersionException possible exception that could fail this test
     * @throws com.l7tech.server.MethodNotAllowedException possible exception that could fail this test
     * @throws com.l7tech.policy.assertion.PolicyAssertionException possible exception that could fail this test
     */
    @Test
    public void handleMessage() throws MqNativeException, IOException, MessageProcessingSuspendedException,
            LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException, MqNativeConfigException {

        handleMessageInitialize();
        whenProcessMessageThenAnswer(AssertionStatus.NONE);
        mqNativeModule.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);
    }

    @Test
    public void handleMessageRequestSizeTooLarge() throws MqNativeException, IOException, MessageProcessingSuspendedException,
            LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException, MQDataException, MqNativeConfigException {

        handleMessageInitialize();

        // set maxMessageBytes to less than our test mqMessage
        final Pair<byte[], byte[]> parsedRequest = MqNativeUtils.parseHeaderPayload(mqMessage);
        final Long maxMessageBytes = (long) parsedRequest.right.length - 1;
        serverConfig.putProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES, maxMessageBytes.toString());
        when(ssgActiveConnector.getLongProperty(SsgActiveConnector.PROPERTIES_KEY_REQUEST_SIZE_LIMIT, maxMessageBytes )).thenReturn(maxMessageBytes);

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                final ApplicationEvent event = (ApplicationEvent) invocation.getArguments()[0];
                assertTrue(event instanceof FaultProcessed);
                FaultProcessed faultProcessed = (FaultProcessed) event;
                assertThat(faultProcessed.getFaultMessage(), containsString("<faultstring>Message too large</faultstring>"));
                return null;
            }}).when(applicationEventPublisher).publishEvent(any(ApplicationEvent.class));

        mqNativeModule.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);

        verify(applicationEventPublisher).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    public void handleMessageWithMessageProcessorError() throws MQException, MqNativeConfigException,  MqNativeException, IOException,
            MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {

        handleMessageInitialize();
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_SPECIFIED_QUEUE);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.AUTOMATIC);
        whenProcessMessageThenAnswer(AssertionStatus.FAILED);

        MqNativeModule mqNativeModuleSpy = spy(mqNativeModule);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                final MQMessage responseMessage = (MQMessage) invocation.getArguments()[1];
                try {
                    final Pair<byte[], byte[]> parsedRequest = MqNativeUtils.parseHeaderPayload(responseMessage);
                    assertEquals(new String(parsedRequest.right), AssertionStatus.FAILED.getMessage());
                } catch (IOException | MQDataException e) {
                    fail(e.getMessage());
                }
                return null;
            }}).when(mqNativeModuleSpy).sendResponse(any(MQMessage.class), any(MQMessage.class), any(SsgActiveConnector.class), any(MqNativeClient.class));

        mqNativeModuleSpy.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);
    }

    @Test
    public void addSingleConnector() throws ListenerException, MqNativeConfigException, FindException, ParseException {
        mqNativeModule.setApplicationContext(ApplicationContexts.getTestApplicationContext());

        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME)).thenReturn(targetQueueName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME)).thenReturn(replyQueueName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE)).thenReturn(replyType.toString());
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME)).thenReturn(hostName);
        when(ssgActiveConnector.getIntegerProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, -1)).thenReturn(port);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL)).thenReturn(channel);
        when(ssgActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED)).thenReturn(isQueueCredentialRequired);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_USERID)).thenReturn(userId);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID, "-1L")).thenReturn(securePasswordGoid.toHexString());

        when(securePassword.getEncodedPassword()).thenReturn(encryptedPassword);

        when(securePasswordManager.findByPrimaryKey(securePasswordGoid)).thenReturn(securePassword);
        when(securePasswordManager.decryptPassword(encryptedPassword)).thenReturn(password);

        serverConfig.putProperty(MQ_CONNECT_ERROR_SLEEP_PROPERTY, "10s");
        serverConfig.putProperty(MQ_LISTENER_POLLING_INTERVAL_PROPERTY, "5s");

        when(ssgActiveConnector.getName()).thenReturn("Test SSG Active Connector");
        when(ssgActiveConnector.getGoid()).thenReturn(new Goid(0,999999999L));

        // test one listener create
        int numberOfListenersToCreate = 1;
        when(ssgActiveConnector.getIntegerProperty(PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE, 1)).thenReturn(numberOfListenersToCreate);
        mqNativeModule.addConnector(ssgActiveConnector);
        Set<MqNativeListener> activeListenerSet = mqNativeModule.getActiveListeners().get(ssgActiveConnector.getGoid());
        assertEquals(numberOfListenersToCreate, activeListenerSet.size());
    }

    @Test
    public void addMultipleConnectorsConcurrently() throws ListenerException, MqNativeConfigException, FindException, ParseException {
        mqNativeModule.setApplicationContext(ApplicationContexts.getTestApplicationContext());

        SsgActiveConnector activeConnector = new SsgActiveConnector();

        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME, queueManagerName);
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME, targetQueueName);
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME, replyQueueName);
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, replyType.toString());
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME, hostName);
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, Integer.toString(port));
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL, channel);
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED, Boolean.toString(isQueueCredentialRequired));
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_USERID, userId);
        activeConnector.setProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID, securePasswordGoid.toHexString());

        when(securePassword.getEncodedPassword()).thenReturn(encryptedPassword);

        when(securePasswordManager.findByPrimaryKey(securePasswordGoid)).thenReturn(securePassword);
        when(securePasswordManager.decryptPassword(encryptedPassword)).thenReturn(password);

        serverConfig.putProperty(MQ_CONNECT_ERROR_SLEEP_PROPERTY, "10s");
        serverConfig.putProperty(MQ_LISTENER_POLLING_INTERVAL_PROPERTY, "5s");

        activeConnector.setName("Test SSG Active Connector");
        activeConnector.setGoid(new Goid(0,999999999L));

        // test multiple concurrent listener create
        int numberOfListenersToCreate = 20;
        activeConnector.setProperty(PROPERTIES_KEY_NUMBER_OF_SAC_TO_CREATE, Integer.toString(numberOfListenersToCreate));

        mqNativeModule.addConnector(activeConnector);

        Set<MqNativeListener> activeListenerSet = mqNativeModule.getActiveListeners().get(activeConnector.getGoid());
        assertEquals(numberOfListenersToCreate, activeListenerSet.size());
    }

    private MQMessage createMqMessage() throws IOException {
        final MQMessage requestMessage = new MQMessage();
        final String pubCommand = "<psc><Command>Publish</Command><Topic>Stock</Topic>" +
                "<QMgrName>QFLEXT1</QMgrName><QName>QFLEXT1.A</QName></psc>";
        final int folderLength = pubCommand.length();
        requestMessage.format = MQFMT_RF_HEADER_2; // Msg Format
        requestMessage.writeString(MQRFH_STRUC_ID); // StrucId
        requestMessage.writeInt4(MQRFH_VERSION_2); // Version
        requestMessage.writeInt4(MQRFH_STRUC_LENGTH_FIXED_2 + folderLength + 4); //4) + rf); // StrucLength
        requestMessage.writeInt4(MQENC_NATIVE); // Encoding
        requestMessage.writeInt4(MQCCSI_DEFAULT); // CodedCharacterSetId
        requestMessage.writeString(MQFMT_NONE); // Format (content)
        requestMessage.writeInt4(MQRFH_NO_FLAGS); // Flags
        requestMessage.writeInt4(1208); // NameValueCCSID = UTF-8
        requestMessage.writeInt4(folderLength);
        requestMessage.writeString(pubCommand);
        requestMessage.writeInt4(folderLength);
        requestMessage.writeString(pubCommand);
        requestMessage.writeInt4(folderLength);
        requestMessage.writeString(pubCommand);
        requestMessage.writeString("begin payload");
        return requestMessage;
    }

    private MqNativeModule createMqNativeModule() {
        MqNativeModule mqNativeModule = new MqNativeModule(threadPoolBean);
        mqNativeModule.setMessageProcessor(messageProcessor);
        mqNativeModule.setServerConfig(serverConfig);
        mqNativeModule.setStashManagerFactory(stashManagerFactory);
        mqNativeModule.setMessageProcessingEventChannel(applicationEventPublisher);
        mqNativeModule.setSecurePasswordManager(securePasswordManager);
        mqNativeModule.setAuditContextFactory(new AuditContextFactoryStub(ConfigFactory.getCachedConfig(), "testnode"));
        mqNativeModule.setMessageSummaryAuditFactory(new MessageSummaryAuditFactory("testnode"));
        return mqNativeModule;
    }

    private void handleMessageInitialize() throws IOException, MessageProcessingSuspendedException,
            LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {

        final Long maxMessageBytes = (long) DEFAULT_MESSAGE_MAX_BYTES;
        when(ssgActiveConnector.getLongProperty(SsgActiveConnector.PROPERTIES_KEY_REQUEST_SIZE_LIMIT, maxMessageBytes)).thenReturn(maxMessageBytes);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_NONE);
        when(stashManagerFactory.createStashManager()).thenReturn(stashManager);
    }

    private void whenProcessMessageThenAnswer(final AssertionStatus assertionStatus) throws IOException, MessageProcessingSuspendedException, LicenseException,
            PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {

        when(messageProcessor.processMessageNoAudit(any(PolicyEnforcementContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, assertionStatus.getMessage().getBytes());
                return assertionStatus;
            }
        });
    }

    @Test
    public void testCustomizeResponse() throws MessageProcessingSuspendedException, LicenseException, IOException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException, MqNativeException, MqNativeConfigException {
        handleMessageInitialize();
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_SPECIFIED_QUEUE);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.AUTOMATIC);

        MqNativeModule mqNativeModuleSpy = spy(mqNativeModule);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                final MQMessage responseMessage = (MQMessage) invocation.getArguments()[1];
                try {
                    MqMessageProxy proxy = new MqMessageProxy(responseMessage);
                    assertEquals("3000", String.valueOf(proxy.getMessageDescriptor("expiry")));
                    assertEquals("propertyValue", proxy.getMessageProperty("folder.propertyName"));
                    assertEquals("headerValue", proxy.getPrimaryHeaderValue("folder.headerName"));
                } catch (Exception e) {
                    fail("Invalid MQMessage.");
                }

                return null;
            }}).when(mqNativeModuleSpy).sendResponse(any(MQMessage.class), any(MQMessage.class), any(SsgActiveConnector.class), any(MqNativeClient.class));

        when(messageProcessor.processMessageNoAudit(any(PolicyEnforcementContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                addHeader(context, TargetMessageType.RESPONSE, "mqnative.md.expiry", "3000");
                addHeader(context, TargetMessageType.RESPONSE, "mqnative.md.format", "MQRFH2");//output as format MQRFH2 format
                addHeader(context, TargetMessageType.RESPONSE, "mqnative.property.folder.propertyName", "propertyValue");
                addHeader(context, TargetMessageType.RESPONSE, "mqnative.additionalheader.folder.headerName", "headerValue");
                return AssertionStatus.NONE;
            }
        });

        mqNativeModuleSpy.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);

    }

    /**
     * Ensure that the replyToQueueManagerName IS specified when accessing the reply queue when the
     * io.mqRoutingIncludeReplyToQueueManagerName cluster property is set to true (enabled)
     */
    @Test
    public void testSendResponse_ReplyToQueueManagerNameSpecified_NameUsedToAccessQueue() throws Exception {
        handleMessageInitialize();

        String replyQueue = "replyQueue";
        String queueManagerName = "exampleReplyQueueManager1";

        mqMessage.replyToQueueName = replyQueue;
        mqMessage.replyToQueueManagerName = queueManagerName;

        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_AUTOMATIC);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.AUTOMATIC);

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);

        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(anyString(), anyInt(), anyString(), anyString(), anyString())).thenReturn(mock(MQQueue.class));

        ClientBag clientBag = mock(ClientBag.class);

        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        mqNativeModule.sendResponse(mqMessage, new MQMessage(), ssgActiveConnector, mqNativeClient);

        verify(queueManager, times(1)).accessQueue(eq(replyQueue), anyInt(), eq(queueManagerName), anyString(), anyString());
        verify(queueManager, never()).accessQueue(anyString(), anyInt());
    }

    /**
     * Ensure that the replyToQueueManagerName is NOT specified when accessing the reply queue when the
     * io.mqRoutingIncludeReplyToQueueManagerName cluster property is set to false (disabled)
     */
    @Test
    public void testSendResponse_ReplyToQueueManagerNameSpecifiedButIncludeDisabled_NameNotUsedToAccessQueue() throws Exception {
        handleMessageInitialize();

        String replyQueue = "replyQueue";
        String queueManagerName = "exampleReplyQueueManager1";

        mqMessage.replyToQueueName = replyQueue;
        mqMessage.replyToQueueManagerName = queueManagerName;

        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_AUTOMATIC);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.AUTOMATIC);

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);

        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(mock(MQQueue.class));

        ClientBag clientBag = mock(ClientBag.class);

        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        serverConfig.putProperty(ServerConfigParams.PARAM_IO_MQ_INCLUDE_REPLY_QUEUE_MANAGER_NAME, "false");

        mqNativeModule.sendResponse(mqMessage, new MQMessage(), ssgActiveConnector, mqNativeClient);

        verify(queueManager, never()).accessQueue(anyString(), anyInt(), anyString(), anyString(), anyString());
        verify(queueManager, times(1)).accessQueue(eq(replyQueue), anyInt());
    }

    @Test
    public void testContextVariables() throws MQException, MqNativeConfigException,  MqNativeException, IOException,
            MessageProcessingSuspendedException, LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {

        handleMessageInitialize();
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_SPECIFIED_QUEUE);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.AUTOMATIC);

        when(messageProcessor.processMessageNoAudit(any(PolicyEnforcementContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final Map<String, Object> vars = context.getVariableMap(new String[]{"request"}, audit);

                if (!ExpandVariables.process("${request.mqNative.md.accountingToken}", vars, audit, false).equals(HexUtils.encodeBase64(mqMessage.accountingToken))) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.md.applicationIdData}", vars, audit, false).equals(mqMessage.applicationIdData)) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.md.backoutCount}", vars, audit, false).equals(Integer.toString(mqMessage.backoutCount))) {
                    return AssertionStatus.FAILED;
                }

                String putDataTime = MqNativeUtils.DATE_FORMAT.format(mqMessage.putDateTime.getTime());
                if (!ExpandVariables.process("${request.mqNative.md.putDateTime}", vars, audit, false).equals(putDataTime)) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.md.version}", vars, audit, false).equals(Integer.toString(mqMessage.getVersion()))) {
                    return AssertionStatus.FAILED;
                }

                if (!ExpandVariables.process("${request.mqNative.md.version}", vars, audit, false).equals(Integer.toString(mqMessage.getVersion()))) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.additionalheadernames}", vars, audit, false).equals("folder.rfh2Field1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.alladditionalheadervalues}", vars, audit, false).equals("rhf2Value1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.propertynames}", vars, audit, false).contains("folder.testObjectProperty")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.propertynames}", vars, audit, false).contains("testLongProperty")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.propertynames}", vars, audit, false).contains("folder.propertyField1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.allpropertyvalues}", vars, audit, false).contains("java.lang.Exception: testObjectValueAsException")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.allpropertyvalues}", vars, audit, false).contains("9223372036854775807")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.allpropertyvalues}", vars, audit, false).contains("propertyValue1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.additionalheadernames.length}", vars, audit, false).equals("1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.alladditionalheadervalues.length}", vars, audit, false).equals("1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.propertynames.length}", vars, audit, false).equals("3")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.allpropertyvalues.length}", vars, audit, false).equals("3")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.additionalheader.folder.rfh2Field1}", vars, audit, false).equals("rhf2Value1")) {
                    return AssertionStatus.FAILED;
                }
                if (!ExpandVariables.process("${request.mqNative.property.folder.propertyField1}", vars, audit, false).equals("propertyValue1")) {
                    return AssertionStatus.FAILED;
                }
                context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, AssertionStatus.NONE.getMessage().getBytes());
                return AssertionStatus.NONE;
            }
        });

        MqNativeModule mqNativeModuleSpy = spy(mqNativeModule);
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                final MQMessage responseMessage = (MQMessage) invocation.getArguments()[1];
                try {
                    final Pair<byte[], byte[]> parsedRequest = MqNativeUtils.parseHeaderPayload(responseMessage);
                    assertEquals(new String(parsedRequest.right), AssertionStatus.NONE.getMessage());
                } catch (IOException | MQDataException e) {
                    fail(e.getMessage());
                }
                return null;
            }}).when(mqNativeModuleSpy).sendResponse(any(MQMessage.class), any(MQMessage.class), any(SsgActiveConnector.class), any(MqNativeClient.class));

        mqMessage = new MQMessage();
        mqMessage.accountingToken = "accountToken".getBytes();
        mqMessage.applicationIdData = "applicationIdData";
        mqMessage.backoutCount = "backoutCount".length();
        mqMessage.putDateTime = (GregorianCalendar) GregorianCalendar.getInstance();
        mqMessage.setVersion(2);
        mqMessage.format = MQFMT_RF_HEADER_2;

        MQRFH2 rfh2= new MQRFH2();
        rfh2.setFieldValue("folder", "rfh2Field1","rhf2Value1");

        MQHeaderList headerList = new MQHeaderList();
        headerList.add(rfh2);
        headerList.write(mqMessage);

        // set no header (message properties)
        mqMessage.setLongProperty("testLongProperty", Long.MAX_VALUE);
        mqMessage.setStringProperty("folder.propertyField1", "propertyValue1");
        mqMessage.setObjectProperty("folder.testObjectProperty", new Exception("testObjectValueAsException"));

        // set data
        String output = "Test Data";
        mqMessage.write(output.getBytes());

        mqNativeModuleSpy.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);
    }

    @Test
    public void testInboundReplyQueuePutMessageOptionsDefault_OnTake() throws Exception {
        handleMessageInitialize();

        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.AUTOMATIC);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_SPECIFIED_QUEUE);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME)).thenReturn(replyQueueName);

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);
        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(mock(MQQueue.class));

        ClientBag clientBag = mock(ClientBag.class);
        MQQueue replyQueue = mock(MQQueue.class);
        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);
        when(clientBag.getSpecifiedReplyQueue()).thenReturn(replyQueue);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        mqNativeModule.sendResponse(mqMessage, new MQMessage(), ssgActiveConnector, mqNativeClient);

        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(replyQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(MQPMO_NEW_MSG_ID | MQPMO_NO_SYNCPOINT, pmoCaptor.getValue().options);
    }

    @Test
    public void testInboundReplyQueuePutMessageOptionsDefault_OnCompletion() throws Exception {
        handleMessageInitialize();

        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE, null, MqNativeAcknowledgementType.class)).thenReturn(MqNativeAcknowledgementType.ON_COMPLETION);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_SPECIFIED_QUEUE);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME)).thenReturn(replyQueueName);

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);
        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(mock(MQQueue.class));

        ClientBag clientBag = mock(ClientBag.class);
        MQQueue replyQueue = mock(MQQueue.class);
        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);
        when(clientBag.getSpecifiedReplyQueue()).thenReturn(replyQueue);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        mqNativeModule.sendResponse(mqMessage, new MQMessage(), ssgActiveConnector, mqNativeClient);

        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(replyQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(MQPMO_NEW_MSG_ID | MQPMO_SYNCPOINT, pmoCaptor.getValue().options);
    }

    @Test
    public void testInboundReplyQueuePutMessageOptionsOverwritten() throws Exception {
        handleMessageInitialize();

        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getEnumProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE, REPLY_AUTOMATIC, MqNativeReplyType.class)).thenReturn(MqNativeReplyType.REPLY_SPECIFIED_QUEUE);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME)).thenReturn(replyQueueName);
        when(ssgActiveConnector.getBooleanProperty(MqNativeModule.PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_REPLY_QUEUE_PUT_MESSAGE_OPTIONS_USED)).thenReturn(true);
        when(ssgActiveConnector.getIntegerProperty(eq(MqNativeModule.PROPERTIES_KEY_MQ_NATIVE_INBOUND_REPLY_QUEUE_PUT_MESSAGE_OPTIONS), anyInt())).thenReturn(7654321);

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);
        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(anyString(), anyInt())).thenReturn(mock(MQQueue.class));

        ClientBag clientBag = mock(ClientBag.class);
        MQQueue replyQueue = mock(MQQueue.class);
        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);
        when(clientBag.getSpecifiedReplyQueue()).thenReturn(replyQueue);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        mqNativeModule.sendResponse(mqMessage, new MQMessage(), ssgActiveConnector, mqNativeClient);

        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(replyQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(7654321, pmoCaptor.getValue().options);
    }

    @Test
    public void testInboundFailureQueuePutMessageOptionsDefault() throws Exception {
        handleMessageInitialize();

        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED)).thenReturn(true);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME)).thenReturn("failureQueueName");

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);
        MQQueue failureQueue = mock(MQQueue.class);
        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(eq("failureQueueName"), anyInt())).thenReturn(failureQueue);

        ClientBag clientBag = mock(ClientBag.class);
        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        boolean result = mqNativeModule.postMessageToFailureQueue(mqMessage, ssgActiveConnector, mqNativeClient);
        assertTrue(result);

        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(failureQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(MQPMO_SYNCPOINT, pmoCaptor.getValue().options);
    }

    @Test
    public void testInboundFailureQueuePutMessageOptionsOverwritten() throws Exception {
        handleMessageInitialize();

        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED)).thenReturn(true);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME)).thenReturn("failureQueueName");
        when(ssgActiveConnector.getBooleanProperty(MqNativeModule.PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_PUT_MESSAGE_OPTIONS_USED)).thenReturn(true);
        when(ssgActiveConnector.getIntegerProperty(eq(MqNativeModule.PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_PUT_MESSAGE_OPTIONS), anyInt())).thenReturn(7654321);

        @SuppressWarnings("unchecked")
        NullaryThrows<Hashtable, MqNativeConfigException>  queueManagerProperties =
                (NullaryThrows<Hashtable, MqNativeConfigException>) mock(NullaryThrows.class);

        MqNativeConnectionListener listener = mock(MqNativeConnectionListener.class);

        MqNativeClient mqNativeClient =
                spy(new MqNativeClient("queueManager", queueManagerProperties, "", Option.<String>none(), listener));

        MQQueueManager queueManager = mock(MQQueueManager.class);
        MQQueue failureQueue = mock(MQQueue.class);
        when(queueManager.isConnected()).thenReturn(true);
        when(queueManager.isOpen()).thenReturn(true);
        when(queueManager.accessQueue(eq("failureQueueName"), anyInt())).thenReturn(failureQueue);

        ClientBag clientBag = mock(ClientBag.class);
        when(clientBag.getQueueManager()).thenReturn(queueManager, queueManager);

        @SuppressWarnings("unchecked")
        Option<ClientBag> clientBagOption = (Option<ClientBag>) mock(Option.class);

        when(clientBagOption.isSome()).thenReturn(true);
        when(clientBagOption.some()).thenReturn(clientBag, clientBag);

        mqNativeClient.setClientBag(clientBagOption);

        boolean result = mqNativeModule.postMessageToFailureQueue(mqMessage, ssgActiveConnector, mqNativeClient);
        assertTrue(result);

        ArgumentCaptor<MQPutMessageOptions> pmoCaptor = ArgumentCaptor.forClass(MQPutMessageOptions.class);
        verify(failureQueue, times(1)).put(any(MQMessage.class), pmoCaptor.capture());
        assertEquals(7654321, pmoCaptor.getValue().options);
    }

    @BugId("DE341493")
    @Test
    public void testGenerateFaultTemplateUsesTemplate() throws IOException, MqNativeException {
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        SoapFaultLevel faultLevel = new SoapFaultLevel();
        final String expectedFaultTemplate = "EXPECTED FAULT TEMPLATE";
        faultLevel.setFaultTemplate(expectedFaultTemplate);
        faultLevel.setLevel(SoapFaultLevel.TEMPLATE_FAULT);
        context.setFaultlevel(faultLevel);

        //Test
        assertEquals(mqNativeModule.generateFaultTemplate("dummy msg","dummy code",context), expectedFaultTemplate);
    }

    @Test
    public void testGenerateFaultTemplateDefaultSoapFault() throws IOException, MqNativeException {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        final String faultMessage = "fault Message";
        final String faultCode = "this is the fault code";

        final String lineSeperator = System.lineSeparator();
        final String expectedFaultTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeperator  +
                "<soapenv:Envelope" + lineSeperator  +
                "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" + lineSeperator  +
                "    xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"" + lineSeperator  +
                "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" + lineSeperator  +
                "    <soapenv:Body>" + lineSeperator  +
                "        <soapenv:Fault>" + lineSeperator  +
                "            <faultcode>"+ faultCode +"</faultcode>" + lineSeperator  +
                "            <faultstring>" + faultMessage + "</faultstring>" + lineSeperator  +
                "            <faultactor/>" + lineSeperator  +
                "            <detail/>" + lineSeperator  +
                "        </soapenv:Fault>" + lineSeperator  +
                "    </soapenv:Body>" + lineSeperator  +
                "</soapenv:Envelope>" + lineSeperator;
        //Test
        assertEquals(mqNativeModule.generateFaultTemplate(faultMessage,faultCode,context), expectedFaultTemplate);
    }

    private void addHeader(PolicyEnforcementContext context, TargetMessageType targetMessageType, String name, String value) throws IOException, PolicyAssertionException {

        AddHeaderAssertion addHeaderAssertion = new AddHeaderAssertion();
        addHeaderAssertion.setTarget(targetMessageType);
        addHeaderAssertion.setHeaderName(name);
        addHeaderAssertion.setHeaderValue(value);
        ServerAddHeaderAssertion serverAddHeaderAssertion = new ServerAddHeaderAssertion(addHeaderAssertion);
        serverAddHeaderAssertion.checkRequest(context);
    }


}
