/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.credential.http.HttpDigestCredentialFinder;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpDigest extends ServerHttpCredentialSource implements ServerAssertion {
    public ServerHttpDigest( HttpDigest data ) {
        super( data );
        _data = data;
    }

    public String scheme() {
        return "Digest";
    }

    protected AssertionStatus doCheckCredentials(Request request, Response response) {
        // TODO
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    protected Class getCredentialFinderClass() {
        return HttpDigestCredentialFinder.class;
    }

    protected HttpDigest _data;
}
