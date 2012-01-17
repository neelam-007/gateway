package com.l7tech.external.assertions.mqnative;

/**
 * Enumerated type for MQ native reply types.
 */
public enum MqNativeMessageFormatType {

    /**
     * Do not send a reply.
     */
    AUTOMATIC,

    /**
     * Send a reply if it was specified in the message.
     */
    BYTES,

    /**
     * Send a reply to a specified queue.
     */
    TEXT
}
