/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp.pre32;

import com.l7tech.policy.assertion.Assertion;

/**
 * Superclass for TypeMappings that know how to serialize policy assertions into a policy XML document.
 */
class Pre32AssertionMapping extends Pre32BeanTypeMapping {
    Assertion source;

    Pre32AssertionMapping(Assertion a, String externalName) {
        super(a.getClass(), externalName);
        this.source = a;
    }

    public Pre32AssertionMapping(Assertion a, String externalName, String nsUri, String nsPrefix) {
        super(a.getClass(), externalName, nsUri, nsPrefix);
        this.source = a;
    }
}
