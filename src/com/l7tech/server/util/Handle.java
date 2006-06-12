/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import com.l7tech.common.util.Closeable;

/**
 * Represents a handle to a referenceable object.  Subclasses will likely delegate to the target.
 */
public class Handle<T extends ReferenceCounted> implements Closeable {
    private final T target;
    private volatile boolean closed = false;

    protected Handle(T cs) {
        this.target = cs;
    }

    /**
     * Report that noone will ever again try to use this handle. This may cause the underlying object to
     * be immediately unloaded/destroyed/etc, without waiting for the finalizer to get around to it,
     * if this was the last handle using it.
     */
    public void close() {
        if (closed) return;
        closed = true;
        target.unref();
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    protected T getTarget() {
        return target;
    }
}
