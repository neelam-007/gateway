/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientOneOrMoreAssertion extends ClientCompositeAssertion {
    private static final Category log = Category.getInstance(ClientOneOrMoreAssertion.class);

    public ClientOneOrMoreAssertion( OneOrMoreAssertion data ) {
        super( data );
        this.data = data;
    }

    /**
     * Modify the provided PendingRequest to conform to this policy assertion.
     * For OneOrMoreAssertion, we'll run children only until one succeeds (or we run out of children).
     * @param req
     * @return the AssertionStatus.NONE if at least one child succeeded; the rightmost-child error otherwise.
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest req) throws PolicyAssertionException {
        data.mustHaveChildren();
        AssertionStatus result = AssertionStatus.FAILED;
        for ( int i = 0; i < children.length; i++ ) {
            ClientAssertion assertion = children[i];
            result = assertion.decorateRequest(req);
            if (result == AssertionStatus.NONE)
                return result;
        }
        return result;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected OneOrMoreAssertion data;
}
