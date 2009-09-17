/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.proxy.message.PolicyApplicationContext;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientTrueAssertion extends ClientAssertionWithMetaSupport {
    public ClientTrueAssertion( TrueAssertion ass ) {
        super(ass);
        // meaningless
    }

    @Override
    public AssertionStatus decorateRequest(PolicyApplicationContext context)  {
        return AssertionStatus.NONE;
    }

    @Override
    public AssertionStatus unDecorateReply(PolicyApplicationContext context)  {
        // no action on response
        return AssertionStatus.NONE;
    }
}
