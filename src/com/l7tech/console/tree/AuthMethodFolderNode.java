package com.l7tech.console.tree;

import java.util.Collections;
import java.util.Enumeration;


/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with transport layer securitry.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class AuthMethodFolderNode extends AbstractTreeNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public AuthMethodFolderNode() {
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
        return true;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return "Authentication methods";
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
