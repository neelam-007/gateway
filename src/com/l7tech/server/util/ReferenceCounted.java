/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

/**
 * Interface for things that can be unref'ed.
 */
public interface ReferenceCounted {
    /**
     * Called by a handle to indicate that the handle has been close.  The unref() method may immediately
     * dispose the referent before returning.  In spite of being public, this method should ONLY be called
     * by a handle.
     */
    void unref();
}
