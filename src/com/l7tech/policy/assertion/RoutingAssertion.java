/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;

/**
 * @author alex
 */
public class RoutingAssertion extends Assertion {
    public int checkRequest(Request request, Response response) throws PolicyAssertionExcepion {
        return 0;
    }
}
