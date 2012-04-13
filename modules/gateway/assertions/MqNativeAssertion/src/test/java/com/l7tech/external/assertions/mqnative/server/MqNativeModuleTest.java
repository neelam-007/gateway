package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.mqnative.MqNativeAcknowledgementType;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.*;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;

import static com.l7tech.external.assertions.mqnative.MqNativeReplyType.REPLY_AUTOMATIC;
import static com.l7tech.external.assertions.mqnative.server.MqNativeModule.DEFAULT_MESSAGE_MAX_BYTES;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_INBOUND_ACKNOWLEDGEMENT_TYPE;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test the MqNativeModule component without dependency on a live MQ server.
 */
@RunWith(MockitoJUnitRunner.class)
public class MqNativeModuleTest extends AbstractJUnit4SpringContextTests {

    @Mock
    private ApplicationContext applicationContext;
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
            LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {

        handleMessageInitialize();
        whenProcessMessageThenAnswer(AssertionStatus.NONE);
        mqNativeModule.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);
    }

    @Test
    public void handleMessageRequestSizeTooLarge() throws MqNativeException, IOException, MessageProcessingSuspendedException,
            LicenseException, PolicyVersionException, MethodNotAllowedException, PolicyAssertionException {

        handleMessageInitialize();

        // set maxMessageBytes to less than our test mqMessage
        final Pair<byte[], byte[]> parsedRequest = MqNativeUtils.parseHeader(mqMessage);
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
                    final Pair<byte[], byte[]> parsedRequest = MqNativeUtils.parseHeader(responseMessage);
                    assertEquals(new String(parsedRequest.right), AssertionStatus.FAILED.getMessage());
                } catch (IOException e) {
                    fail(e.getMessage());
                }
                return null;
            }}).when(mqNativeModuleSpy).sendResponse(any(MQMessage.class), any(MQMessage.class), any(SsgActiveConnector.class), any(MqNativeClient.class));

        mqNativeModuleSpy.handleMessageForConnector(ssgActiveConnector, mqNativeClient, mqMessage);
    }

    private MQMessage createMqMessage() throws IOException {
        final MQMessage requestMessage = new MQMessage();
        final String pubCommand = "<psc><Command>Publish</Command><Topic>Stock</Topic>" +
                "<QMgrName>QFLEXT1</QMgrName><QName>QFLEXT1.A</QName></psc>";
        final int folderLength = pubCommand.length();
        requestMessage.format = MQC.MQFMT_RF_HEADER_2; // Msg Format
        requestMessage.writeString(MQC.MQRFH_STRUC_ID); // StrucId
        requestMessage.writeInt4(MQC.MQRFH_VERSION_2); // Version
        requestMessage.writeInt4(MQC.MQRFH_STRUC_LENGTH_FIXED_2 + folderLength + 4); //4) + rf); // StrucLength
        requestMessage.writeInt4(MQC.MQENC_NATIVE); // Encoding
        requestMessage.writeInt4(MQC.MQCCSI_DEFAULT); // CodedCharacterSetId
        requestMessage.writeString(MQC.MQFMT_NONE); // Format (content)
        requestMessage.writeInt4(MQC.MQRFH_NO_FLAGS); // Flags
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

        when(messageProcessor.processMessage(any(PolicyEnforcementContext.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                context.getResponse().initialize(ContentTypeHeader.XML_DEFAULT, assertionStatus.getMessage().getBytes());
                return assertionStatus;
            }
        });
    }
}
