/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * Gathers state for a single policy branch (ie, an All assertion in a normalized policy).
 * An top-level All assertion in a normalized policy may contain zero or one security binding,
 * zero or one Trust10, zero or one Wss10, zero or one SignedParts, and zero or one EncryptedParts.
 */
class TopLevelAll extends WsspVisitor {
    private Set algorithmSuites = new HashSet();
    private boolean timestamp = false;

    protected TopLevelAll(WsspVisitor parent) {
        super(parent);
    }

    protected Map getConverterMap() {
        return getParent().getConverterMap();
    }

    protected boolean maybeAddPropertyQnameValue(String propName, QName propValue) {
        if ("AlgorithmSuite".equals(propName)) {
            algorithmSuites.add(propValue);
            return true;
        }
        return super.maybeAddPropertyQnameValue(propName, propValue);
    }

    protected boolean maybeSetSimpleProperty(String propName, boolean propValue) {
        if ("Timestamp".equals(propName)) {
            timestamp = propValue;
            return true;
        }
        return super.maybeSetSimpleProperty(propName, propValue);
    }
}
