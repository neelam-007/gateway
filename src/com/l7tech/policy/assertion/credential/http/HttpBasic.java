/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.*;
import com.l7tech.credential.http.HttpBasicCredentialFinder;
import com.l7tech.policy.assertion.AssertionError;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpBasic extends HttpCredentialSourceAssertion {
    public AssertionError doCheckRequest(Request request, Response response) throws CredentialFinderException {
        // TODO
        return AssertionError.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return HttpBasicCredentialFinder.class;
    }

    /**
     * Set up HTTP Basic auth on the PendingRequest.
     * @param request    The request to decorate.
     * @return AssertionError.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionError decorateRequest(PendingRequest request) throws PolicyAssertionException {
        String username = request.getSsg().getUsername();
        String password = request.getSsg().getPassword();
        if (username == null || password == null || username.length() < 1)
            return AssertionError.FAILED;
        request.setBasicAuthRequired(true);
        request.setHttpBasicUsername(username);
        request.setHttpBasicPassword(password);
        return AssertionError.NONE;
    }
}
