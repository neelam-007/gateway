package com.l7tech.console.tree;

/**
 * The class represents an gui node element in the TreeModel that
 * represents the misc assertions folder.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 19, 2004<br/>
 * $Id$
 */
public class MiscFolderNode extends AbstractTreeNode {
    /**
     * construct the <CODE>ProvidersFolderNode</CODE> instance.
     */
    public MiscFolderNode() {
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
     * subclasses override this method
     */
    protected void loadChildren() {
        int index = 0;
        children = null;
        insert( new TimeRangePaletteNode(), index++ );
        insert( new RemoteIpRangePaletteNode(), index++ );
        insert( new ThroughputQuotaPaletteNode(), index++ );
    }

    /**
     * Returns the node name.
     * Gui nodes have name to facilitate handling in
     * hierarchical gui components such as JTree.
     *
     * @return the FQ name as a String
     */
    public String getName() {
        return "Service Availability";
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
