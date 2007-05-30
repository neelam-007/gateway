/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import java.util.Map;

/**
 * Information about a message that was received or will be sent via JMS.
 */
public interface JmsKnob extends MessageKnob, HasSoapAction {
    boolean isBytesMessage();

    /**
     * @return a map of JMS message properties
     */
    Map<String, Object> getJmsMsgPropMap();
}
