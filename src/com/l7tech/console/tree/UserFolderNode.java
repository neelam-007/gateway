package com.l7tech.console.tree;

import com.l7tech.identity.UserManager;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.console.action.NewUserAction;

import javax.swing.tree.MutableTreeNode;
import javax.swing.*;
import java.util.Enumeration;


/**
 * The class represents a node element in the TreeModel.
 * It represents the folder with users.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class UserFolderNode extends AbstractTreeNode {
    public static String INTERNAL_USERS_NAME = "Internal Users";

    private UserManager userManager;
    private long providerId;
    private String name;

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param um the user manager
     */
    public UserFolderNode(UserManager um, long providerId) {
       this(um, providerId, INTERNAL_USERS_NAME);
    }

    /**
     * construct the <CODE>UserFolderNode</CODE> instance
     *
     * @param um the user manager
     * @param providerId the provider id
     * @param name the folder name
     */
    public UserFolderNode(UserManager um, long providerId, String name) {
        super(null);
        userManager = um;
        this.providerId = providerId;
        this.name = name;
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
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        final NewUserAction newUserAction = new NewUserAction(this);
        newUserAction.setEnabled(providerId == IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        return new Action[]{newUserAction};
    }

    /**
     * Returns the provider id for the users.
     *
     * @return the provider id
     */
    public long getProviderId() {
        return providerId;
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return name;
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
        children = null;
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode)e.nextElement(), index++);
        }
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
