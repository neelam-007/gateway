/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: MqResBag.java 27341 2010-12-02 19:33:31Z vchan $
 */

package com.l7tech.external.assertions.mqnative.server;

import com.ibm.mq.*;
import com.l7tech.external.assertions.mqnative.MqNativeReplyType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MQ native client for the Gateway to access a MQ server.
 */
public class MqNativeClient implements Closeable {
    private static final Logger logger = Logger.getLogger(MqNativeClient.class.getName());

    /**
     * Listener interface for connection notifications
     */
    static interface MqNativeConnectionListener {
        void notifyConnected();
        void notifyConnectionError( String message );
    }

    private final MQQueueManager queueManager;
    private final MQQueue targetQueue;
    private final MQQueue specifiedReplyQueue;
    private MQGetMessageOptions mqGetMessageOptions;
    private final MQPutMessageOptions mqPutMessageOptions;
    private final MQGetMessageOptions browseFirstOptions, browseNextOptions;
    private final MQGetMessageOptions onCompleteAcknowledgementOptions, onTakeAcknowledgementOptions;
    private final MQPutMessageOptions onCompleteAutoReplyOptions, onTakeAutoReplyOptions, onCompleteSpecifiedReplyOptions, onTakeSpecifiedReplyOptions;
    private final MqNativeConnectionListener connectionListener;

    public MqNativeClient(@NotNull final MQQueueManager queueManager,
                          @NotNull final MQQueue requestQueue,
                          MQQueue specifiedReplyQueue, int receiveTimeout,
                          @NotNull final MqNativeConnectionListener listener) {
        this.queueManager = queueManager;
        this.targetQueue = requestQueue;
        this.specifiedReplyQueue = specifiedReplyQueue;
        this.connectionListener = listener;

        // write options
        this.mqPutMessageOptions = new MQPutMessageOptions();
        this.mqPutMessageOptions.options = MQC.MQPMO_NEW_MSG_ID | MQC.MQPMO_NO_SYNCPOINT;

        // read-browse options to peek at a message on the queue, leaving it on queue
        this.browseFirstOptions = new MQGetMessageOptions();
        this.browseFirstOptions.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_FIRST; // | MQC.MQGMO_LOCK;
        this.browseFirstOptions.waitInterval = receiveTimeout;
        this.browseNextOptions = new MQGetMessageOptions();
        this.browseNextOptions.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_NEXT; // | MQC.MQGMO_LOCK;
        this.browseNextOptions.waitInterval = receiveTimeout;

        // read-acknowledge options to pop a message off the queue, removing it from queue
        this.onCompleteAcknowledgementOptions = new MQGetMessageOptions();
        this.onCompleteAcknowledgementOptions.options = MQC.MQGMO_WAIT | MQC.MQGMO_SYNCPOINT;
        this.onCompleteAcknowledgementOptions.matchOptions = MQC.MQMO_MATCH_MSG_ID;
        this.onCompleteAcknowledgementOptions.waitInterval = receiveTimeout;
        this.onTakeAcknowledgementOptions = new MQGetMessageOptions();
        this.onTakeAcknowledgementOptions.options = MQC.MQGMO_WAIT | MQC.MQGMO_NO_SYNCPOINT;
        this.onTakeAcknowledgementOptions.matchOptions = MQC.MQMO_MATCH_MSG_ID;

        // write-reply options
        this.onCompleteAutoReplyOptions = new MQPutMessageOptions();
        this.onCompleteAutoReplyOptions.options = MQC.MQPMO_NEW_MSG_ID | MQC.MQPMO_SYNCPOINT;
        this.onTakeAutoReplyOptions = new MQPutMessageOptions();
        this.onTakeAutoReplyOptions.options = MQC.MQPMO_NEW_MSG_ID | MQC.MQPMO_NO_SYNCPOINT;
        this.onCompleteSpecifiedReplyOptions = new MQPutMessageOptions();
        this.onCompleteSpecifiedReplyOptions.options = MQC.MQPMO_SYNCPOINT;
        this.onTakeSpecifiedReplyOptions = new MQPutMessageOptions();
        this.onTakeSpecifiedReplyOptions.options = MQC.MQPMO_NO_SYNCPOINT;
    }

