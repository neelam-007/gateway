/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;

/**
 * Superclass for TypeMappings that know how to serialize policy assertions into a policy XML document.
 */
class AssertionMapping extends BeanTypeMapping {
    Assertion source;

    AssertionMapping(Assertion a, String externalName) {
        super(a.getClass(), externalName);
        this.source = a;
    }

    public AssertionMapping(Assertion a, String externalName, String nsUri, String nsPrefix) {
        super(a.getClass(), externalName, nsUri, nsPrefix);
        this.source = a;
    }
}
