package com.l7tech.console.tree.policy;


import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.Iterator;
import java.util.Set;

/**
 * Class IdentityViewTreeNode.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IdentityViewTreeNode extends AssertionTreeNode {
    private IdentityPath path;

    /**
     * Construct the new <code>IdentityViewTreeNode</code> for
     * the identity path.
     * <p>
     * @param path the identity patht
     */
    public IdentityViewTreeNode(IdentityPath path, Assertion root) {
        super(root);
        this.path = path;
        if (path == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        CompositeAssertion assertion = (CompositeAssertion)asAssertion();
        children = null;
        insert(AssertionTreeNodeFactory.asTreeNode(assertion), 0);
    }

    protected boolean pathContains(Assertion a) {
        Set paths = getIdentityPath().getPaths();
        for (Iterator i = paths.iterator(); i.hasNext();) {
            AssertionPath ap = (AssertionPath)i.next();
            Assertion[] assertions = ap.getPath();
            for (int j = assertions.length - 1; j >= 0; j--) {
                Assertion ass = assertions[j];
                if (ass.equals(a)) return true;
            }
        }
        return false;
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
        return "com/l7tech/console/resources/user16.png";

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
        return getIdentityPath().getPrincipal().getName();
    }

    public IdentityPath getIdentityPath() {
        return path;
    }
}