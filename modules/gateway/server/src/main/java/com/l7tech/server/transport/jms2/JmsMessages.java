package com.l7tech.server.transport.jms2;

/**
 * This is the central place that consolidates all messages used by the Jms subsystem
 * for exceptions or logging.
 *
 * @author: vchan
 */
public class JmsMessages {

    /* Error messages */
//    public static final String ERROR_xxx = "";

    /* Warning messages */
    public static final String WARN_LISTENER_THREAD_ALIVE = "Listener thread did not shutdown on request ({0})";
    public static final String WARN_LISTENER_RECEIVE_ERROR = "Unable to receive message from JMS endpoint {0}";
    public static final String WARN_LISTENER_MAX_OOPS_REACHED = "Listener for endpoint {0} received too many errors ({1}) - will try again in {2} ms";
    public static final String WARN_THREADPOOL_LIMIT_REACHED = "JMS ThreadPool size limit reached.  Unable to add new JmsTask: {0}";
    public static final String INFO_THREADPOOL_LIMIT_REACHED_DELAY = "JMS ThreadPool size limit reached.  Delay new JmsTask until working queue is free.";
    public static final String WARN_PROPERTY_CHANGE_IGNORE_SLEEPTIME = "";

    /* Info messages */
    public static final String INFO_LISTENER_START = "Starting listener {0} ...";
    public static final String INFO_LISTENER_STARTED = "Started {0}";
    public static final String INFO_LISTENER_STOP = "Stopping listener {0}";
    public static final String INFO_LISTENER_POLLING_START = "Starting JMS poller on {0}";
    public static final String INFO_LISTENER_POLLING_STOP = "Stopping JMS poller on {0}";
    public static final String INFO_LISTENER_POLLING_INTERRUPTED = "Listener thread interrupted during {0}";
    public static final String INFO_LISTENER_RECEIVE_MSG = "Message received on {0} : {1}";

    public static final String INFO_EVENT_CONNECT_SUCCESS = "Connected to JMS endpoint ({0})";
    public static final String INFO_EVENT_CONNECT_FAIL = "Error connecting to JMS endpoint ({0}) with: {1}";
    public static final String INFO_EVENT_NOT_PUBLISHED = "Not publishing event due to recent failure.";
    public static final String INFO_EVENT_NOT_PUBLISHABLE = "Event not published, message is: {0}";

    /**
     * Helper method to returned the formatted text with the appropriate replacement values.
     *
     * @param key jms message key
     * @param replacements message formatter replacement values
     * @return formatted message string
     */
//    public static String getMessage(String key, Object[] replacements) {
//
//        return MessageFormat.format(key, replacements);
//    }
//
//
//    public static String getMessage(String key, Object replacement) {
//
//        return getMessage(key, new Object[] {replacement});
//    }
//
//    public static String getMessage(String key) {
//
//        return getMessage(key, new Object[0]);
//    }
}
