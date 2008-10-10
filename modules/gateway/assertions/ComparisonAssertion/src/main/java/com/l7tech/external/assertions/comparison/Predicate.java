/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import java.io.Serializable;

/**
 * @author alex
 */
public abstract class Predicate implements Cloneable, Serializable {
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

    @Override
    public Object clone() throws CloneNotSupportedException {        
        return super.clone();
    }
}
