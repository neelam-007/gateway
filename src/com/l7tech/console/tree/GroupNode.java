package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;


/**
 * The class represents a node element in the TreeModel.
 * It represents the group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.2
 */
public class GroupNode extends EntityHeaderNode {
    /**
     * construct the <CODE>UserFolderNode</CODE> instance for
     * a given e.
     *
     * @param e the EntityHeader instance, must represent Group
     * @throws IllegalArgumentException thrown if the EntityHeader instance does not
     *                                  represent a group.
     */
    public GroupNode(EntityHeader e)
      throws IllegalArgumentException {
        super(e);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/group16.png";
    }
}
