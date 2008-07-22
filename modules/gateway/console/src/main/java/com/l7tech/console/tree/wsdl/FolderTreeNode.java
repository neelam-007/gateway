/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.wsdl;

import javax.swing.tree.MutableTreeNode;
import java.util.Iterator;

/**
 * @author mike
 */
class FolderTreeNode extends WsdlTreeNode {
    private FolderLister lister;

    FolderTreeNode(FolderLister l, Options options) {
        super(null, options);
        this.lister = l;
    }

    protected void loadChildren() {
        int index = 0;
        children = null;
        for (Iterator i = lister.list().iterator(); i.hasNext();) {
            insert((MutableTreeNode)i.next(), index++);
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

    /**
     * @return a string representation of the object.
     */
    public String toString() {
        return lister.toString();
    }

}
