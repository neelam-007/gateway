package com.l7tech.console.tree;

import com.l7tech.console.action.UserPropertiesAction;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * The class represents a node element in the TreeModel.
 * It represents the user.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.2
 */
public class UserNode extends EntityHeaderNode {
    /**
     * construct the <CODE>UserNode</CODE> instance for
     * a given e.
     * 
     * @param e the EntityHeader instance, must represent user
     * @throws IllegalArgumentException thrown if unexpected type
     */
    public UserNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
        // assume that the strid is a valuable piece of information if it;s something else than a number
        String strid = e.getStrId();
        try {
            Long.parseLong(strid);
            tooltip = null;
        } catch (NumberFormatException nfe) {
            tooltip = strid;
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
        final UserPropertiesAction userPropertiesAction = new UserPropertiesAction(this);
        //userPropertiesAction.setEnabled(canDelete());
        list.add(userPropertiesAction);
        list.addAll(Arrays.asList(super.getActions()));

        return (Action[])list.toArray(new Action[]{});
    }

    /**
     * test whether the node can be deleted. Only the internal
     * nodes can be deleted.
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return isInternal();
    }

    /**
     * test whether the node belongs to the internal provider
     * 
     * @return true if the node is internal, false otherwise
     */
    protected final boolean isInternal() {
        ProviderNode parent = (ProviderNode)getParent();
        return parent.getEntityHeader().getOid() == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID;
    }


    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     * 
     * @return the popup menu
     */
    public Assertion asAssertion() {
        ProviderNode parent = (ProviderNode)getParent();
        EntityHeader e = parent.getEntityHeader();
        return new SpecificUser(parent.getEntityHeader().getOid(), getEntityHeader().getName());
    }

    /**
     * Gets the default action for this node.
     * 
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new UserPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     * 
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

    public String getTooltipText() {
        return tooltip;
    }

    private String tooltip = null;
}
