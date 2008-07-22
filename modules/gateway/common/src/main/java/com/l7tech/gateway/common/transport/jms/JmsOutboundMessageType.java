package com.l7tech.gateway.common.transport.jms;

import java.io.Serializable;

/**
 * Configures whether to use a TextMessage or BytesMessage for outbound JMS.
 */
public enum JmsOutboundMessageType implements Serializable {

    /** Create TextMessage only if forwarding a request that arrived over JMS as a TextMessage; otherwise BytesMessage. */
    AUTOMATIC(false, true),

    /** Always create a TextMessage. */
    ALWAYS_TEXT(true, false),

    /** Always create a BytesMessage. */
    ALWAYS_BINARY(false, false),

    ; // Begin fields and methods

    private final boolean defaultsToText;
    private final boolean copyRequestType;

    private JmsOutboundMessageType(boolean defaultsToText, boolean copyRequestType) {
        this.defaultsToText = defaultsToText;
        this.copyRequestType = copyRequestType;
    }

    /** @return true if the the default type should be TextMessage; otherwise it should be BytesMessage. */
    public boolean isDefaultsToText() {
        return defaultsToText;
    }

    /** @return true if the routing assertion should use the message type of a forwarded JMS request. */
    public boolean isCopyRequestType() {
        return copyRequestType;
    }
}
