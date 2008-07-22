package com.l7tech.console.tree.policy;


import com.l7tech.console.action.InverseHttpFormPostPropertiesAction;
import com.l7tech.policy.assertion.InverseHttpFormPost;

import javax.swing.*;

/**
 * Class HttpFormPostPolicyNode is a policy node that corresponds the
 * {@link com.l7tech.policy.assertion.HttpFormPost} assertion.
 */
public class InverseHttpFormPostPolicyNode extends LeafAssertionTreeNode {

    public InverseHttpFormPostPolicyNode(InverseHttpFormPost assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "MIME to HTTP Form Translation";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new InverseHttpFormPostPropertiesAction(this);
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