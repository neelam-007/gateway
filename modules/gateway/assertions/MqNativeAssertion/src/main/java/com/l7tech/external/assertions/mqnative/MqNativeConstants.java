package com.l7tech.external.assertions.mqnative;

import com.ibm.mq.MQC;

import java.util.List;

import static com.l7tech.util.CollectionUtils.list;

/**
 * MQ Native constants
 */
public interface MqNativeConstants {

    // Context variables used
    String MQ_MESSAGE_MAX_BYTES_PROPERTY = "ioMqMessageMaxBytes";
    String MQ_MESSAGE_MAX_BYTES_UI_PROPERTY = "io.mqMessageMaxBytes";
    String MQ_MESSAGE_MAX_BYTES_DESC = "Maximum number of bytes permitted for an MQ Native message, or 0 for unlimited (Integer)";

    String LISTENER_THREAD_LIMIT_PROPERTY = "mqListenerThreadLimit";
    String LISTENER_THREAD_LIMIT_UI_PROPERTY = "mq.listenerThreadLimit";
    String LISTENER_THREAD_LIMIT_DESC = "The global limit on the number of processing threads that can be created to work off all MQ Native queue listeners. Value must be >= 5.";

    String MQ_RESPONSE_TIMEOUT_PROPERTY = "ioMqResponseTimeout";
    String MQ_RESPONSE_TIMEOUT_UI_PROPERTY = "io.mqResponseTimeout";
    String MQ_RESPONSE_TIMEOUT_DESC = "Timeout for MQ Native routing to wait on the replyTo queue in milliseconds, default 10 seconds.";

    String MQ_CONNECT_ERROR_SLEEP_PROPERTY = "mqConnectErrorSleep";
    String MQ_CONNECT_ERROR_SLEEP_UI_PROPERTY = "mq.connectErrorSleep";
    String MQ_CONNECT_ERROR_SLEEP_DESC = "Time to sleep after a connection error for an inbound MQ Native queue (timeunit)";

    String MQ_CONNECTION_CACHE_MAX_AGE_PROPERTY = "ioMqConnectionCacheMaxAge";
    String MQ_CONNECTION_CACHE_MAX_AGE_UI_PROPERTY = "io.mqConnectionCacheMaxAge";
    String MQ_CONNECTION_CACHE_MAX_AGE_DESC = "Maximum age for cached MQ Native connections or 0 for no limit (timeunit)";

    String MQ_CONNECTION_CACHE_MAX_IDLE_PROPERTY = "ioMqConnectionCacheMaxIdleTime";
    String MQ_CONNECTION_CACHE_MAX_IDLE_UI_PROPERTY = "io.mqConnectionCacheMaxIdleTime";
    String MQ_CONNECTION_CACHE_MAX_IDLE_DESC = "The maximum time an idle MQ Native connection will be cached or 0 for no limit (timeunit)";

    String MQ_CONNECTION_CACHE_MAX_SIZE_PROPERTY = "ioMqConnectionCacheSize";
    String MQ_CONNECTION_CACHE_MAX_SIZE_UI_PROPERTY = "io.mqConnectionCacheSize";
    String MQ_CONNECTION_CACHE_MAX_SIZE_DESC = "The number of MQ Native connections to cache (not a hard limit) or 0 for no limit (Integer)";

    // configurable MQ properties (from Queue config UI)
    String MQ_PROPERTY_APPDATA = "applicationIdData";
    String MQ_PROPERTY_APPORIGIN = "applicationOriginData";
    String MQ_PROPERTY_CHARSET = "characterSet";
    String MQ_PROPERTY_ENCODING = "encoding";
    String MQ_PROPERTY_EXPIRY = "expiry";
    String MQ_PROPERTY_FEEDBACK = "feedback";
    String MQ_PROPERTY_FORMAT = "format";
    String MQ_PROPERTY_GROUPID = "groupId";
    String MQ_PROPERTY_MSG_FLAGS = "messageFlags";
    String MQ_PROPERTY_MSG_SEQNUM = "messageSequenceNumber";
    String MQ_PROPERTY_MSG_TYPE = "messageType";
    String MQ_PROPERTY_OFFSET = "offset";
    String MQ_PROPERTY_PERSISTENCE = "persistence";
    String MQ_PROPERTY_PRIORITY = "priority";
    String MQ_PROPERTY_APPNAME = "putApplicationName";
    String MQ_PROPERTY_APPTYPE = "putApplicationType";
    String MQ_PROPERTY_REPORT = "report";
    String MQ_PROPERTY_USERID = "userId";

    /**
     * Properties to expose as options in the UI
     */
    List<String> MQ_PROPERTIES = list(
            MQ_PROPERTY_CHARSET,
            MQ_PROPERTY_ENCODING,
            MQ_PROPERTY_EXPIRY,
            MQ_PROPERTY_FEEDBACK,
            MQ_PROPERTY_FORMAT,
            MQ_PROPERTY_MSG_FLAGS,
            MQ_PROPERTY_PERSISTENCE,
            MQ_PROPERTY_PRIORITY );

    // queue open options
    int QUEUE_OPEN_OPTIONS_INBOUND = MQC.MQOO_INPUT_AS_Q_DEF | MQC.MQOO_INQUIRE;
    int QUEUE_OPEN_OPTIONS_INBOUND_REPLY_SPECIFIED_QUEUE = MQC.MQOO_OUTPUT;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_PUT = MQC.MQOO_OUTPUT | MQC.MQOO_FAIL_IF_QUIESCING;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_GET = MQC.MQOO_INPUT_AS_Q_DEF;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_SPECIFIED_QUEUE = QUEUE_OPEN_OPTIONS_OUTBOUND_GET;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_MODEL_QUEUE = MQC.MQOO_INPUT_AS_Q_DEF | MQC.MQOO_INQUIRE;
}
