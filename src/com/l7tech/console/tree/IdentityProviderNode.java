package com.l7tech.console.tree;

import com.l7tech.console.action.*;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * The class represents an tree node gui node element that
 * corresponds to the Provider entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class IdentityProviderNode extends EntityHeaderNode {
    /**
     * construct the <CODE>ProviderNode</CODE> instance for
     * a given entity.
     * The parameter entity must represent a provider, otherwise the
     * runtime IllegalArgumentException exception is thrown.
     *
     * @param e the Entry instance, must be provider
     * @throws IllegalArgumentException thrown if the entity instance is not a provider
     */
    public IdentityProviderNode(EntityHeader e) {
        super(e);
        if (e == null) {
            throw new IllegalArgumentException("entity == null");
        }
        //setAllowsChildren(true);
    }


    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(new IdentityProviderPropertiesAction(this));
        FindIdentitiesDialog.Options options = new FindIdentitiesDialog.Options();
        options.enableDeleteAction();
        options.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Object obj = getUserObject();
        if (obj instanceof EntityHeader) {
            options.setInitialProvider(((EntityHeader)obj).getOid());
        }
        list.add(new FindIdentityAction(options));
        final Action newUserAction;
        final NewGroupAction newGroupAction;
        final NewVirtualGroupAction newVirtualGroupAction;

        IdentityProviderConfig config = getProviderConfig();

        if (config.type() == IdentityProviderType.INTERNAL) {
            newUserAction = new NewInternalUserAction(this);
            newGroupAction = new NewGroupAction(this);
            newVirtualGroupAction = new NewVirtualGroupAction(this);
            newUserAction.setEnabled(true);
            newGroupAction.setEnabled(true);
            newVirtualGroupAction.setEnabled(false);
        } else if (config.type() == IdentityProviderType.FEDERATED) {
            newUserAction = new NewFederatedUserAction(this);
            newGroupAction = new NewGroupAction(this);
            newVirtualGroupAction = new NewVirtualGroupAction(this);
            newUserAction.setEnabled(true);
            newGroupAction.setEnabled(true);
            newVirtualGroupAction.setEnabled(true);
        } else {
            // the actions here is dummy as they are always disabled from the beginning
            // this is currently for LDAP user
            newUserAction = new NewInternalUserAction(this);
            newGroupAction = new NewGroupAction(this);
            newVirtualGroupAction = new NewVirtualGroupAction(this);
            newUserAction.setEnabled(false);
            newGroupAction.setEnabled(false);
            newVirtualGroupAction.setEnabled(false);
        }

        list.add(newUserAction);
        list.add(newGroupAction);
        list.add(newVirtualGroupAction);

        list.addAll(Arrays.asList(super.getActions()));
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            Action action = (Action)iterator.next();
            if (action instanceof DeleteEntityAction) {
                action.setEnabled(config.type() != IdentityProviderType.INTERNAL);
            }

        }

        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new IdentityProviderPropertiesAction(this);
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
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/CreateIdentityProvider16x16.gif";
    }

    /**
     * test whether the node can refresh its children. The provider
     * node can always refresh its children
     *
     * @return always true
     */
    public boolean canRefresh() {
        return false;
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

}
