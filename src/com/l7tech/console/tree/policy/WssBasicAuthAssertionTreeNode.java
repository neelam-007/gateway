package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.credential.wss.WssBasic;

/**
 * Class HttpBasicAuthAssertionTreeNode is a tree node that correspinds
 * to the <code>HttpBasic</code> asseriton.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WssBasicAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public WssBasicAuthAssertionTreeNode(WssBasic assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require WS token - BASIC authentication";
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