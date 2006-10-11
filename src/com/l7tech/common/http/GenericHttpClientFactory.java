/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.http;

/**
 * Creates GenericHttpClient instances on demand.
 */
public interface GenericHttpClientFactory {
    /**
     * @return a new GenericHttpClient instance ready to make outgoing requests, possibly already including any
     *         SSL setup required for the current environment.
     */
    GenericHttpClient createHttpClient();
    
    GenericHttpClient createHttpClient(int hostConnections,
                                       int totalConnections,
                                       int connectTimeout,
                                       int timeout,
                                       Object identity);
}
