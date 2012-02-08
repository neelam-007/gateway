package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.l7tech.external.assertions.mqnative.MqNativeAdmin;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.closeQuietly;
import static com.l7tech.gateway.common.transport.SsgActiveConnector.*;

/**
 * Sever-side glue for the MQ Native Admin implementation,
 * which is exposed via a simple admin extension interface.
 */
public class MqNativeAdminServerSupport {
    private static final Logger logger = Logger.getLogger(MqNativeAdminServerSupport.class.getName());

    private static MqNativeAdminServerSupport instance;

    private Config config;
    private SecurePasswordManager securePasswordManager;

    public static synchronized MqNativeAdminServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            MqNativeAdminServerSupport support = new MqNativeAdminServerSupport();
            support.init(context);
            instance = support;
        }
        return instance;
    }

    public void init(ApplicationContext context) {
        config = context.getBean("serverConfig", ServerConfig.class);
        securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<MqNativeAdmin>(MqNativeAdmin.class, null, new MqNativeAdmin() {

            @Override
            public long getDefaultMqMessageMaxBytes() {
                return config.getLongProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES, 2621440L);  // ioMqMessageMaxBytes.default = 2621440
            }

            @Override
            public void testSettings(SsgActiveConnector mqNativeActiveConnector) throws MqNativeTestException {
                MQQueueManager queueManager = null;
                MQQueue targetQueue = null;
                MQQueue replyQueue = null;
                String testName = "";
                try {
                    // test queue manager
                    logger.finer("Attempting to get queue manager ...");
                    testName = "queue manager - ";
                    queueManager = new MQQueueManager(
                            mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME ),
                            MqNativeUtils.buildQueueManagerConnectProperties( mqNativeActiveConnector, securePasswordManager ) );
                    logger.finer("... successfully got queue manager " + queueManager.name + "!");

                    // test inbound
                    if (mqNativeActiveConnector.getBooleanProperty( PROPERTIES_KEY_IS_INBOUND )) {

                        // test target queue
                        logger.finer("Attempting to get inbound queue ...");
                        testName = "inbound queue - ";
                        targetQueue = queueManager.accessQueue(
                                mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME ), QUEUE_OPEN_OPTIONS_INBOUND );
                        targetQueue.close();
                        logger.finer("... successfully got inbound queue " + targetQueue.name + "!");

                        // if applicable test reply queue
                        MqNativeReplyType replyType = MqNativeReplyType.valueOf( mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE) );
                        String specifiedReplyQueueName = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME);
                        if ( MqNativeReplyType.REPLY_SPECIFIED_QUEUE == replyType && !StringUtils.isEmpty( specifiedReplyQueueName ) ) {
                            logger.finer("Attempting to get specified inbound reply queue ...");
                            testName = "inbound specified reply queue - ";
                            replyQueue = queueManager.accessQueue( specifiedReplyQueueName, QUEUE_OPEN_OPTIONS_INBOUND_REPLY_SPECIFIED_QUEUE );
                            replyQueue.close();
                            logger.finer("... successfully got specified inbound reply queue " + replyQueue.name + "!");
                        }

                    // else test outbound when queue is not a template
                    } else if ( !mqNativeActiveConnector.getBooleanProperty( PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_IS_TEMPLATE_QUEUE ) ) {
                        // test target queue
                        logger.finer("Attempting to get outbound put queue ...");
                        testName = "outbound queue - ";
                        targetQueue = queueManager.accessQueue(
                                mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME ), QUEUE_OPEN_OPTIONS_OUTBOUND_PUT );
                        targetQueue.close();
                        logger.finer("... successfully got outbound put queue " + targetQueue.name + "!");

                        logger.finer("Attempting to get outbound get queue ...");
                        targetQueue = queueManager.accessQueue(
                                mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME ), QUEUE_OPEN_OPTIONS_OUTBOUND_GET );
                        targetQueue.close();
                        logger.finer("... successfully got outbound get queue " + targetQueue.name + "!");

                        MqNativeReplyType replyType = MqNativeReplyType.valueOf( mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_REPLY_TYPE) );
                        if ( MqNativeReplyType.REPLY_AUTOMATIC == replyType) {
                            // if applicable test model queue
                            logger.finer("Attempting to get outbound model queue ...");
                            testName = "outbound model queue - ";
                            String modelQueueName = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_OUTBOUND_TEMPORARY_QUEUE_NAME_PATTERN);
                            replyQueue = queueManager.accessQueue(modelQueueName, QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_MODEL_QUEUE);
                            replyQueue.close();
                            logger.finer("... successfully got outbound model queue " + replyQueue.name + "!");
                        } else if ( MqNativeReplyType.REPLY_SPECIFIED_QUEUE == replyType) {
                            // if applicable test specified reply queue
                            logger.finer("Attempting to get outbound specified reply queue ...");
                            testName = "outbound specified reply queue - ";
                            String specifiedReplyQueueName = mqNativeActiveConnector.getProperty(PROPERTIES_KEY_MQ_NATIVE_SPECIFIED_REPLY_QUEUE_NAME);
                            replyQueue = queueManager.accessQueue(specifiedReplyQueueName, QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_SPECIFIED_QUEUE);
                            replyQueue.close();
                            logger.finer("... successfully got outbound specified reply queue " + replyQueue.name + "!");
                        }
                    }
                } catch (Throwable t) {
                    try {
                        // At admin extension load time the MQ jars may not have been installed. Cannot use MQException explicitly in this interface.
                        final Class<?> aClass = Class.forName("com.ibm.mq.MQException", false, MqNativeAdminServerSupport.class.getClassLoader());
                        if (aClass.isInstance(t)) {
                            //noinspection ThrowableResultOfMethodCallIgnored
                            logger.log(Level.INFO, "Caught MQException while testing MQ Native destination '" + ExceptionUtils.getMessage(t) + "'.", ExceptionUtils.getDebugException(t));
                            throw new MqNativeTestException(testName + ExceptionUtils.getMessage(t));
                        }
                    } catch (ClassNotFoundException e) {
                        throw new MqNativeTestException("Unable to test MQ Native destination as MQ Native jars are not installed.");
                    }
                    // fall through for non MQException
                    //noinspection ThrowableResultOfMethodCallIgnored
                    logger.log(Level.INFO, "Caught Throwable while testing MQ Native destination '" + ExceptionUtils.getMessage(t) + "'.", ExceptionUtils.getDebugException(t));
                    throw new MqNativeTestException(testName + ExceptionUtils.getMessage(t));
                } finally {
                    closeQuietly(targetQueue);
                    closeQuietly(replyQueue);
                    closeQuietly( queueManager, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some(new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
                        @Override
                        public void call(final MQQueueManager mqQueueManager) throws MQException {
                            mqQueueManager.disconnect();
                        }
                    }) );
                }
            }
        });

        return Collections.singletonList(binding);
    }
}