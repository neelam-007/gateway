/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

/**
 * This class isn't actually used except to fill a hole in the inheritance hierarchy due to the bizarre way that
 * ServerSslAssertion delegates to ServerHttpClientCert.  Don't ask.
 *  
 * @author alex
 */
public class HttpClientCert extends HttpCredentialSourceAssertion { }
