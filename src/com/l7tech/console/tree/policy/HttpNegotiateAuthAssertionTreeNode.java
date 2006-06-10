package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.credential.http.HttpNegotiate;

/**
 * Tree node for Windows Integrated authentication (Negotiate)
 *
 * @author $Author$
 * @version $Revision$
 */
public class HttpNegotiateAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpNegotiateAuthAssertionTreeNode(HttpNegotiate assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require Windows Integrated Authentication";
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
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
        return "com/l7tech/console/resources/authentication.gif";
    }
}
