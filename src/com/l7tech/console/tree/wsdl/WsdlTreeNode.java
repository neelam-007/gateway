package com.l7tech.console.tree.wsdl;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.tree.AbstractTreeNode;

import javax.swing.tree.MutableTreeNode;
import java.util.Iterator;
import java.util.List;


/**
 * the WSDL Tree Node represents the WSDL backed model.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @see com.l7tech.console.tree.AbstractTreeNode
 */
public abstract class WsdlTreeNode extends AbstractTreeNode {

    protected WsdlTreeNode(Object userObject) {
        super(userObject);
    }

    /**
     * creates a <CODE>TreeNode</CODE> with the given Wsdl
     * as a user object.
     * 
     * @param wsdl the tree node this node points to
     */
    public static WsdlTreeNode newInstance(Wsdl wsdl) {
        if (wsdl == null) {
            throw new IllegalArgumentException();
        }
        return new DefinitionsTreeNode(wsdl.getDefinition());
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
     * @return the node name that is displayed
     */
    public String getName() {
        return this.toString();
    }

    public interface FolderLister {
        List list();
    }
}

class FolderTreeNode extends WsdlTreeNode {
    private FolderLister lister;

    FolderTreeNode(FolderLister l) {
        super(null);
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
