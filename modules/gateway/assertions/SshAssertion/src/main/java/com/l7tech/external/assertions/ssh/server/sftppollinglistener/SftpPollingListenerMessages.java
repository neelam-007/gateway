package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.gateway.common.audit.Messages;

/**
 * This is the central place that consolidates all messages used by the SFTP polling subsystem for exceptions or logging.
 */
public class SftpPollingListenerMessages extends Messages {

    /* Warning messages */
    public static final String WARN_LISTENER_THREAD_ALIVE = "Listener thread did not shutdown on request ({0})";
    public static final String WARN_LISTENER_RECEIVE_ERROR = "Unable to receive message from SFTP listener {0}";
    public static final String WARN_LISTENER_MAX_OOPS_REACHED = "SFTP listener {0} received too many errors ({1}) - will try again in {2} ms";
    public static final String WARN_THREADPOOL_LIMIT_REACHED = "SFTP Polling Listener ThreadPool size limit reached.  Unable to add new SftpPollingTask: {0}";

    /* Info messages */
    public static final String INFO_LISTENER_START = "Starting listener {0} ...";
    public static final String INFO_LISTENER_STARTED = "Started {0}";
    public static final String INFO_LISTENER_STOP = "Stopping listener {0}";
    public static final String INFO_LISTENER_POLLING_START = "Starting SFTP poller on {0}";
    public static final String INFO_LISTENER_POLLING_STOP = "Stopping SFTP poller on {0}";
    public static final String INFO_LISTENER_POLLING_INTERRUPTED = "Listener thread interrupted during {0}";
    public static final String INFO_LISTENER_RECEIVE_MSG = "Message received on {0} : {1}";

    public static final String INFO_EVENT_CONNECT_SUCCESS = "Connected to SFTP listener ({0})";
    public static final String INFO_EVENT_CONNECT_FAIL = "Error connecting to SFTP listener ({0}) with: {1}";
    public static final String INFO_EVENT_NOT_PUBLISHED = "Not publishing event due to recent failure.";
    public static final String INFO_EVENT_NOT_PUBLISHABLE = "Event not published, message is: {0}";
}