/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.service.ProtectedService;

/**
 * @author alex
 */
public class RoutingAssertion extends Assertion {
    public RoutingAssertion(String protectedServiceTarget) {
        this.protectedServiceTarget = protectedServiceTarget;
    }

    /** Default constructor, for Hibernate only, don't call! */
    public RoutingAssertion() {
        super();
    }

    public String getProtectedServiceTarget() {
        return protectedServiceTarget;
    }

    public void setProtectedServiceTarget(String protectedServiceTarget) {
        this.protectedServiceTarget = protectedServiceTarget;
    }

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        return AssertionError.NONE;
    }

    protected String protectedServiceTarget;
}
