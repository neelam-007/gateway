package com.l7tech.external.assertions.mqnative;

import com.ibm.mq.headers.MQRFH;
import com.ibm.mq.headers.MQRFH2;
import com.l7tech.external.assertions.mqnative.server.header.MqRfh1;
import com.l7tech.external.assertions.mqnative.server.header.MqRfh2;
import com.l7tech.external.assertions.mqnative.server.header.MqUnsupportedHeader;

/**
 * Supported MQ message header types.
 */
public enum MqNativeMessageHeaderType {
    /**
     * Use the primary original message header from the source message.
     */
    ORIGINAL("Original", null, null),
    MQRFH2("MQRFH2", "com.ibm.mq.headers.MQRFH2", MqRfh2.class),
    MQRFH1("MQRFH1", "com.ibm.mq.headers.MQRFH", MqRfh1.class);

    private String displayName;
    private String headerClassName;
    private Class adaptorClass;

    private MqNativeMessageHeaderType(String displayName, String headerClassName, Class adaptorClass) {
        this.displayName = displayName;
        this.headerClassName = headerClassName;
        this.adaptorClass = adaptorClass;
    }

    public String getHeaderClassName() {
        return headerClassName;
    }

    public Class getAdaptorClass() {
        return adaptorClass;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static MqNativeMessageHeaderType fromValue(String value) {
        if (value != null) {
            for (MqNativeMessageHeaderType e : MqNativeMessageHeaderType.values()) {
                if (e.displayName.equals(value.trim())) {
                    return e;
                }
            }
        }
        return ORIGINAL;
    }
}
