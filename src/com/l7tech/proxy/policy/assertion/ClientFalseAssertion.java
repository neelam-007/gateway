/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientFalseAssertion extends ClientAssertion {
    public ClientFalseAssertion( FalseAssertion ass ) {
        // meaningless
    }

    public ClientFalseAssertion() {
        // meaningless
    }

    public AssertionStatus decorateRequest(PendingRequest request) {
        return AssertionStatus.FALSIFIED;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "Automatic failure";
    }

    public String iconResource(boolean open) {
        return null;
    }
}
