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
    public RoutingAssertion(String protectedServiceUrl) {
        this();
        this.protectedServiceUrl = protectedServiceUrl;
    }

    /** Default constructor, for Hibernate only, don't call! */
    public RoutingAssertion() {
        super();
    }

    public String getProtectedServiceUrl() {
        return protectedServiceUrl;
    }

    public void setProtectedServiceUrl( String protectedServiceUrl ) {
        this.protectedServiceUrl = protectedServiceUrl;
    }

    /**
     * Forwards the request along to a ProtectedService at the configured URL.
     * @param request The request to be forwarded.
     * @param response The response that was received from the ProtectedService.
     * @return an AssertionError indicating the success or failure of the request.
     * @throws PolicyAssertionException if some error preventing the execution of the PolicyAssertion has occurred.
     */
    public AssertionError checkRequest(Request request, Response response) throws PolicyAssertionException {
        // TODO
        return AssertionError.NOT_YET_IMPLEMENTED;
    }

    protected String protectedServiceUrl;
}
