package com.l7tech.external.assertions.mqnative;

/**
 * Enumerated type for MQ native reply types.
 */
public enum MqNativeReplyType {

    /**
     * Do not send a reply.
     */
    REPLY_NONE,

    /**
     * Send a reply if it was specified in the message.
     */
    REPLY_AUTOMATIC,

    /**
     * Send a reply to a specified queue.
     */
    REPLY_SPECIFIED_QUEUE
}
