/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

/**
 * Base class for XML security assertions whose primary configurable feature is an Xpath expression.
 */
public abstract class XpathBasedAssertion extends Assertion {
    protected XpathExpression xpathExpression;

    protected XpathBasedAssertion() {
    }

    protected XpathBasedAssertion(CompositeAssertion parent) {
        super(parent);
    }

    public XpathExpression getXpathExpression() {
        return xpathExpression;
    }

    public void setXpathExpression(XpathExpression xpathExpression) {
        this.xpathExpression = xpathExpression;
    }
}
