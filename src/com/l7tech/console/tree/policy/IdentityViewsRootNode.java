package com.l7tech.console.tree.policy;


import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.assertion.Assertion;

import java.util.Iterator;
import java.util.Set;

/**
 * Class IdentityViewTreeNode. This is an assertion tree node
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IdentityViewsRootNode extends AssertionTreeNode {
    private Set paths;

    /**
     * Construct the new <code>IdentityViewTreeNode</code> for
     * the identity path.
     * <p>
     * @param identityPaths the identity path collections
     */
    public IdentityViewsRootNode(Set identityPaths, Assertion root) {
        super(root);
        this.paths = identityPaths;
        if (identityPaths == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {

        int index = 0;
        for (Iterator i = getIdentityPaths().iterator(); i.hasNext();) {
            insert(new IdentityViewTreeNode((IdentityPath)i.next(), asAssertion()), index++);
        }
    }

    /**
     * By default, this node accepts leaf nodes.
     *
     * @param node the node to accept
     * @return true if sending node is leaf
     */
    public boolean accept(AbstractTreeNode node) {
        return false;
    }

    /**
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
           if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
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
     * @return the node name that is displayed
     */
    public String getName() {
        return "Identities folder";
    }


    private Set getIdentityPaths() {
        return paths;
    }
}