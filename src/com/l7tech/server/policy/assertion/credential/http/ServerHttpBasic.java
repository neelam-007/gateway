/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.credential.http.HttpBasicCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpBasic extends ServerHttpCredentialSource implements ServerAssertion {
    public ServerHttpBasic( HttpBasic data ) {
        super( data );
        _data = data;
    }

    public Class getCredentialFinderClass() {
        return HttpBasicCredentialFinder.class;
    }

    protected String scheme() {
        return "Basic";
    }

    protected AssertionStatus doCheckCredentials(Request request, Response response) {
        return AssertionStatus.NONE;
    }

    protected HttpBasic _data;
}
