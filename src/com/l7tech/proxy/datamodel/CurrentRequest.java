/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

/**
 * Holds current PendingRequest in thread-local storage.
 *
 * User: mike
 * Date: Sep 15, 2003
 * Time: 5:03:33 PM
 */
public class CurrentRequest {
    private static ThreadLocal currentRequest = new ThreadLocal();

    public static void setCurrentRequest(PendingRequest req) {
        currentRequest.set(req);
    }

    public static PendingRequest getCurrentRequest() {
        return (PendingRequest) currentRequest.get();
    }
}

