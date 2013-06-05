package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import static com.l7tech.external.assertions.mqnative.MqNativeConstants.*;
import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.closeQuietly;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Functions.NullaryThrows;
import com.l7tech.util.Functions.UnaryThrows;
import com.l7tech.util.Option;

import static com.l7tech.external.assertions.mqnative.server.MqNativeUtils.getIntboundReplyMessageOption;
import static com.l7tech.util.Option.none;
import static com.l7tech.util.Option.some;

import com.l7tech.util.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.Hashtable;

/**
 * MQ native client for the Gateway to access a MQ server.
 */
class MqNativeClient implements Closeable {

    /**
     * Listener interface for connection notifications
     */
    static interface MqNativeConnectionListener {
        void notifyConnected();
        void notifyConnectionError( String message );
    }

    final String queueManagerName;
    final NullaryThrows<Hashtable, MqNativeConfigException> queueManagerProperties;
    final String queueName;
    final Option<String> replyQueueName;
    Option<ClientBag> clientBag = none();
    private final MqNativeConnectionListener connectionListener;

    public MqNativeClient( @NotNull final String queueManagerName,
                           @NotNull final NullaryThrows<Hashtable, MqNativeConfigException> queueManagerProperties,
                           @NotNull final String queueName,
                           @NotNull final Option<String> replyQueueName,
                           @NotNull final MqNativeConnectionListener listener) {
        this.queueManagerName = queueManagerName;
        this.queueManagerProperties = queueManagerProperties;
        this.queueName = queueName;
        this.replyQueueName = replyQueueName;
        this.connectionListener = listener;
    }

    <R> R doWork( final UnaryThrows<R,ClientBag,MQException> callback ) throws MQException, MqNativeConfigException {
        return doWork( callback, true );
    }

    <R> R doWork( final UnaryThrows<R,ClientBag,MQException> callback,
                  final boolean reconnect ) throws MQException, MqNativeConfigException {
        checkConnect(reconnect);
        return callback.call( clientBag.some() );
    }

    @Override
    public void close() {
        if ( clientBag.isSome() ) {
            clientBag.some().close();
        }
    }

    protected Triple<MQQueueManager,MQQueue,MQQueue> getMqContext(String queueManagerName, Hashtable queueManagerProperties)
            throws MQException, MqNativeConfigException {

        MQQueueManager queueManager = new MQQueueManager( queueManagerName, queueManagerProperties);
        MQQueue targetQueue = queueManager.accessQueue( queueName, QUEUE_OPEN_OPTIONS_INBOUND );
        MQQueue specifiedReplyQueue = replyQueueName.isSome() ?
                queueManager.accessQueue( replyQueueName.some(), getIntboundReplyMessageOption() ) :
                null;

        return new Triple<MQQueueManager,MQQueue,MQQueue>(queueManager,targetQueue,specifiedReplyQueue);
    }

    private void checkConnect( final boolean reconnect ) throws MQException, MqNativeConfigException {
        if ( !clientBag.isSome() ||
             !clientBag.some().getQueueManager().isConnected() ||
             !clientBag.some().getQueueManager().isOpen() ) {

            close();

            if ( !reconnect ) {
                clientBag = none();
                throw new MqNativeConfigException("Connection lost");
            }

            MQQueueManager queueManager = null;
            MQQueue targetQueue = null;
            MQQueue  specifiedReplyQueue = null;
            try {
                Triple<MQQueueManager,MQQueue,MQQueue> mqContext = getMqContext(queueManagerName,queueManagerProperties.call());
                queueManager = mqContext.left;
                targetQueue = mqContext.middle;
                specifiedReplyQueue = mqContext.right;

                clientBag = some( new ClientBag( queueManager, targetQueue, specifiedReplyQueue ) );
                connectionListener.notifyConnected();

                // resources now owned by client bag
                queueManager = null;
                targetQueue = null;
                specifiedReplyQueue = null;
            //TODO [jdk7] Multicatch
            } catch ( MQException e ) {
                connectionListener.notifyConnectionError( ExceptionUtils.getMessage( e ) );
                throw e;
            } catch ( MqNativeConfigException e ) {
                connectionListener.notifyConnectionError( ExceptionUtils.getMessage( e ) );
                throw e;
            } finally {
                closeResources( queueManager, targetQueue, specifiedReplyQueue );
            }
        }
        
    }

    private static void closeResources( final MQQueueManager queueManager, final MQQueue targetQueue, final MQQueue specifiedReplyQueue ) {
        closeQuietly( specifiedReplyQueue );
        closeQuietly( targetQueue );
        closeQuietly( queueManager, Option.<Functions.UnaryVoidThrows<MQQueueManager, MQException>>some( new Functions.UnaryVoidThrows<MQQueueManager, MQException>() {
            @Override
            public void call( final MQQueueManager mqQueueManager ) throws MQException {
                mqQueueManager.disconnect();
            }
        } ) );
    }

    protected void setClientBag(Option<ClientBag> clientBag) {
        this.clientBag = clientBag;
    }

    public static class ClientBag implements Closeable {
        private final MQQueueManager queueManager;
        private final MQQueue targetQueue;
        private final MQQueue specifiedReplyQueue;

        private ClientBag( @NotNull  final MQQueueManager queueManager,
                           @NotNull  final MQQueue targetQueue,
                           @Nullable final MQQueue specifiedReplyQueue ) {
            this.queueManager = queueManager;
            this.targetQueue = targetQueue;
            this.specifiedReplyQueue = specifiedReplyQueue;
        }

        public MQQueueManager getQueueManager() {
            return queueManager;
        }

        public MQQueue getTargetQueue() {
            return targetQueue;
        }

        @Nullable
        public MQQueue getSpecifiedReplyQueue() {
            return specifiedReplyQueue;
        }

        public void close() {
            closeResources( queueManager, targetQueue, specifiedReplyQueue );
        }
    }
}
