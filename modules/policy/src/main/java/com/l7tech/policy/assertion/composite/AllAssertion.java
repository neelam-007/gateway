/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.composite;

import java.util.List;

/**
 * Evaluate children until none left or one fails; return last result evaluated.
 *
 * Semantically equivalent to a short-circuited AND.
 *
 * @author alex
 * @version $Revision$
 */
public class AllAssertion extends CompositeAssertion {
    public AllAssertion() {
    }

    public AllAssertion( List children ) {
        super( children );
    }
}
