/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.http;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.http.HttpDigest;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientHttpDigest implements ClientAssertion {
    public ClientHttpDigest( HttpDigest data ) {
        this.data = data;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        Ssg ssg = request.getSsg();
        if (ssg.getUsername() == null || ssg.password() == null || ssg.getUsername().length() < 1) {
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.AUTH_REQUIRED;
        }
        request.setDigestAuthRequired(true);
        request.setHttpDigestUsername(ssg.getUsername());
        request.setHttpDigestPassword(ssg.password());
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected HttpDigest data;
}
