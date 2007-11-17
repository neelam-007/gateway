/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.HtmlFormDataAssertionPropertiesAction;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;

import javax.swing.*;

/**
 * A leaf node in a policy tree that represents an {@link HtmlFormDataAssertion}.
 */
public class HtmlFormDataAssertionPolicyNode extends LeafAssertionTreeNode {
    public HtmlFormDataAssertionPolicyNode(HtmlFormDataAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Validate data fields in an HTML Form submisssion";
    }

    public Action getPreferredAction() {
        return new HtmlFormDataAssertionPropertiesAction(this);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/check16.gif";
    }
}
