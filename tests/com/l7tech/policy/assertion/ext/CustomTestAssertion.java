/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

/**
 * @author emil
 * @version Feb 13, 2004
 */
public class CustomTestAssertion extends Assertion {
    public CustomTestAssertion() {
    }

    public String getProtectedResource() {
        return protectedResource;
    }

    public void setProtectedResource(String protectedResource) {
        this.protectedResource = protectedResource;
    }

    private String protectedResource;
}