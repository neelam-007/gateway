package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.MQC;
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
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.isTransactional;
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

    public long getDefaultMqMessageMaxBytes() {
        return config.getLongProperty(ServerConfigParams.PARAM_IO_MQ_MESSAGE_MAX_BYTES, 2621440L);  // ioMqMessageMaxBytes.default = 2621440
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<MqNativeAdmin>(MqNativeAdmin.class, null, new MqNativeAdmin() {

            @Override
            public long getDefaultMqMessageMaxBytes() {
                return MqNativeAdminServerSupport.this.getDefaultMqMessageMaxBytes();
            }

            @Override
            public void testSettings(SsgActiveConnector mqNativeActiveConnector) throws MqNativeTestException {
                MQQueueManager queueManager = null;
                MQQueue targetQueue = null;
                MQQueue replyQueue = null;
                String testName = "";
                int targetQueueType;
                
                try {
                    // test queue manager
                    logger.finer("Attempting to get queue manager ...");
                    testName = "queue manager - ";
                    queueManager = new MQQueueManager(
                            mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_QUEUE_MANAGER_NAME ),
                            MqNativeUtils.buildQueueManagerConnectProperties( mqNativeActiveConnector, securePasswordManager ) );
                    logger.finer("... successfully got queue manager " + queueManager.name + "!");
                    
                    logger.finer("Attempting to get queue to inquire on its properties ...");
                    testName = "inquiry queue properties (type) - ";
                    targetQueue = queueManager.accessQueue(
                            mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME ), MQC.MQOO_INQUIRE);
                    targetQueueType = targetQueue.getQueueType();
                    targetQueue.close();
                    logger.finer("... successfully got queue properties");

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

                        // if applicable test failure queue
                        String failureQueueName = mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_INBOUND_FAILED_QUEUE_NAME );
                        if (isTransactional( mqNativeActiveConnector )
                                && mqNativeActiveConnector.getBooleanProperty(PROPERTIES_KEY_MQ_NATIVE_INBOUND_IS_FAILED_QUEUE_USED)
                                && !StringUtils.isEmpty(failureQueueName)) {
                            logger.finer("Attempting to get inbound failure queue ...");
                            testName = "inbound failure queue - ";
                            replyQueue = queueManager.accessQueue( failureQueueName, QUEUE_OPEN_OPTIONS_INBOUND_FAILURE_QUEUE );
                            replyQueue.close();
                            logger.finer("... successfully got inbound failure queue " + replyQueue.name + "!");
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

                        if ( targetQueueType !=  MQC.MQQT_REMOTE ) {    // GETs cannot be performed on Remote Queues
                            logger.finer("Attempting to get outbound get queue ...");
                            targetQueue = queueManager.accessQueue(
                                    mqNativeActiveConnector.getProperty( PROPERTIES_KEY_MQ_NATIVE_TARGET_QUEUE_NAME ), QUEUE_OPEN_OPTIONS_OUTBOUND_GET );
                            targetQueue.close();
                            logger.finer("... successfully got outbound get queue " + targetQueue.name + "!");
                        }
                        
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
                } catch (MQException e) {
                    logger.log(Level.INFO, "Caught MQException while testing MQ Native destination '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
                    throw new MqNativeTestException(testName + getMeaningfulMqErrorDetail(e.toString()));
                } catch (Throwable t) {
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

    /**
     * Make a meaningful MQ error detail with MQ Reason Code from a original MQ error message.
     * Currently we handle 6 types of Reason Codes: 2009, 2035, 2058, 2059, 2085, and 2397.
     * If more reason codes are found, please add them into this method.
     * If same codes specify different errors, please update them in this method.
     *
     * @param originalMqErrorMessage: the original MQ error message before this process
     * @return a meaningful error detail
     */
    private String getMeaningfulMqErrorDetail(String originalMqErrorMessage) {
        if (originalMqErrorMessage == null || originalMqErrorMessage.trim().isEmpty()) return null;

        if (originalMqErrorMessage.contains("Reason 2009")) {
            return "Invalid channel name";
        } else if (originalMqErrorMessage.contains("Reason 2035")) {
            return "The user is not authorized to perform the operation attempted";
        } else if (originalMqErrorMessage.contains("Reason 2058")) {
            return "Invalid queue manager name";
        } else if (originalMqErrorMessage.contains("Reason 2059")) {
            return "Invalid host name or port number";
        } else if (originalMqErrorMessage.contains("Reason 2085")) {
            return "Invalid queue name, reply queue name, or failure queue name";
        }  else if (originalMqErrorMessage.contains("Reason 2397")) {
            return "Invalid SSL setting";
        }

        return originalMqErrorMessage;
    }
}