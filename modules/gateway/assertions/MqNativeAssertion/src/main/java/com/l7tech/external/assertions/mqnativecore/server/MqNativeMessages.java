package com.l7tech.external.assertions.mqnativecore.server;

import com.l7tech.gateway.common.audit.Messages;

import java.util.logging.Level;

/**
 * This is the central place that consolidates all messages used by the MQ native subsystem for exceptions or logging.
 */
public class MqNativeMessages extends Messages {

    /* Error messages */
//    public static final String ERROR_xxx = "";

    /* Warning messages */
    public static final String WARN_LISTENER_THREAD_ALIVE = "Listener thread did not shutdown on request ({0})";
    public static final String WARN_LISTENER_RECEIVE_ERROR = "Unable to receive message from MQ endpoint {0}";
    public static final String WARN_LISTENER_MAX_OOPS_REACHED = "Listener for endpoint {0} received too many errors ({1}) - will try again in {2} ms";
    public static final String WARN_THREADPOOL_LIMIT_REACHED = "MQ ThreadPool size limit reached.  Unable to add new MqTask: {0}";
    public static final String WARN_PROPERTY_CHANGE_IGNORE_SLEEPTIME = "";

    /* Info messages */
    public static final String INFO_LISTENER_START = "Starting listener {0} ...";
    public static final String INFO_LISTENER_STARTED = "Started {0}";
    public static final String INFO_LISTENER_STOP = "Stopping listener {0}";
    public static final String INFO_LISTENER_POLLING_START = "Starting MQ (native) poller on {0}";
    public static final String INFO_LISTENER_POLLING_STOP = "Stopping MQ (native) poller on {0}";
    public static final String INFO_LISTENER_POLLING_INTERRUPTED = "Listener thread interrupted during {0}";
    public static final String INFO_LISTENER_RECEIVE_MSG = "Message received on {0} : {1}";

    public static final String INFO_EVENT_CONNECT_SUCCESS = "Connected to MQ endpoint ({0})";
    public static final String INFO_EVENT_CONNECT_FAIL = "Error connecting to MQ endpoint ({0}) with: {1}";
    public static final String INFO_EVENT_NOT_PUBLISHED = "Not publishing event due to recent failure.";
    public static final String INFO_EVENT_NOT_PUBLISHABLE = "Event not published, message is: {0}";
    
    // ServerJmsRoutingAssertion
    public static final Messages.M MQ_ROUTING_CONNECT_FAILED                  = m(9300, Level.INFO, "Failed to establish MQ connection on try #{0}: Will retry after {1}ms");
    public static final Messages.M MQ_ROUTING_INBOUD_REQUEST_QUEUE_NOT_EMPTY  = m(9301, Level.FINE,  "Inbound request queue is not temporary; using selector to filter responses to our message");
    public static final Messages.M MQ_ROUTING_NO_TOPIC_WITH_REPLY             = m(9302, Level.WARNING, "Topics not supported when reply type is not NO_REPLY");
    public static final Messages.M MQ_ROUTING_REQUEST_ROUTED                  = m(9303, Level.FINER, "Routing request to protected service");
    public static final Messages.M MQ_ROUTING_GETTING_RESPONSE                = m(9304, Level.FINEST, "Getting response from protected service");
    public static final Messages.M MQ_ROUTING_NO_RESPONSE                     = m(9305, Level.WARNING, "Did not receive a routing reply within the timeout period of {0} ms; empty response being returned");
    public static final Messages.M MQ_ROUTING_GOT_RESPONSE                    = m(9306, Level.FINER, "Received routing reply");
    public static final Messages.M MQ_ROUTING_UNSUPPORTED_RESPONSE_MSG_TYPE   = m(9307, Level.WARNING, "Received MQ reply with unsupported message type {0}");
    public static final Messages.M MQ_ROUTING_NO_RESPONSE_EXPECTED            = m(9308, Level.INFO, "No response expected from protected service");
    public static final Messages.M MQ_ROUTING_DELETE_TEMPORARY_QUEUE          = m(9309, Level.FINER, "Deleting temporary queue" );
    @Deprecated public static final Messages.M __UNUSED_MQ_ROUTING_RETURN_NO_REPLY        = m(9310, Level.FINER, "Returning NO_REPLY (null) for {0}");
    @Deprecated public static final Messages.M __UNUSED_MQ_ROUTING_RETURN_AUTOMATIC       = m(9311, Level.FINER, "Returning AUTOMATIC {0} for {1}");
    @Deprecated public static final Messages.M __UNUSED_MQ_ROUTING_RETURN_REPLY_TO_OTHER  = m(9312, Level.FINER, "Returning REPLY_TO_OTHER {0} for {1}");
    public static final Messages.M MQ_ROUTING_UNKNOW_MQ_REPLY_TYPE           = m(9313, Level.WARNING, "Unknown JmsReplyType {0}");
    @Deprecated public static final Messages.M __UNUSED_MQ_ROUTING_ENDPOINTS_ON_SAME_CONNECTION    = m(9314, Level.WARNING, "Request and reply endpoints must belong to the same connection");
    public static final Messages.M MQ_ROUTING_CREATE_REQUEST_AS_TEXT_MESSAGE  = m(9315, Level.FINER, "Creating request as TextMessage");
    public static final Messages.M MQ_ROUTING_CREATE_REQUEST_AS_BYTES_MESSAGE = m(9316, Level.FINER, "Creating request as BytesMessage");
    public static final Messages.M MQ_ROUTING_REQUEST_WITH_NO_REPLY           = m(9317, Level.FINE, "Outbound request endpoint {0} specifies NO_REPLY");
    public static final Messages.M MQ_ROUTING_REQUEST_WITH_REPLY_TO_OTHER     = m(9318, Level.FINE, "Outbound request endpoint {0} specifies REPLY_TO_OTHER, setting replyToQueueName to {1}");
    public static final Messages.M MQ_ROUTING_NON_EXISTENT_ENDPOINT           = m(9319, Level.WARNING, "Route via MQ Assertion contains a reference to nonexistent JmsEndpoint #{0}");
    public static final Messages.M MQ_ROUTING_NO_SAML_SIGNER                  = m(9320, Level.WARNING, "Route via MQ Assertion cannot access SAML signing information");
    public static final Messages.M MQ_ROUTING_CANT_CONNECT_RETRYING           = m(9321, Level.WARNING, "Failed to establish MQ connection on try #{0}.  Will retry after {1}ms.");
    public static final Messages.M MQ_ROUTING_CANT_CONNECT_NOMORETRIES        = m(9322, Level.WARNING, "Tried {0} times to establish MQ connection and failed.");
    public static final Messages.M MQ_ROUTING_REQUEST_WITH_AUTOMATIC          = m(9323, Level.FINE, "Outbound request endpoint {0} specifies AUTOMATIC, using temporary queue");
    public static final Messages.M MQ_ROUTING_REQUEST_TOO_LARGE               = m(9324, Level.WARNING, "Request message too large.");
    public static final Messages.M MQ_ROUTING_TEMPLATE_ERROR                  = m(9325, Level.WARNING, "Error processing MQ outbound template ''{0}''.");
    public static final Messages.M MQ_ROUTING_DESTINATION_SESSION_MISMATCH    = m(9326, Level.WARNING, "MQ Destination/Session type mismatch.");
    public static final Messages.M MQ_ROUTING_MISSING_MESSAGE_ID              = m(9327, Level.WARNING, "Sent message had no message ID. Unable to correlate.");
    public static final Messages.M MQ_ROUTING_CONFIGURATION_ERROR             = m(9328, Level.WARNING, "Invalid MQ configuration ''{0}''.");
}