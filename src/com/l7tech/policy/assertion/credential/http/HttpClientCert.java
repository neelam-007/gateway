/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpClientCert extends HttpCredentialSourceAssertion {
    public String scheme() {
        return SCHEME;
    }

    public static final String SCHEME = "ClientCert";
}
