package com.l7tech.console.tree;




/**
 * The class represents a node element in the palette assertion tree.
 * It represents the folder with XML security elements.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class XmlSecurityFolderNode extends AbstractTreeNode {
    /**
     * construct the <CODE>PoliciesFolderNode</CODE> instance for
     * a given entry.
     *
     */
    public XmlSecurityFolderNode() {
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
        int index = 0;
        insert(new RequestWssIntegrityNode(), index++);
        insert(new RequestWssConfidentialityNode(), index++);
        insert(new ResponseWssIntegrityNode(), index++);
        insert(new ResponseWssConfidentialityNode(), index++);
        insert(new SamlSecurityNode(), index++);
    }

    /**
     * Returns the node name.
     *
     * @return the name as a String
     */
    public String getName() {
        return "XML Security";
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
