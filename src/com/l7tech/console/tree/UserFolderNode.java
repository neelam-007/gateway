package com.l7tech.console.tree;

import com.l7tech.identity.UserManager;

import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with users.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class UserFolderNode extends AbstractTreeNode {
    private UserManager userManager;

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param um the children enumeration
     */
    public UserFolderNode(UserManager um) {
        super(null);
        userManager = um;
    }

    /**
     * Returns true if the receiver is a leaf.
     *
     * @return true if leaf, false otherwise
     */
    public boolean isLeaf() {
        return false;
    }

    /**
     * Returns true if the receiver allows children.
     */
    public boolean getAllowsChildren() {
        return true;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
         Enumeration e =
       TreeNodeFactory.
         getTreeNodeEnumeration(
           new EntitiesEnumeration(new UserEntitiesCollection(userManager)));
       int index = 0;
       for (; e.hasMoreElements();) {
           insert((MutableTreeNode)e.nextElement(), index++);
       }
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return "Users";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
         if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }

}
