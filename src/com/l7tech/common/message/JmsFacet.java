/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

/**
 * @author mike
 */
public class JmsFacet extends MessageFacet {
    private final JmsKnob jmsKnob;

    /**
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    public JmsFacet(com.l7tech.common.message.Message message, MessageFacet delegate, JmsKnob jmsKnob) {
        super(message, delegate);
        this.jmsKnob = jmsKnob;
    }

    public MessageKnob getKnob(Class c) {
        if (c == JmsKnob.class) {
            return jmsKnob;
        }
        return super.getKnob(c);
    }
}
