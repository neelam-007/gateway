/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.ResponseXpathAssertion;

/**
 * @author Franco
 */
public class ResponseXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public ResponseXpathPolicyTreeNode(ResponseXpathAssertion assertion) {
        super( assertion );
        _assertion = assertion;
    }

    public String getBaseName() {
        StringBuffer sb = new StringBuffer("Response must match XPath ");
        if (_assertion.getXpathExpression() == null) {
            sb.append("[XPath expression not set]");
        } else {
            sb.append(_assertion.getXpathExpression().getExpression());
            String vp = _assertion.getVariablePrefix();
            if (vp != null && vp.length() > 0) {
                sb.append(" (setting variables starting with '");
                sb.append(vp);
                sb.append("')");
            }
        }
        return sb.toString();
    }


    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    private ResponseXpathAssertion _assertion;
}
