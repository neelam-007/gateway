/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

/**
 * Holds current Ssg in thread-local storage.
 *
 * User: mike
 * Date: Sep 15, 2003
 * Time: 5:03:33 PM
 */
public class CurrentRequest {
    private static ThreadLocal currentRequest = new ThreadLocal();
    private static ThreadLocal currentPeerSsg = new ThreadLocal();

    /** Set the SSG in whose context we are currently working. */
    public static void setCurrentSsg(Ssg ssg) {
        currentRequest.set(ssg);
    }

    /** @return the SSG in whose context we are currently working. */
    public static Ssg getCurrentSsg() {
        return (Ssg) currentRequest.get();
    }

    /** Set the SSG to which we are about to make an SSL connection. */
    public static void setPeerSsg(Ssg ssg) {
        currentPeerSsg.set(ssg);
    }

    /** Get the SSG to which we were about to make an SSL connection. */
    public static Ssg getPeerSsg() {
        return (Ssg)currentPeerSsg.get();
    }

    /** Clear the current SSG context. */
    public static void clearCurrentRequest() {
        currentRequest.set(null);
        currentPeerSsg.set(null);
    }
}

