/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.http.HttpClientCertCredentialFinder;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpClientCert extends ServerHttpCredentialSource implements ServerAssertion {
    public ServerHttpClientCert( HttpClientCert data ) {
        super( data );
        _data = data;
    }

    protected String scheme() {
        return "ClientCert";
    }

    protected AssertionStatus doCheckCredentials(Request request, Response response) {
        return AssertionStatus.NONE;
    }

    protected Class getCredentialFinderClass() {
        return HttpClientCertCredentialFinder.class;
    }

    protected HttpClientCert _data;
}
