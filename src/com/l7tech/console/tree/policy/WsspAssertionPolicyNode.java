package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.WsspAssertion;

/**
 * Policy node for WSSP assertion.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsspAssertionPolicyNode extends LeafAssertionTreeNode {

    //- PUBLIC

    public WsspAssertionPolicyNode(WsspAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    public WsspAssertion getAssertion() {
        return assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "WS-SecurityPolicy Compliance.";
    }

    /**
     * Test if the node can be deleted.
     *
     * @return always true
     */
    public boolean canDelete() {
        return true;
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }

    //- PRIVATE

    private WsspAssertion assertion;
}
