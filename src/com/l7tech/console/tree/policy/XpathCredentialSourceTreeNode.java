/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditXpathCredentialSourceAction;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;

import javax.swing.*;

public class XpathCredentialSourceTreeNode extends LeafAssertionTreeNode<XpathCredentialSource> {
    private EditXpathCredentialSourceAction editAction = new EditXpathCredentialSourceAction(this);

    public XpathCredentialSourceTreeNode(XpathCredentialSource assertion) {
        super(assertion);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    public Action getPreferredAction() {
        return editAction;
    }

    public String getName() {
        return "XPath credentials: login = '" + assertion.getXpathExpression().getExpression() +
                       "', password = '" + assertion.getPasswordExpression().getExpression() + "'";
    }
}
