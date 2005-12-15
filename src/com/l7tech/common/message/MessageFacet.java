/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

/**
 * Represents a feature or aspect of a Message.  For example, its MIME part, or XML tree, or HTTP headers.
 */
abstract class MessageFacet {
    private final MessageFacet delegate;
    private final Message message;

    /**
     * @param message the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    MessageFacet(Message message, MessageFacet delegate) {
        if (message == null) throw new NullPointerException();
        this.message = message;
        this.delegate = delegate;
    }

    protected final Message getMessage() {
        return message;
    }

    MessageKnob getKnob(Class c) {
        if (delegate != null)
            return delegate.getKnob(c);
        return null; // END OF LINE
    }

    public void close() {
        if (delegate != null)
            delegate.close();
    }
}
