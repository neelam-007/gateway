package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.credential.http.HttpDigest;

/**
 * Class HttpDigestAuthAssertionTreeNode is a tree node that corresponds
 * to the <code>HttpDigest</code> asseriton.
 * <p/>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class HttpDigestAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpDigestAuthAssertionTreeNode(HttpDigest assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require HTTP DIGEST authentication";
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