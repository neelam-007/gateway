/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Closeable;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public interface ServerAssertion<AT extends Assertion> extends Closeable {
    /**
     * SSG Server-side processing of the given request.
     * @param context the PolicyEnforcementContext.  Never null.
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws PolicyAssertionException something is wrong in the policy dont throw this if there is an issue with the request or the response
     * @throws java.io.IOException if there is a problem reading a request or response
     * @throws AssertionStatusException as an alternate mechanism to return an assertion status other than AssertionStatus.NONE.
     */
    AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException;

    /**
     * @return the assertion associated with this server assertion
     */
    AT getAssertion();
}
