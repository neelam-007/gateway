package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.MQException;
import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static java.text.MessageFormat.format;

/**
 * Listener thread responsible for receiving messages from the MQ endpoint.
 */
public class MqNativeListenerThread extends Thread {
    /* The amount of time the thread sleeps when the MAXIMUM_OOPSES limit is reached */
    private final AtomicInteger oopsSleep = new AtomicInteger(MqNativeListener.DEFAULT_OOPS_SLEEP);

    private final MqNativeListener mqNativeListener;
    private final String connectorInfo;

     MqNativeListenerThread(MqNativeListener mqNativeEndpointListener, String threadName) {
        super(threadName);
        setDaemon( true );
        this.mqNativeListener = mqNativeEndpointListener;
        this.connectorInfo = mqNativeEndpointListener.getDisplayName();
    }

    @Override
    public final void run() {
        mqNativeListener.log(Level.INFO, MqNativeMessages.INFO_LISTENER_POLLING_START, connectorInfo);

        int oopses = 0;
        try {
            MQMessage mqMessage;
            MQMessage lastMsg = null;
            boolean retryLastMsg = false;
            while ( !mqNativeListener.isStop() ) {
                try {
                    if (!retryLastMsg || lastMsg == null) {
                        mqMessage = mqNativeListener.doWithMqNativeClient( new Functions.BinaryThrows<MQMessage,MQQueue,MQGetMessageOptions,MQException>() {
                            @Override
                            public MQMessage call(final MQQueue queue, final MQGetMessageOptions getMessageOptions) throws MQException {
                                return mqNativeListener.receiveMessage( queue, getMessageOptions );
                            }
                        });

                        // TODO (TL) add configurable poll interval

                        mqNativeListener.log(Level.FINE, MqNativeMessages.INFO_LISTENER_RECEIVE_MSG, connectorInfo,
                                mqMessage == null ? null : new String(mqMessage.messageId));

                        retryLastMsg = false;
                        lastMsg = null;
                    } else {
                        retryLastMsg = false;
                        mqMessage = lastMsg;
                    }

                    if ( mqMessage != null && !mqNativeListener.isStop() ) {
                        oopses = 0;

                        // process on the message
                        lastMsg = mqMessage;
                        mqNativeListener.handleMessage(mqMessage);

                    }
                } catch ( Throwable e ) {
                    if (ExceptionUtils.causedBy(e, InterruptedException.class)) {
                        mqNativeListener.log(Level.FINE, "MQ listener on {0} caught throwable: " + ExceptionUtils.getMessage(e),
                                ExceptionUtils.getDebugException(e));
                        continue;
                    }

                    if (!ExceptionUtils.causedBy(e, RejectedExecutionException.class)) {
                        mqNativeListener.log(Level.WARNING, format(
                                MqNativeMessages.WARN_LISTENER_RECEIVE_ERROR,
                                connectorInfo, ExceptionUtils.getMessage(e)),
                                ExceptionUtils.getDebugException(e));

                        mqNativeListener.cleanup();
                    } else {
                        mqNativeListener.log(Level.WARNING, "Running out threads in the MQ ThreadPool, " +
                                "consider increasing the mq.listenerThreadLimit : {0}", e.getMessage());
                        retryLastMsg = true;
                    }

                    if ( ++oopses < MqNativeListener.MAXIMUM_OOPSES ) {
                        // sleep for a short period of time before retrying
                        mqNativeListener.log(Level.FINE, "MQ listener on {0} sleeping for {1} milliseconds.",
                                connectorInfo, MqNativeListener.OOPS_RETRY);
                        try {
                            Thread.sleep(MqNativeListener.OOPS_RETRY);
                        } catch ( InterruptedException e1 ) {
                            mqNativeListener.log(Level.INFO, MqNativeMessages.INFO_LISTENER_POLLING_INTERRUPTED, "retry interval");
                        }

                    } else {
                        // max oops reached .. sleep for a longer period of time before retrying
                        int sleepTime = oopsSleep.get();
                        mqNativeListener.log(Level.WARNING, MqNativeMessages.WARN_LISTENER_MAX_OOPS_REACHED, connectorInfo,
                                MqNativeListener.MAXIMUM_OOPSES, sleepTime);
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
