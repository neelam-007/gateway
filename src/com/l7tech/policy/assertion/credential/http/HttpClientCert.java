/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.http.HttpDigestCredentialFinder;
import com.l7tech.policy.assertion.AssertionStatus;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HttpClientCert extends HttpCredentialSourceAssertion {
    public AssertionStatus doCheckRequest(Request request, Response response) throws CredentialFinderException {
        // TODO
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return HttpDigestCredentialFinder.class;
    }
}
