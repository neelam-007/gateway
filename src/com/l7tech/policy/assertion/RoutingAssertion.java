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
    public RoutingAssertion( ProtectedService service ) {
        super();
        _protectedService = service;
    }

    /** Default constructor, for Hibernate only, don't call! */
    public RoutingAssertion() {
        super();
    }

    public ProtectedService getProtectedService() {
        return _protectedService;
    }

    public void setProtectedService(ProtectedService protectedService) {
        _protectedService = protectedService;
    }

    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        return AssertionError.NONE;
    }

    protected ProtectedService _protectedService;
}
