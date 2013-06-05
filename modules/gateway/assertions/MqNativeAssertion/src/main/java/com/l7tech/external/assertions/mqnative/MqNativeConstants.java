package com.l7tech.external.assertions.mqnative;

import java.util.List;

import static com.ibm.mq.constants.MQConstants.*;
import static com.l7tech.util.CollectionUtils.list;

/**
 * MQ Native constants
 */
public interface MqNativeConstants {

    // Context variables used
    String MQ_MESSAGE_MAX_BYTES_PROPERTY = "ioMqMessageMaxBytes";
    String MQ_MESSAGE_MAX_BYTES_UI_PROPERTY = "io.mqMessageMaxBytes";
    String MQ_MESSAGE_MAX_BYTES_DESC = "Maximum number of bytes permitted for an MQ Native message, or 0 for unlimited (Integer)";

    String MQ_LISTENER_POLLING_INTERVAL_PROPERTY = "mqListenerPollingInterval";
    String MQ_LISTENER_POLLING_INTERVAL_UI_PROPERTY = "mq.listenerPollingInterval";
    String MQ_LISTENER_POLLING_INTERVAL_DESC = "Time to wait when polling for messages on an empty queue (timeunit). " +
            "Requires listener (or Gateway) restart.";

    String MQ_LISTENER_THREAD_LIMIT_PROPERTY = "mqListenerThreadLimit";
    String MQ_LISTENER_THREAD_LIMIT_UI_PROPERTY = "mq.listenerThreadLimit";
    String MQ_LISTENER_THREAD_LIMIT_DESC = "The global limit on the number of processing threads that can be created to work " +
            "off all MQ Native queue listeners. Value must be >= 5. Requires Gateway restart.";

    String MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_PROPERTY = "mqListenerMaxConcurrentConnections";
    String MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_UI_PROPERTY = "mq.listenerMaxConcurrentConnections";
    String MQ_LISTENER_MAX_CONCURRENT_CONNECTIONS_DESC = "The maximum number of concurrent connections allowed for any inbound MQ Native queue.  " +
            "This limit will override any larger value entered. Requires listener (or Gateway) restart.";

    String MQ_CONNECT_ERROR_SLEEP_PROPERTY = "mqConnectErrorSleep";
    String MQ_CONNECT_ERROR_SLEEP_UI_PROPERTY = "mq.connectErrorSleep";
    String MQ_CONNECT_ERROR_SLEEP_DESC = "Time to sleep after a connection error for an inbound MQ Native queue (timeunit). " +
            "Requires listener (or Gateway) restart.";

    String MQ_PREVENT_AUDIT_FLOOD_PERIOD_PROPERTY = "mqPreventAuditFloodPeriod";
    String MQ_PREVENT_AUDIT_FLOOD_PERIOD_UI_PROPERTY = "mq.preventAuditFloodPeriod";
    String MQ_PREVENT_AUDIT_FLOOD_PERIOD_DESC = "Time period used to prevent audit message flooding by the MQ Native listener.  " +
            "If the last listener audit message occurred within this period, the next listener message will just log instead.  " +
            "0 for no audit flood throttling (timeunit). Requires listener (or Gateway) restart.";

    String MQ_RESPONSE_TIMEOUT_PROPERTY = "ioMqResponseTimeout";
    String MQ_RESPONSE_TIMEOUT_UI_PROPERTY = "io.mqResponseTimeout";
    String MQ_RESPONSE_TIMEOUT_DESC = "Timeout for MQ Native routing to wait on the reply to queue in milliseconds, default 10 seconds.";

    String MQ_CONNECTION_CACHE_MAX_AGE_PROPERTY = "ioMqConnectionCacheMaxAge";
    String MQ_CONNECTION_CACHE_MAX_AGE_UI_PROPERTY = "io.mqConnectionCacheMaxAge";
    String MQ_CONNECTION_CACHE_MAX_AGE_DESC = "Maximum age for cached MQ Native connections or 0 for no limit (timeunit)";

