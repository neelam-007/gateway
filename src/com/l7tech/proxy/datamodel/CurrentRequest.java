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

    public static void setCurrentSsg(Ssg ssg) {
        currentRequest.set(ssg);
    }

    public static Ssg getCurrentSsg() {
        return (Ssg) currentRequest.get();
    }

    public static void clearCurrentRequest() {
        currentRequest.set(null);
    }
}

