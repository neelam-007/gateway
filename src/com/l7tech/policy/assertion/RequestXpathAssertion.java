/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestXpathAssertion extends Assertion {
    public RequestXpathAssertion() {
        super();
    }

    public RequestXpathAssertion( CompositeAssertion parent ) {
        super( parent );
    }

    public String getPattern() {
        return _pattern;
    }

    public void setPattern(String pattern) {
        _pattern = pattern;
    }

    public Map getNamespaceMap() {
        return _namespaceMap;
    }

    public void setNamespaceMap(Map namespaceMap) {
        _namespaceMap = namespaceMap;
    }

    private String _pattern;
    private Map _namespaceMap;
}
