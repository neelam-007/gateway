/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.util;

import org.apache.commons.httpclient.HttpClient;

/**
 * Provides a single HttpClient per thread.
 * User: mike
 * Date: Jun 26, 2003
 * Time: 10:04:37 AM
 */
public class ThreadLocalHttpClient {
    private ThreadLocalHttpClient() {}

    private static ThreadLocal httpClient = new ThreadLocal() {
        protected synchronized Object initialValue() {
            HttpClient client =  new HttpClient();
            return client;
        }
    };

    /**
     * Get an HttpClient for the exclusive use of the current thread.
     * Of course callers must still be careful not to overlap calls needing different State settings (ie Basic auth)
     * @return A thread-local HttpClient
     */
    public static HttpClient getHttpClient() {
        return (HttpClient)httpClient.get();
    }
}
