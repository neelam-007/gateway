/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.HttpFormPostPropertiesAction;
import com.l7tech.policy.assertion.HttpFormPost;

import javax.swing.*;

/**
 * Class HttpFormPostPolicyNode is a policy node that corresponds the
 * {@link com.l7tech.policy.assertion.HttpFormPost} assertion.
 */
public class HttpFormPostPolicyNode extends LeafAssertionTreeNode {

    public HttpFormPostPolicyNode(HttpFormPost assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName(final boolean decorate) {
        return "HTTP Form to MIME Translation";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new HttpFormPostPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/network.gif";
    }
}