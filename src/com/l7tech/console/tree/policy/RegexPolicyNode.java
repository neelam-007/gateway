package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Regex;

/**
 * Class RegexPolicyNode is a policy node that corresponds the
 * <code>Regex</code> assertion.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RegexPolicyNode extends LeafAssertionTreeNode {

    public RegexPolicyNode(Regex assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Reqular Expression";
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
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/regex16.gif";
    }
}