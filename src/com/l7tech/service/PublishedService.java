/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;

import java.util.Set;

/**
 * @author alex
 */
public class PublishedService extends Service {
    public PublishedService( String name, Set operations, Assertion rootAssertion ) {
        super(name, operations);
        _rootAssertion = rootAssertion;
    }

    protected Assertion _rootAssertion;
}
