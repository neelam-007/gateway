/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public interface ServerAssertion {
    /**
     * SSG Server-side processing of the given request.
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws PolicyAssertionException something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException;
}
