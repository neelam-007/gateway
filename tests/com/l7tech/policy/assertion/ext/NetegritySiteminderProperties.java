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
public class NetegritySiteminderProperties implements CustomAssertion {
    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    private String resource;

    public String getName() {
        return "Netegrity Siteminder";
    }
}