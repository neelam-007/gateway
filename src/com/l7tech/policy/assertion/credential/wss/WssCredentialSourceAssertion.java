/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class WssCredentialSourceAssertion extends CredentialSourceAssertion {
    /**
     * ClientProxy client-side processing of the given request.
     * @param requst    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }
}
