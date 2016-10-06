package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.l7tech.external.assertions.mqnative.server.MqNativeClient.ClientBag;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.UnaryThrows;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import static com.ibm.mq.constants.MQConstants.MQGMO_SYNCPOINT;
import static com.ibm.mq.constants.MQConstants.MQGMO_WAIT;
import static com.ibm.mq.constants.MQConstants.MQGMO_PROPERTIES_FORCE_MQRFH2;
import static com.ibm.mq.constants.MQConstants.MQGMO_CONVERT;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_CONVERT_MESSAGE_APPLICATION_DATA_FORMAT;
import static com.l7tech.server.ServerConfigParams.PARAM_IO_MQ_FORCE_RETURN_PROPS_IN_MQRFH2_HEADER;
import static java.text.MessageFormat.format;

/**
 * Listener thread responsible for receiving messages from the MQ endpoint.
 */
class MqNativeListenerThread extends Thread {
    // The amount of time the thread sleeps when the MAXIMUM_OOPSES limit is reached
    private final AtomicLong oopsSleep = new AtomicLong(MqNativeListener.DEFAULT_OOPS_SLEEP);
    // Time interval to wait before polling again on an empty queue
    private final AtomicInteger pollInterval = new AtomicInteger(MqNativeListener.DEFAULT_POLL_INTERVAL);
    // Whether to convert the message application data format
    private final AtomicBoolean messageDataConversionEnabled = new AtomicBoolean(MqNativeListener.MESSAGE_DATA_CONVERSION_ENABLED);
    // Whether to force returning MQ Message properties in the MQRFH2 header
    private final AtomicBoolean forceReturnPropertiesInMQFRH2Header = new AtomicBoolean(MqNativeListener.FORCE_PROPS_IN_MQRFH2_ENABLED);

    private final MqNativeListener mqNativeListener;
    private final String connectorInfo;

    MqNativeListenerThread(MqNativeListener mqNativeEndpointListener, String threadName) {
        super(threadName);
        setDaemon( true );
        this.mqNativeListener = mqNativeEndpointListener;
        this.connectorInfo = mqNativeEndpointListener.getDisplayName();
    }

    public void setOopsSleep(long oopsSleepLong) {
        this.oopsSleep.set(oopsSleepLong);
    }

    public void setPollInterval(int pollIntervalInt) {
        this.pollInterval.set(pollIntervalInt);
    }

    public void setMessageDataConversionEnabled(boolean enabledState) {
        this.messageDataConversionEnabled.set(enabledState);
    }

    public void setForceReturnPropertiesInMQFRH2Header(boolean force) {
        this.forceReturnPropertiesInMQFRH2Header.set(force);
    }

    @Override
    public final void run() {
        mqNativeListener.log(Level.INFO, MqNativeMessages.INFO_LISTENER_POLLING_START, connectorInfo);

        int oopses = 0;
        try {
            MQMessage mqMessage;
            while ( !mqNativeListener.isStop() ) {
                try {
                    mqMessage = mqNativeListener.doWithMqNativeClient( new UnaryThrows<MQMessage,ClientBag,MQException>() {
                        @Override
                        public MQMessage call( final ClientBag bag ) throws MQException {
                            final MQGetMessageOptions getOptions = new MQGetMessageOptions();
                            getOptions.options = MQGMO_WAIT | MQGMO_SYNCPOINT;

                            if (messageDataConversionEnabled.get()) {
                                getOptions.options |= MQGMO_CONVERT;
                            }

                            if (forceReturnPropertiesInMQFRH2Header.get()) {
                                getOptions.options |= MQGMO_PROPERTIES_FORCE_MQRFH2;
                            }

                            getOptions.waitInterval = pollInterval.get();
                            return mqNativeListener.receiveMessage( bag.getTargetQueue(), getOptions );
                        }
                    });

                    mqNativeListener.log(Level.FINE, MqNativeMessages.INFO_LISTENER_RECEIVE_MSG, connectorInfo,
                            mqMessage == null ? null : new String(mqMessage.messageId));

                    if ( mqMessage != null && !mqNativeListener.isStop() ) {
                        oopses = 0;

                        // process the message
                        mqNativeListener.handleMessage(mqMessage);
                    }
                } catch ( Throwable e ) {
                    if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                        mqNativeListener.log(Level.FINE, "MQ listener on {0} caught throwable: " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e));
                        continue;
                    }

                    if (!ExceptionUtils.causedBy(e, RejectedExecutionException.class)) {
                        mqNativeListener.addAuditDetail(format(MqNativeMessages.WARN_LISTENER_RECEIVE_ERROR,
                                connectorInfo, ExceptionUtils.getMessage(e)),
                                ExceptionUtils.getDebugException(e));
                        mqNativeListener.cleanup();
                    } else {
                        mqNativeListener.addAuditDetail(format("Running out threads in the MQ ThreadPool, " +
                                "consider increasing the mq.listenerThreadLimit : {0}", e.getMessage()));
                    }

                    if ( ++oopses < MqNativeListener.MAXIMUM_OOPSES ) {
                        // sleep for a short period of time before retrying
                        mqNativeListener.log(Level.FINE, "MQ listener on {0} sleeping for {1} milliseconds.",
                                connectorInfo, mqNativeListener.getOopsRetry());
                        try {
                            Thread.sleep(mqNativeListener.getOopsRetry());
                        } catch ( InterruptedException e1 ) {
                            mqNativeListener.log(Level.INFO, MqNativeMessages.INFO_LISTENER_POLLING_INTERRUPTED, "retry interval");
                        }

                    } else {
                        // max oops reached .. sleep for a longer period of time before retrying
                        long sleepTime = oopsSleep.get();
                          mqNativeListener.addAuditDetail(format(MqNativeMessages.WARN_LISTENER_MAX_OOPS_REACHED, connectorInfo,
                                MqNativeListener.MAXIMUM_OOPSES, sleepTime));

                        try {
                            Thread.sleep(sleepTime);
                        } catch ( InterruptedException e1 ) {
                            mqNativeListener.log(Level.INFO, MqNativeMessages.INFO_LISTENER_POLLING_INTERRUPTED, "sleep interval");
                        }
                    }
                }
            }
        } finally {
            mqNativeListener.log(Level.INFO, MqNativeMessages.INFO_LISTENER_POLLING_STOP, connectorInfo);
            mqNativeListener.cleanup();
        }
    }
}
