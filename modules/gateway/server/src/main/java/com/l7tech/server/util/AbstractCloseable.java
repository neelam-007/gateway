/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

/**
 * Utility superclass that maintains a closed flag and ensures that only the first call to close() will result
 * in a dispatch to doClose().
 * <p/>
 * This class does not formally implement {@link java.io.Closeable} because not all subclasses
 * necessarily want to make their close() method public.
 * <p/>
 * This superclass does not provide a finalize method.  The presence of a finalize method, especially on an
 * object whose lifecycle is within a single request, can cause objects to pile up on the finalizer queue
 * (to the point of filling up memory and crashing, in some circumstances -- see Bug #2953).  Implementors
 * can of course provide their own finalize method if it is really, truly, absolutely necessary (to free
 * hardware resources, for example).
 * <p/>
 * Note that you do NOT need a finalize method just because you keep a reference to an object that has its own.
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
}
