package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;


/**
 * The class represents a node element in the TreeModel.
 * It represents the user.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
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
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

}
