package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQC;
import com.ibm.mq.MQException;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueueManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Hashtable;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.MQ_CONNECT_ERROR_SLEEP_PROPERTY;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.MQ_LISTENER_POLLING_INTERVAL_PROPERTY;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;
import static com.l7tech.util.Option.some;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the MqNativeListener component without dependency on a live MQ server.
 */
@RunWith(MockitoJUnitRunner.class)
public class MqNativeListenerTest extends AbstractJUnit4SpringContextTests {
    private static final String queueManagerName = "queueManagerName";
    private static final String targetQueueName = "targetQueueName";
    private static final String replyQueueName = "replyQueueName";
    private static final MqNativeReplyType replyType = MqNativeReplyType.REPLY_SPECIFIED_QUEUE;
    private static final String hostName = "hostName";
    private static final int port = 4444;
    private static final String channel = "channel";
    private static final boolean isQueueCredentialRequired = true;
    private static final String userId = "userId";
    private static final long securePasswordOid = 4444;
    private static final String encryptedPassword = "?????????????????";
    private static final char[] password = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

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

    @Before
    public void setup() throws IOException {
        serverConfig = ApplicationContexts.getTestApplicationContext().getBean("serverConfig", ServerConfig.class);
    }

    /**
     * Exercise code that builds MqNativeClient (MqNativeListener.buildMqNativeClient()) without dependency on live MQ server.
     * @throws MqNativeConfigException configuration error
     * @throws MqNativeException runtime error
     * @throws FindException secure password find error
     * @throws ParseException secure password decrypt error
     */
    @Test
    public void buildMqNativeClient() throws MqNativeConfigException, MqNativeException, FindException, ParseException {
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME)).thenReturn(queueManagerName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME)).thenReturn(targetQueueName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME)).thenReturn(replyQueueName);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE)).thenReturn(replyType.toString());
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_HOST_NAME)).thenReturn(hostName);
        when(ssgActiveConnector.getIntegerProperty(PROPERTIES_KEY_MQ_NATIVE_PORT, -1)).thenReturn(port);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_CHANNEL)).thenReturn(channel);
        when(ssgActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_IS_QUEUE_CREDENTIAL_REQUIRED)).thenReturn(isQueueCredentialRequired);
        when(ssgActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_USERID)).thenReturn(userId);
        when(ssgActiveConnector.getLongProperty(PROPERTIES_KEY_MQ_NATIVE_SECURE_PASSWORD_OID, -1L)).thenReturn(securePasswordOid);

        when(securePassword.getEncodedPassword()).thenReturn(encryptedPassword);

        when(securePasswordManager.findByPrimaryKey(securePasswordOid)).thenReturn(securePassword);
        when(securePasswordManager.decryptPassword(encryptedPassword)).thenReturn(password);

        serverConfig.putProperty(MQ_CONNECT_ERROR_SLEEP_PROPERTY, "10s");
        serverConfig.putProperty(MQ_LISTENER_POLLING_INTERVAL_PROPERTY, "5s");

        MqNativeListener newListener = new MqNativeListener(ssgActiveConnector, 0, applicationContext, securePasswordManager, serverConfig) {

            /**
             * Use this method (handleMessage) to gain access to mqNativeClient inside the class
             */
            @Override
            void handleMessage(final MQMessage queueMessage) throws MqNativeException {
                try {
                    // mock ClientBag so call to MqNativeClient.checkConnect(...) completes without creating MQ objects that depend on a live MQ server
                    MqNativeClient.ClientBag mockClientBag = mock(MqNativeClient.ClientBag.class);
                    when(mockClientBag.getQueueManager()).thenReturn(mock(MQQueueManager.class));
                    when(mockClientBag.getQueueManager().isConnected()).thenReturn(true);
                    when(mockClientBag.getQueueManager().isOpen()).thenReturn(true);
                    Option<MqNativeClient.ClientBag> clientBag = some(mockClientBag);
                    mqNativeClient.setClientBag(clientBag);

                    mqNativeClient.doWork(new Functions.UnaryThrows<MQMessage, MqNativeClient.ClientBag, MQException>() {
                        @Override
                        public MQMessage call(final MqNativeClient.ClientBag bag) throws MQException {

                            // verify configuration parameters
                            assertEquals(queueManagerName, mqNativeClient.queueManagerName);
                            assertEquals(targetQueueName, mqNativeClient.queueName);
                            assertEquals(replyQueueName, mqNativeClient.replyQueueName.some());
                            try {
                                Hashtable queueManagerProperties = mqNativeClient.queueManagerProperties.call();
                                assertEquals(hostName, queueManagerProperties.get(MQC.HOST_NAME_PROPERTY));
                                assertEquals(port, queueManagerProperties.get(MQC.PORT_PROPERTY));
                                assertEquals(channel, queueManagerProperties.get(MQC.CHANNEL_PROPERTY));
                                assertEquals(userId, queueManagerProperties.get(MQC.USER_ID_PROPERTY));
                                assertEquals(new String(password), queueManagerProperties.get(MQC.PASSWORD_PROPERTY));
                            } catch (MqNativeConfigException e) {
                                System.err.println(Arrays.toString(e.getStackTrace()));
                                fail(e.getMessage());
                            }

                            return null;
                        }
                    });
                } catch (MQException e) {
                    System.err.println(Arrays.toString(e.getStackTrace()));
                    fail(e.getMessage());
                } catch (MqNativeConfigException e) {
                    System.err.println(Arrays.toString(e.getStackTrace()));
                    fail(e.getMessage());
                }
            }

            @Override
            final void auditError( final String message, @Nullable final Throwable exception ) {
                // do nothing
            }
        };

        newListener.handleMessage(new MQMessage());
    }
}
