package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.console.action.IdentityProviderPropertiesAction;

import javax.swing.*;
import java.util.*;

/**
 * The class represents an tree node gui node element that
 * corresponds to the Provider entity.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class ProviderNode extends EntityHeaderNode {
    /**
     * construct the <CODE>ProviderNode</CODE> instance for
     * a given entity.
     * The parameter entity must represent a provider, otherwise the
     * runtime IllegalArgumentException exception is thrown.
     *
     * @param e  the Entry instance, must be Realm
     * @exception IllegalArgumentException
     *                   thrown if the entity instance is not a provider
     */
    public ProviderNode(EntityHeader e) {
        super(e);
        if (e == null) {
            throw new IllegalArgumentException("entity == null");
        }
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
        list.addAll(Arrays.asList(super.getActions()));

        return (Action[]) list.toArray(new Action[]{});
    }

    /**
     * Gets the default action for this node.
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new IdentityProviderPropertiesAction(this);
    }

    /**
     * Returns the children of the reciever as an Enumeration.
     *
     * @return the Enumeration of the child nodes.
     */
    public Enumeration children() {
        List list =
          Arrays.asList(new AbstractTreeNode[]{});
        return Collections.enumeration(list);
    }

    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/providers16.gif";
    }
}
