/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.io.Serializable;

/**
 * The custom assertion wraps a user defined bean instance that is
 * essentialy user defined property.
 * The <code>customAssertionBean,/code> property is required to
 * be serializable and must offer Java Bean style get/set operations.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CustomAssertion extends Assertion {
    protected CompositeAssertion parent;

    public CustomAssertion() {
        this.parent = null;
    }

    public CustomAssertion(CompositeAssertion parent) {
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
    public Serializable getCustomAssertionBean() {
        return customAssertionBean;
    }

    /**
     * Set the custome assertion bean
     *
     * @param customAssertionBean the new custome assertino bean
     */
    public void setCustomAssertionBean(Serializable customAssertionBean) {
        this.customAssertionBean = customAssertionBean;
    }

    private Serializable customAssertionBean;
}

