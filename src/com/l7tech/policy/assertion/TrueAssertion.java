/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;

/**
 * An assertion that always returns a positive result.
 *
 * @author alex
 * @version $Revision$
 */
public class TrueAssertion extends Assertion {
    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        return AssertionError.NONE;
    }
}
