package com.l7tech.console.tree;

import com.l7tech.console.action.NewProviderAction;
import com.l7tech.identity.IdentityProviderConfigManager;

import javax.swing.*;
import javax.swing.tree.MutableTreeNode;
import java.util.Enumeration;

/**
 * The class represents an entry gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class ProvidersFolderNode extends AbstractTreeNode {
    IdentityProviderConfigManager manager;
    public static String NAME = "Identity providers";

    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public ProvidersFolderNode(IdentityProviderConfigManager im) {
        super(null);
        manager = im;
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
     * subclasses override this method
     */
    protected void loadChildren() {
         Enumeration e =
          TreeNodeFactory.
          getTreeNodeEnumeration(
            new EntitiesEnumeration(new ProviderEntitiesCollection(manager)));
        int index = 0;
        children = null;
        for (; e.hasMoreElements();) {
            insert((MutableTreeNode) e.nextElement(), index++);
        }
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * <P>
     * By default returns the empty actions arrays.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{new NewProviderAction(this)};
    }

    /**
     * Returns the node name.
     * Gui nodes have name to facilitate handling in
     * hierarchical gui components such as JTree.
     *
     * @return the FQ name as a String
     */
    public String getName() {
        return NAME;
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
