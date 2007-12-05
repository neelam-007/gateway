/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.policy.variable.ExpandVariables;

/**
 * @author Franco
 */
public class ResponseXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public ResponseXpathPolicyTreeNode(ResponseXpathAssertion assertion) {
        super( assertion );
        _assertion = assertion;
    }

    public String getBaseName() {
        StringBuffer sb = new StringBuffer();
        final String variableName = _assertion.getXmlMsgSrc();
        if (variableName == null) {
            sb.append("Response");
        } else {
            sb.append(ExpandVariables.SYNTAX_PREFIX);
            sb.append(variableName);
            sb.append(ExpandVariables.SYNTAX_SUFFIX);
        }
        sb.append(" must match XPath ");
        if (_assertion.getXpathExpression() == null) {
            sb.append("[XPath expression not set]");
        } else {
            sb.append(_assertion.getXpathExpression().getExpression());
        }
        return sb.toString();
    }


    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    private ResponseXpathAssertion _assertion;
}
