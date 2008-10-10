package com.l7tech.server.transport.email;

/**
 * Contains error and info messages for email listener events.
 */
public class EmailMessages {
    /* Warning messages */
    public static final String WARN_LISTENER_THREAD_ALIVE = "Listener thread did not shutdown on request ({0})";
    public static final String WARN_LISTENER_RECEIVE_ERROR = "Unable to receive message from email server {0}";
    public static final String WARN_LISTENER_MAX_OOPS_REACHED = "Listener for email account {0} received too many errors ({1}) - will try again in {2} ms";
    public static final String WARN_LISTENER_FAILED_MARK_READ = "Failed to mark messages as read on the email server {0}";
    public static final String WARN_LISTENER_FAILED_TO_PROCESS = "Failed to process message #{0} from email server {0}";
    public static final String WARN_PROPERTY_CHANGE_IGNORE_SLEEPTIME = "";
    public static final String WARN_POLL_TIME_UPDATE_FAIL = "Failed to update the last poll time for email listener {0}";

    /* Info messages */
    public static final String INFO_LISTENER_START = "Starting listener {0} ...";
    public static final String INFO_LISTENER_STARTED = "Started {0}";
    public static final String INFO_LISTENER_STOP = "Stopping listener {0}";
    public static final String INFO_LISTENER_POLLING_START = "Starting email poller on {0}";
    public static final String INFO_LISTENER_POLLING_STOP = "Stopping email poller on {0}";
    public static final String INFO_LISTENER_POLLING_INTERRUPTED = "Listener thread interrupted during {0}";
    public static final String INFO_LISTENER_RECEIVE_MSG = "Received a message on {0}";

    public static final String INFO_EVENT_CONNECT_SUCCESS = "Connected to email server ({0})";
    public static final String INFO_EVENT_CONNECT_FAIL = "Error connecting to email server ({0}) with: {1}";
    public static final String INFO_EVENT_NOT_PUBLISHED = "Not publishing event due to recent failure.";
    public static final String INFO_EVENT_NOT_PUBLISHABLE = "Event not published, message is: {0}";
}
