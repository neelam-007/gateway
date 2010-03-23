/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import java.util.concurrent.Callable;

/**
 * Holds current SSL peer in thread-local storage.
 */
public class CurrentSslPeer {
    private static ThreadLocal<SslPeer> currentSslPeer = new ThreadLocal<SslPeer>();

    /**
     * Set the SSG to which we are about to make an SSL connection.
     * @param ssg the new SSL peer to set for this thread, or null.
     **/
    public static void set(SslPeer ssg) {
        currentSslPeer.set(ssg);
    }

    /**
     * Get the SSG to which we were about to make an SSL connection.
     * @return the current SSL peer for this thread, or null.
     **/
    public static SslPeer get() {
        return currentSslPeer.get();
    }

    /** Clear the current Ssl Peer context. */
    public static void clear() {
        currentSslPeer.set(null);
    }

    /**
     * Run some code with the current SSL peer set to the specified value, restoring any old value afterward.
     *
     * @param sp  SslPeer to have in effect during callable, or null to clear it.
     * @param callable  the callable to run with the specified thread-local SslPeer in effect.  Required.
     * @return the return value of the callable
     * @throws Exception if the callable throws an exception
     */
    public static <RT> RT doWithSslPeer(SslPeer sp, Callable<RT> callable) throws Exception {
        final SslPeer old = get();
        try {
            set(sp);
            return callable.call();
        } finally {
            set(old);
        }
    }
}

