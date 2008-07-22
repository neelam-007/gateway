/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import java.util.List;

/**
 * Evaluate children until none left or one succeeds; returns last result evaluated.
 *
 * Semantically equivalent to a short-circuited OR.
 *
 * @author alex
 * @version $Revision$
 */
public class OneOrMoreAssertion extends CompositeAssertion {
    public OneOrMoreAssertion() {
    }

    public OneOrMoreAssertion(List children) {
        super(children);
    }
}
