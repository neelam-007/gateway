/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.logic;

import java.io.Serializable;

/**
 * @author alex
 */
public abstract class Predicate implements Serializable {
    protected boolean negated;

    public Predicate() {
    }

    public boolean isNegated() {
        return negated;
    }

    public void setNegated(boolean negated) {
        this.negated = negated;
    }

    public abstract String getSimpleName();
}
