/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;

/**
 * @author mike
 */
class AssertionMapping extends BeanTypeMapping {
    Assertion source;

    AssertionMapping(Assertion a, String externalName) {
        super(a.getClass(), externalName);
        this.source = a;
    }
}
