/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientTrueAssertion extends ClientAssertion {
    public ClientTrueAssertion( TrueAssertion ass ) {
        // meaningless
    }

    public ClientTrueAssertion() {
        // meaningless
    }

    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }
}
