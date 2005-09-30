package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSizeLimit;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 29, 2005
 * Time: 3:43:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class RequestSizeLimitPaletteNode extends AbstractTreeNode {
    public RequestSizeLimitPaletteNode() {
        super(null);
    }

    protected void loadChildren() {
    }

    public String getName() {
        return "Request Size Limit";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/MessageLength-16x16.gif";
    }

    public Assertion asAssertion() {
        return new RequestSizeLimit();
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return super.getActions();
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return true;
    }
}
