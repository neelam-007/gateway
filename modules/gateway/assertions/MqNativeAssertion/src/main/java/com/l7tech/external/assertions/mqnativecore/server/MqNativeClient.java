/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: MqResBag.java 27341 2010-12-02 19:33:31Z vchan $
 */

package com.l7tech.external.assertions.mqnativecore.server;

import com.ibm.mq.*;
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
    private final MqNativeConnectionListener connectionListener;
    private final int receiveTimeout;

    // private final Object sync = new Object();
    // private boolean firstRead = true;
    // private MQGetMessageOptions browseFirstOpts, browseNextOpts, ackOpts;

    /*public MqNativeClient(@NotNull final MQQueueManager queueManager,
                          @NotNull final MQQueue queue,
                          @NotNull final MqNativeConnectionListener listener) {
        this(queueManager, queue, null, null, null, null, listener);
    }*/

    public MqNativeClient(@NotNull final MQQueueManager queueManager,
                          @NotNull final MQQueue requestQueue,
                          MQQueue specifiedReplyQueue, int receiveTimeout, MQPutMessageOptions pmo,
                          @NotNull final MqNativeConnectionListener listener) {
        this.queueManager = queueManager;
        this.targetQueue = requestQueue;
        this.specifiedReplyQueue = specifiedReplyQueue;
        this.connectionListener = listener;
        this.receiveTimeout = receiveTimeout;

        // MQ message options
        /*this.browseFirstOpts = new MQGetMessageOptions();
        this.browseFirstOpts.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_FIRST; // | MQC.MQGMO_LOCK;
        this.browseNextOpts = new MQGetMessageOptions();
        this.browseNextOpts.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_NEXT; // | MQC.MQGMO_LOCK;
        this.ackOpts = new MQGetMessageOptions();
        this.ackOpts.options = MQC.MQGMO_WAIT | MQC.MQGMO_NO_SYNCPOINT;
        this.ackOpts.matchOptions = MQC.MQMO_MATCH_MSG_ID;*/

        if (pmo == null) {
            this.mqPutMessageOptions = new MQPutMessageOptions();
        } else {
            this.mqPutMessageOptions = pmo;
        }
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

    public MQGetMessageOptions getMqGetMessageOptions() {
        /*if (firstRead) {
            synchronized (sync) {
                MQGetMessageOptions gmo;
                if (!firstRead)
                    gmo = browseNextOpts;
                else
                    gmo = browseFirstOpts;

                gmo.waitInterval = receiveTimeout;
                firstRead = false;
                return gmo;
            }
        } else {
            browseNextOpts.waitInterval = receiveTimeout;
            return browseNextOpts;
        }*/

        // set message options
        if (mqGetMessageOptions == null) {
            MQGetMessageOptions browseNextOptions = new MQGetMessageOptions();
            browseNextOptions.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_NEXT; // | MQC.MQGMO_LOCK;
            browseNextOptions.waitInterval = receiveTimeout;
            mqGetMessageOptions = browseNextOptions;

            MQGetMessageOptions browseFirstOptions = new MQGetMessageOptions();
            browseFirstOptions.options = MQC.MQGMO_WAIT | MQC.MQGMO_BROWSE_FIRST; // | MQC.MQGMO_LOCK;
            browseFirstOptions.waitInterval = receiveTimeout;
            return browseFirstOptions;
        }

        return mqGetMessageOptions;
    }

    public MQPutMessageOptions getMqPutMessageOptions() {
        return mqPutMessageOptions;
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