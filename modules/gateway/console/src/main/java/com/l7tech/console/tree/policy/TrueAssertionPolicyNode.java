package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.TrueAssertion;

/**
 * Class TrueAssertionPolicyNode is a policy node that corresponds the
 * <code>TrueAssertion</code>.
 */
public class TrueAssertionPolicyNode extends LeafAssertionTreeNode {
    public TrueAssertionPolicyNode(TrueAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName(final boolean decorate) {
        return "Continue processing";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/check16.gif";
    }
}