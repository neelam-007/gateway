/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.wss.WssClientCertCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;


/**
 * @author alex
 * @version $Revision$
 */
public class WssClientCert extends WssCredentialSourceAssertion {
    public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // FIXME: Implement
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return WssClientCertCredentialFinder.class;
    }

    /**
     * ClientProxy client-side processing of the given request.
     * @param requst    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

}
