/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.http.HttpDigestCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpDigest extends HttpCredentialSourceAssertion {
    public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // FIXME: Implement
        return super.checkCredentials( request, response );
    }

    protected String scheme() {
        return "Digest";
    }

    public Class getCredentialFinderClass() {
        return HttpDigestCredentialFinder.class;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        Ssg ssg = request.getSsg();
        if (ssg.getUsername() == null || ssg.getPassword() == null || ssg.getUsername().length() < 1) {
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.AUTH_REQUIRED;
        }
        request.setDigestAuthRequired(true);
        request.setHttpDigestUsername(ssg.getUsername());
        request.setHttpDigestPassword(ssg.getPassword());
        return AssertionStatus.NONE;
    }
}
