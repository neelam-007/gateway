/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientFalseAssertion implements ClientAssertion {
    public ClientFalseAssertion( FalseAssertion ass ) {
        // meaningless
    }

    public ClientFalseAssertion() {
        // meaningless
    }

    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }
}
