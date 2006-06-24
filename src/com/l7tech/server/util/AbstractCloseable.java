/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

/**
 * Utility superclass that maintains a closed flag and ensures that only the first call to close() will result
 * in a dispatch to doClose().
 * <p/>
 * This class does not formally implement {@link com.l7tech.common.util.Closeable} because not all subclasses
 * necessarily want to make their close() method public.
 * <p/>
 * If doClose() has not been invoked by the time the finalize() method is invoked,
 * doClose() will be invoked at that time (on the Finalizer thread).
 */
public abstract class AbstractCloseable {
    private boolean closed = false;

    protected synchronized boolean isClosed() {
        return closed;
    }

    /** Sets the {@link #closed} flag and returns the old value. */
    private synchronized boolean setClosed() {
        boolean old = closed;
        closed = true;
        return old;
    }

    /**
     * Perform any actual cleanup operations required by the implementation.
     */
    protected abstract void doClose();

    /**
     * If this is the first call to this method, invokes doClose().  Otherwise, this method does nothing.
     * Subclasses can override this to widen the access to public, but should NOT perform cleanup operations
     * here as that will defeat the goal of having the cleanup operations only performed once.
     */
    protected void close() {
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
