package com.l7tech.console.tree;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.console.action.NewProviderAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * The class represents an entry gui node element in the
 * TreeModel.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
public class ProvidersFolderNode extends AbstractTreeNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public ProvidersFolderNode() {
        super(null);
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
        return false;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
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
        return "Identity providers";
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