    <R> R doWork( final Functions.BinaryThrows<R,MQQueue,MQGetMessageOptions,MQException> callback ) throws MQException {
        synchronized (targetQueue) {
            checkConnect();
            return callback.call(targetQueue, getMqGetMessageOptions());
        }
    }

    public MQQueueManager getQueueManager() {
        return queueManager;
    }

    public MQQueue getTargetQueue() {
        return targetQueue;
    }

    public MQQueue getSpecifiedReplyQueue() {
        return specifiedReplyQueue;
    }

    /**
     * Get write options
     * @return message put options
     */
    public MQPutMessageOptions getMqPutMessageOptions() {
        return mqPutMessageOptions;
    }

    /**
     * Get read-browse options to peek at a message on the queue, leaving it on queue.
     * On first call return browseFirstOptions, subsequent call return browseNextOptions.
     * @return message get options
     */
    public MQGetMessageOptions getMqGetMessageOptions() {
        if (mqGetMessageOptions == null) {
            mqGetMessageOptions = browseNextOptions;
            return browseFirstOptions;
        }

        return mqGetMessageOptions;
    }

    /**
     * Get write-reply options
     * @param replyType reply type
     * @param isTransactional whether acknowledgement type is transactional
     * @return message put options
     */
    public MQPutMessageOptions getReplyOptions(@NotNull final MqNativeReplyType replyType, boolean isTransactional) {
        if (replyType == MqNativeReplyType.REPLY_AUTOMATIC) {
            if (isTransactional) {
                return onCompleteAutoReplyOptions;
            } else {
                return onTakeAutoReplyOptions;
            }
        } else if (replyType == MqNativeReplyType.REPLY_SPECIFIED_QUEUE) {
            if (isTransactional) {
                return onCompleteSpecifiedReplyOptions;
            } else {
                return onTakeSpecifiedReplyOptions;
            }
        }
        return null;
    }

    /**
     * Get read-acknowledge options to pop a message off the queue, removing it from queue
     * @param isTransactional whether acknowledgement type is transactional
     * @return message get options
     */
    protected MQGetMessageOptions getAcknowledgeOptions(boolean isTransactional) {
        if (isTransactional) {
            return onCompleteAcknowledgementOptions;
        } else {
            return onTakeAcknowledgementOptions;
        }
    }

    @Override
    public void close() {
        try {
            targetQueue.close();
            if ( specifiedReplyQueue != null ) {
                specifiedReplyQueue.close();
            }
            queueManager.disconnect();
            queueManager.close();
        } catch ( Exception e ) {
            logger.log(Level.WARNING, "Exception while closing MqNativeClient", e);
        }
    }

    private void checkConnect() throws MQException {
        /*final boolean wasConnected = client.isConnected();
        boolean ok = false;
        String message = null;
        try {
            if (!wasConnected) client.connect();
            client.setDir( remoteDirectory );
            ok = true;
        } catch (SftpFileNotFoundException e) {
            message = "Directory not found.";
            throw e;
        } catch (SftpException e) {
            message = ExceptionUtils.getMessage(e);
            if ("com.jscape.inet.sftp.SftpException".equals(message)) {
                final String host = parameters.getSshHostname();
                final int port = parameters.getSshPort();
                message = "Unable to connect to " + host + ":" + port;
                throw new SftpException( message, e );
            }
            throw e;
        } catch (IOException e) {
            message = ExceptionUtils.getMessage(e);
            throw e;
        } catch (RuntimeException e) {
            message = ExceptionUtils.getMessage(e);
            throw e;
        } finally {
            if (ok) {
                if (!wasConnected) {
                    connectionListener.notifyConnected();
                }
            } else {
                connectionListener.notifyConnectionError(message);
            }
        } */
    }

}