package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.console.action.IdentityPolicyAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class AssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */

public class MemberOfGroupAssertionTreeNode extends LeafAssertionTreeNode {
    /**
     * The <code>MemberOfGroupAssertionTreeNode</code> is the composite
     * assertion node that represents the group membership.
     *
     * @param assertion the composite assertion
     */
    public MemberOfGroupAssertionTreeNode(MemberOfGroup assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/group16.png";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new IdentityPolicyAction((IdentityAssertion)getUserObject());
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Group membership " + ((MemberOfGroup)getUserObject()).getGroupName();
    }
}