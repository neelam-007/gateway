package com.l7tech.console.tree.policy;


import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import java.security.Principal;
import java.util.Iterator;
import java.util.Set;

/**
 * Class IdentityPolicyTreeNode.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class IdentityPolicyTreeNode extends AssertionTreeNode {
    private IdentityPath path;
    private final String iconResource;
    private AssertionTreeNode identityAssertionTreeNode;

    /**
     * Construct the new <code>IdentityPolicyTreeNode</code> for
     * the identity path.
     * <p/>
     *
     * @param path the identity path
     */
    public IdentityPolicyTreeNode(IdentityPath path, Assertion root) {
        super(root);
        this.path = path;
        if (path == null) {
            throw new IllegalArgumentException();
        }
        IdentityAssertion identityAssertion;
        this.setAllowsChildren(true);
        Principal principal = path.getPrincipal();
        if (principal instanceof Group) {
            Group g = (Group)principal;
            identityAssertion = new MemberOfGroup(g.getProviderId(), g.getName(), g.getUniqueIdentifier());
            iconResource = "com/l7tech/console/resources/group16.png";
        } else if (principal instanceof User) {
            User u = (User)principal;
            identityAssertion = new SpecificUser(u.getProviderId(), u.getLogin());
            iconResource = "com/l7tech/console/resources/user16.png";
        } else {
            throw new IllegalArgumentException("Unknown principal class " + principal.getClass());
        }
        identityAssertionTreeNode = AssertionTreeNodeFactory.asTreeNode(identityAssertion);
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
        CompositeAssertion assertion = (CompositeAssertion)asAssertion();
        children = null;
        LoadChildrenStrategy loader = LoadChildrenStrategy.newStrategy(this);
        loader.loadChildren(this, assertion);
//        int index = 0;
//        for (Iterator iterator = assertion.children(); iterator.hasNext();) {
//            Assertion a = (Assertion)iterator.next();
//            insert(AssertionTreeNodeFactory.asTreeNode(a), index++);
//        }
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

    protected boolean contains(AssertionPath a) {
        Set paths = getIdentityPath().getPaths();
        for (Iterator i = paths.iterator(); i.hasNext();) {
            AssertionPath ap = (AssertionPath)i.next();
            if (ap.equals(a)) return true;
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
        return iconResource;
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
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
        return identityAssertionTreeNode.getName();
    }

    public IdentityPath getIdentityPath() {
        return path;
    }
}