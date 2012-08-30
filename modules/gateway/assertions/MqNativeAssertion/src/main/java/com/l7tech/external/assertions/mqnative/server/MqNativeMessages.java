package com.l7tech.external.assertions.mqnative.server;

/**
 * This is the central place that consolidates all messages used by the MQ native subsystem for exceptions or logging.
 */
class MqNativeMessages {

    /* Error messages */
//    public static final String ERROR_xxx = "";

    /* Warning messages */
    static final String WARN_LISTENER_THREAD_ALIVE = "Listener thread did not shutdown on request ({0})";
    static final String WARN_LISTENER_RECEIVE_ERROR = "Unable to receive message from MQ endpoint {0} with: {1}";
    static final String WARN_LISTENER_MAX_OOPS_REACHED = "Listener for endpoint {0} received too many errors ({1}) - will try again in {2} ms";
    static final String WARN_THREADPOOL_LIMIT_REACHED = "MQ ThreadPool size limit reached.  Unable to add new MqTask: {0}";
    static final String WARN_PROPERTY_CHANGE_IGNORE_SLEEPTIME = "";

    /* Info messages */
    static final String INFO_LISTENER_START = "Starting listener {0} ...";
    static final String INFO_LISTENER_STARTED = "Started {0}";
    static final String INFO_LISTENER_STOP = "Stopping listener {0}";
    static final String INFO_LISTENER_POLLING_START = "Starting MQ (native) poller on {0}";
    static final String INFO_LISTENER_POLLING_STOP = "Stopping MQ (native) poller on {0}";
    static final String INFO_LISTENER_POLLING_INTERRUPTED = "Listener thread interrupted during {0}";
    static final String INFO_LISTENER_RECEIVE_MSG = "Message received on {0} : {1}";

    static final String INFO_EVENT_CONNECT_SUCCESS = "Connected to MQ endpoint ({0})";
    static final String INFO_EVENT_CONNECT_FAIL = "Error connecting to MQ endpoint ({0}) with: {1}";
    static final String INFO_EVENT_NOT_PUBLISHED = "Not publishing this event due to recent failure: {0}";
    static final String INFO_EVENT_NOT_PUBLISHABLE = "Event not published, message is: {0}";
}