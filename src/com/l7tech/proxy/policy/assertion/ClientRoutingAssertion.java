/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientRoutingAssertion extends ClientAssertion {
    public ClientRoutingAssertion( RoutingAssertion data ) {
        this.data = data;
    }

    /** Client-side doesn't know or care about server-side routing. */
    public AssertionStatus decorateRequest(PendingRequest request)  {
        return AssertionStatus.NOT_APPLICABLE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response)  {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected RoutingAssertion data;
}
