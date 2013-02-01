/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    MessageFacet(@NotNull Message message, @Nullable MessageFacet delegate) {
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

    /**
     * Visit all subsequent message facets in order (starting from the this facet) and accumulate some return value.
     * <p/>
     * This will first visit this facet, calling the visitor with the initialValue, and collect the return value.
     * If there is a delegate, we will recursive invoke visitFacets on the delegate, passing in the return value
     * from visiting this facet, and using its return value as the new return value.
     *
     * @param visitor  a visitor to invoke on this facet and all subsequent facets.  Required.
     * @param initialValue the initial value to pass as the second argument to the first invocation of the visitor.  Subsequent
     *                     visits will pass in the return value from visiting the previous node.  May be null.
     * @param <R> the type of the accumulated return value.
     * @return the return value from visiting the final facet in the chain.  May be null iff. the visitor may return null.
     */
    <R> R visitFacets(@NotNull Functions.Binary<R, MessageFacet, R> visitor, @Nullable R initialValue) {
        R ret = visitor.call(this, initialValue);
        if (delegate != null)
            ret = delegate.visitFacets(visitor, ret);
        return ret;
    }

    public void close() {
        if (delegate != null)
            delegate.close();
    }
}
