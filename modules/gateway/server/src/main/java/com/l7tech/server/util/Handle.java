/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.util.Closeable;

/**
 * Represents a handle to a referenceable object.  Subclasses will likely delegate to the target.
 */
public class Handle<T extends ReferenceCounted> extends AbstractCloseable implements Closeable {
    private final T target;

    protected Handle(T cs) {
        this.target = cs;
    }

    /**
     * Report that noone will ever again try to use this handle. This may cause the underlying object to
     * be immediately unloaded/destroyed/etc.
     */
    public void close() {
        super.close();
    }

    protected void doClose() {
        target.unref();
    }

    /** @return the target, or null if this handle has been closed (possibly by another thread). */
    protected T getTarget() {
        if (isClosed()) return null;
        return target;
    }
}
