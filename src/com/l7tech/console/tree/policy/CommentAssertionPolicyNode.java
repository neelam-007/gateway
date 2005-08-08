package com.l7tech.console.tree.policy;


import com.l7tech.console.action.CommentAssertionPropertiesAction;
import com.l7tech.policy.assertion.CommentAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

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
     * Test if the node can be deleted.
     *
     * @return always true
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new CommentAssertionPropertiesAction(this);
    }

    protected void loadChildren() {

    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new CommentAssertionPropertiesAction(this);
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
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