/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertion;

/**
 * The custom assertion holder wraps a user defined bean instance that
 * represents user defined set of properties that describe the assertion.
 * The <code>customAssertionBean,/code> property is required to
 * be serializable and must offer Java Bean style get/set operations.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CustomAssertionHolder extends Assertion {
    protected CompositeAssertion parent;

    public CustomAssertionHolder() {
        this.parent = null;
    }

    public CustomAssertionHolder(CompositeAssertion parent) {
        this.parent = parent;
    }

    public CompositeAssertion getParent() {
        return parent;
    }

    public void setParent(CompositeAssertion parent) {
        this.parent = parent;
    }

    /**
     * @return the custome assertion bean
     */
    public CustomAssertion getCustomAssertion() {
        return customAssertion;
    }

    /**
     * Set the custome assertion bean
     *
     * @param ca the new custome assertino bean
     */
    public void setCustomAssertion(CustomAssertion ca) {
        this.customAssertion = ca;
    }

    private CustomAssertion customAssertion;
}

