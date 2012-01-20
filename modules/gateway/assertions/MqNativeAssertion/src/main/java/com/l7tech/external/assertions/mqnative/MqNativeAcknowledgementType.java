package com.l7tech.external.assertions.mqnative;

/**
 * Message acknowledgement types for MQ.
 */
public enum MqNativeAcknowledgementType {
    /**
     * The message is acknowledged when taken from the Queue.
     */
    AUTOMATIC,

    /**
     * The message is acknowledged when policy execution completes.
     */
    ON_COMPLETION
}
