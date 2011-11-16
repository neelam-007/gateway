package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * This is the central place that consolidates all messages used by the SFTP polling subsystem for exceptions or logging.
 */
interface SftpPollingListenerMessages {

    /* Warning messages */
    String WARN_LISTENER_THREAD_ALIVE = "Listener thread did not shutdown on request ({0})";
    String WARN_LISTENER_RECEIVE_ERROR = "Unable to receive message from SFTP listener ''{0}'': {1}";
    String WARN_LISTENER_MAX_OOPS_REACHED = "SFTP listener ''{0}'' received too many errors ({1}) - will try again in {2} ms";
    String WARN_THREADPOOL_LIMIT_REACHED = "SFTP Polling Listener ThreadPool size limit reached.  Unable to add new SftpPollingTask: {0}";

    /* Info messages */
    String INFO_LISTENER_START = "Starting listener {0} ...";
    String INFO_LISTENER_STARTED = "Started {0}";
    String INFO_LISTENER_STOP = "Stopping listener {0}";
    String INFO_LISTENER_POLLING_START = "Starting SFTP poller ''{0}''";
    String INFO_LISTENER_POLLING_STOP = "Stopping SFTP poller ''{0}''";
    String INFO_LISTENER_POLLING_INTERRUPTED = "Listener thread interrupted during {0}";
    String INFO_LISTENER_RECEIVE_MSG = "Message received on {0} : {1}";

    String INFO_EVENT_CONNECT_SUCCESS = "SFTP polling listener ''{0}'' connected";
    String INFO_EVENT_CONNECT_FAIL = "SFTP polling listener ''{0}'' connection error: {1}";
    String INFO_EVENT_NOT_PUBLISHED = "Not publishing event due to recent failure.";
    String INFO_EVENT_NOT_PUBLISHABLE = "Event not published, message is: {0}";
}