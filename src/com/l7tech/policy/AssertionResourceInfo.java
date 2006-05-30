/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionResourceType;

/**
 * @author alex
 */
public abstract class AssertionResourceInfo implements Cloneable {
    public abstract AssertionResourceType getType();

    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
