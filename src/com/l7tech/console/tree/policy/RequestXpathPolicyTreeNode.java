/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.console.action.XpathBasedAssertionPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestXpathPolicyTreeNode extends XpathBasedAssertionTreeNode {
    public RequestXpathPolicyTreeNode( RequestXpathAssertion assertion ) {
        super( assertion );
        _assertion = assertion;
    }

    public String getBaseName() {
        if (_assertion.getXpathExpression() == null) return "XPath expression not set";
        return "Request must match XPath " + _assertion.getXpathExpression().getExpression();
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    private RequestXpathAssertion _assertion;
}
