/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.RequestXpathAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public RequestXpathPolicyTreeNode( RequestXpathAssertion assertion ) {
        super( assertion );
        _assertion = assertion;
    }

    public String getBaseName(final boolean decorate) {
        final String assertionName = "Evaluate Request XPath";
        if(!decorate) return assertionName;
        
        StringBuffer sb = new StringBuffer(assertionName+ " against ");
        if (_assertion.getXpathExpression() == null) {
            sb.append("[XPath expression not set]");
        } else {
            sb.append(_assertion.getXpathExpression().getExpression());
        }
        return sb.toString();
    }

    private RequestXpathAssertion _assertion;
}
