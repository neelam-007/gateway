/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;

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

    public void validate( PolicyValidatorResult result ) {
        if ( _pattern == null ) {
            result.addError( new PolicyValidatorResult.Error( this, "XPath pattern is missing", null ) );
            return;
        } else {
            try {
                DOMXPath temp = new DOMXPath(_pattern);
            } catch (JaxenException e) {
                result.addError( new PolicyValidatorResult.Error( this, "XPath pattern is not valid", e ) );
                return;
            }
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        if (_namespaceMap != null)
            sb.append(" namespacesmap=" + _namespaceMap);
        if (_pattern != null)
            sb.append(" pattern=" + _pattern);
        return sb.toString();
    }

    private String _pattern;
    private Map _namespaceMap;
}
