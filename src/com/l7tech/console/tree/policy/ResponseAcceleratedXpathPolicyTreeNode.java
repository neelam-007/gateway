/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.ResponseAcceleratedXpathAssertion;

/**
 * @author Franco
 */
public class ResponseAcceleratedXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public ResponseAcceleratedXpathPolicyTreeNode(ResponseAcceleratedXpathAssertion assertion) {
        super( assertion );
        _assertion = assertion;
    }

    public String getBaseName() {
        if (_assertion.getXpathExpression() == null) return "Response must match XPath " + "[XPath expression not set] (Hardware Accelerated)";
       return "Response must match XPath " + _assertion.getXpathExpression().getExpression() + "(Hardware Accelerated)";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    private ResponseAcceleratedXpathAssertion _assertion;
}
