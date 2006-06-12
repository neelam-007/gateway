/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility superclass containing a working implementation of {@link ReferenceCounted}.
 */
public abstract class AbstractReferenceCounted<HT extends Handle> implements ReferenceCounted {
    private final AtomicInteger refcount = new AtomicInteger(0);
    private boolean closed = false;

    protected abstract HT createHandle();

    public HT ref() {
        refcount.incrementAndGet();
        return createHandle();
    }

    public void unref() {
        int nval = refcount.decrementAndGet();
        if (nval <= 0) {
            close();
        }
    }

    protected synchronized boolean isClosed() {
        return closed;
    }

    /** Sets the {@link #closed} flag and returns the old value. */
    private synchronized boolean setClosed() {
        boolean old = closed;
        closed = true;
        return old;
    }

    protected abstract void doClose();

    protected final void close() {
        if (setClosed()) return;
        doClose();
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }
}
