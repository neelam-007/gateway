/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EmailAlertAssertionPropertiesAction;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

import javax.swing.*;

/**
 * Policy tree node for {@link EmailAlertAssertion}.
 */
public class EmailAlertAssertionTreeNode extends LeafAssertionTreeNode<EmailAlertAssertion> {
    public EmailAlertAssertionTreeNode(EmailAlertAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Send Email Alert";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new EmailAlertAssertionPropertiesAction(this);
    }
}
