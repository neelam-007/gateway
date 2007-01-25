/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.HtmlFormDataAssertionPropertiesAction;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A leaf node in a policy tree that represents an HTML Form Data Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataAssertionPolicyNode extends LeafAssertionTreeNode {
    public HtmlFormDataAssertionPolicyNode(HtmlFormDataAssertion assertion) {
        super(assertion);
    }

    public boolean canDelete() {
        return true;
    }

    public String getName() {
        return "Validate data fields in an HTML Form submisssion";
    }

    public Action[] getActions() {
        final List<Action> list = new ArrayList<Action>();
        list.add(new HtmlFormDataAssertionPropertiesAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[]{});
    }

    public Action getPreferredAction() {
        return new HtmlFormDataAssertionPropertiesAction(this);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/check16.gif";
    }

    protected void loadChildren() {
    }
}
