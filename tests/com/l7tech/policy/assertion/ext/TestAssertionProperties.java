/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.ext;


/**
 * @author emil
 * @version Feb 18, 2004
 */
public class TestAssertionProperties implements CustomAssertion {
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    private String resource;

    private String cookieName;

    public String getName() {
        return "Test Custom Assertion";
    }
}