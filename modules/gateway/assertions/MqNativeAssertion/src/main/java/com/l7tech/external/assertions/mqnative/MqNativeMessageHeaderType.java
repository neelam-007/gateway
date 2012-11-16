package com.l7tech.external.assertions.mqnative;

/**
 * Supported MQ message header types.
 */
public enum MqNativeMessageHeaderType {
    /**
     * Use the primary original message header from the source message.
     */
    ORIGINAL("Original"),
    MQRFH2("MQRFH2"),
    MQRFH1("MQRFH1"),
    /**
     * Don't use a header, use MQ message properties to store data.
     */
    NO_HEADER("No Header"),
    UNSUPPORTED("");

    private String displayName;

    private MqNativeMessageHeaderType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
