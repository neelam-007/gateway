/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

/**
 * Holds current SSL peer in thread-local storage.
 */
public class CurrentSslPeer {
    private static ThreadLocal currentSslPeer = new ThreadLocal();

    /** Set the SSG to which we are about to make an SSL connection. */
    public static void set(SslPeer ssg) {
        currentSslPeer.set(ssg);
    }

    /** Get the SSG to which we were about to make an SSL connection. */
    public static SslPeer get() {
        return (SslPeer)currentSslPeer.get();
    }

    /** Clear the current Ssl Peer context. */
    public static void clear() {
        currentSslPeer.set(null);
    }
}

