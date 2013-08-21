/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import java.util.Map;

/**
 * Information about a message that was received or will be sent via JMS.
 */
public interface JmsKnob extends MessageKnob, HasSoapAction, HasServiceId, HasHeaders {
    boolean isBytesMessage();

    /**
     * @return a map of JMS message properties
     */
    Map<String, Object> getJmsMsgPropMap();
}
