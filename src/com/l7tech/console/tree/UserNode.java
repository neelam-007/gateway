package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.console.action.NewUserAction;
import com.l7tech.console.action.UserPropertiesAction;

import javax.swing.*;


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
     * @param e  the EntityHeader instance, must represent user
     * @exception IllegalArgumentException
     *                   thrown if unexpected type
     */
    public UserNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }

     /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{new UserPropertiesAction(this)};
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