    String MQ_CONNECTION_CACHE_MAX_IDLE_PROPERTY = "ioMqConnectionCacheMaxIdleTime";
    String MQ_CONNECTION_CACHE_MAX_IDLE_UI_PROPERTY = "io.mqConnectionCacheMaxIdleTime";
    String MQ_CONNECTION_CACHE_MAX_IDLE_DESC = "The maximum time an idle MQ Native connection will be cached or 0 for no limit (timeunit)";

    String MQ_CONNECTION_CACHE_MAX_SIZE_PROPERTY = "ioMqConnectionCacheSize";
    String MQ_CONNECTION_CACHE_MAX_SIZE_UI_PROPERTY = "io.mqConnectionCacheSize";
    String MQ_CONNECTION_CACHE_MAX_SIZE_DESC = "The number of MQ Native connections to cache, this is not a hard limit, 0 for no caching (Integer)";

    // configurable MQ message descriptor fields
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
    String MQ_PROPERTY_BACKOUT_COUNT = "backoutCount";
    String MQ_PROPERTY_REPLY_TO_QUEUE_NAME = "replyToQueueName";
    String MQ_PROPERTY_REPLY_TO_QUEUE_MGR_NAME = "replyToQueueManagerName";
    String MQ_PROPERTY_ORIGINAL_LENGTH = "originalLength";
    String MQ_PROPERTY_VERSION = "version";
    String MQ_PROPERTY_MSG_ID = "messageId";
    String MQ_PROPERTY_CORRELATION_ID = "correlationId";
    String MQ_PROPERTY_ACCOUNTING_TOKEN = "accountingToken";

    // message descriptors exposed as options in the UI
    List<String> MQ_MESSAGE_DESCRIPTORS = list(
            MQ_PROPERTY_CHARSET,
            MQ_PROPERTY_ENCODING,
            MQ_PROPERTY_EXPIRY,
            MQ_PROPERTY_FEEDBACK,
            MQ_PROPERTY_FORMAT,
            MQ_PROPERTY_MSG_FLAGS,
            MQ_PROPERTY_PERSISTENCE,
            MQ_PROPERTY_PRIORITY,
            MQ_PROPERTY_REPLY_TO_QUEUE_NAME,
            MQ_PROPERTY_REPLY_TO_QUEUE_MGR_NAME );

    List<String> MQ_MESSAGE_PROPERTIES = list(
            MQ_JMS_CORRELATION_ID,
            MQ_JMS_DELIVERY_MODE,
            MQ_JMS_DESTINATION,
            MQ_JMS_EXPIRATION,
            MQ_JMSX_GROUP_ID,
            MQ_JMS_PRIORITY,
            MQ_JMS_REPLY_TO,
            MQ_JMSX_GROUP_SEQ,
            MQ_JMS_TIME_STAMP
    );

    // queue open options
    int QUEUE_OPEN_OPTIONS_INBOUND = MQOO_INPUT_AS_Q_DEF | MQOO_INQUIRE;
    int QUEUE_OPEN_OPTIONS_INBOUND_REPLY_SPECIFIED_QUEUE = MQOO_OUTPUT;
    int QUEUE_OPEN_OPTIONS_INBOUND_REPLY_SPECIFIED_QUEUE_SETALLCONTEXT = MQOO_OUTPUT | MQOO_SET_ALL_CONTEXT;
    int QUEUE_OPEN_OPTIONS_INBOUND_FAILURE_QUEUE = MQOO_OUTPUT;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_PUT = MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_PUT_SETALLCONTEXT = MQOO_OUTPUT | MQOO_FAIL_IF_QUIESCING | MQOO_SET_ALL_CONTEXT;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_GET = MQOO_INPUT_AS_Q_DEF;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_SPECIFIED_QUEUE = QUEUE_OPEN_OPTIONS_OUTBOUND_GET;
    int QUEUE_OPEN_OPTIONS_OUTBOUND_REPLY_MODEL_QUEUE = MQOO_INPUT_AS_Q_DEF | MQOO_INQUIRE;
}
