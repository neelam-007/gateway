package com.l7tech.console.tree;

import com.l7tech.console.util.Registry;

import javax.swing.tree.MutableTreeNode;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * The class represents an entry gui node element that
 * corresponds to the Root data element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class RootNode extends AbstractTreeNode {

    /**
     * construct the <CODE>RootNode</CODE> instance
     */
    public RootNode(String title)
      throws IllegalArgumentException {
        super(null);
        if (title == null)
            throw new IllegalArgumentException();
        label = title;
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
        Registry r = Registry.getDefault();
        List list =
          Arrays.asList(
            new AbstractTreeNode[]{
                new UserFolderNode(r.getInternalUserManager()),
                new GroupFolderNode(r.getInternalGroupManager()),
                new ProvidersFolderNode(),
                new PoliciesFolderNode(),
                new AuthMethodFolderNode(),
                new TransportLayerSecurityFolderNode()
            });
        int index = 0;
        for (Iterator i = list.iterator(); i.hasNext();) {
            insert((MutableTreeNode) i.next(), index++);
        }
    }

    /**
     *
     * @return the root name
     */
    public String getName() {
        return label;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/root.gif";
    }

    private String label;
}
