package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.TrueAssertion;

/**
 * Class <code>AnonymousAssertionTreeNode</code> is a tree node that
 * represents the anonymous access. It is modelled as <code>TrueAssertion</code>
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class AnonymousAssertionTreeNode extends LeafAssertionTreeNode {

    public AnonymousAssertionTreeNode(TrueAssertion node) {
        super(node);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Anonymous access";
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/anonymous.gif";
    }
}