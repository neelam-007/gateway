/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.composite;

import java.util.List;

/**
 * Evaluate children until none left or one fails; return last result evaluated.
 *
 * Semantically equivalent to a short-circuited AND.
 */
public final class AllAssertion extends CompositeAssertion {
    public AllAssertion() {
    }

    public AllAssertion( List children ) {
        super( children );
    }

    /**
     * Check if the assertion is the root assertion.
     * @return true if this AllAssertion has no parent.
     */
    public boolean isRoot() {
        return getParent() == null;
    }
}
