package com.l7tech.console.tree;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import java.awt.*;
import java.security.Principal;
import java.util.ArrayList;


/**
 * The class represents a node element in the TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class IdentityNode extends AbstractTreeNode {

    public IdentityNode() {
        super(null);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * test whether the node can be deleted. Only the internal
     * nodes can be deleted.
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return false;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * Return assertions representation of the node. This returns
     * the array of selected users and groups
     *
     * @return the assertion corresponding to this node or null
     */
    public Assertion[] asAssertions() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true);
        fd.pack();
        Utilities.centerOnScreen(fd);
        Principal[] principals = fd.showDialog();
        java.util.List assertions = new ArrayList();
        for (int i = 0; i < principals.length; i++) {
            Principal principal = principals[i];
            if (principal instanceof User) {
                User u = (User)principal;
                assertions.add(new SpecificUser(u.getProviderId(), u.getLogin()));
            } else if (principal instanceof Group) {
                Group g = (Group)principal;
                assertions.add(new MemberOfGroup(g.getProviderId(), g.getName(), g.getUniqueIdentifier()));
            }
        }
        return (Assertion[])assertions.toArray(new Assertion[]{});
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return null;
    }

    public String getName() {
        return "Grant access to users or groups...";
    }

    protected void loadChildren() {
        ;
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

}
