/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.proxy.datamodel.PendingRequest;

/**
 * An assertion that always returns a negative result.
 *
 * @author alex
 * @version $Revision$
 */
public class FalseAssertion extends Assertion {
    public AssertionStatus checkRequest(Request request, Response response) throws PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }

    public AssertionStatus decorateRequest(PendingRequest requst) throws PolicyAssertionException {
        return AssertionStatus.FALSIFIED;
    }
}
