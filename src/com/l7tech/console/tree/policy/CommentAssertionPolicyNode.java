/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.CommentAssertionPropertiesAction;
import com.l7tech.policy.assertion.CommentAssertion;

import javax.swing.*;

/**
 * Class CommentAssertionPolicyNode is a policy node that corresponds to
 * {@link com.l7tech.policy.assertion.CommentAssertion}.
 */
public class CommentAssertionPolicyNode extends LeafAssertionTreeNode {
    private CommentAssertion assertion;

    public CommentAssertionPolicyNode(CommentAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Comment: " + assertion.getComment();
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new CommentAssertionPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/About16.gif";
    }
}