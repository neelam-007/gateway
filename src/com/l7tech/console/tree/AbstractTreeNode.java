package com.l7tech.console.tree;

import com.l7tech.console.util.IconManager2;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.awt.*;

/**
 * todo: remove the BasicTreeNode interface from the
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class AbstractTreeNode
  extends DefaultMutableTreeNode  implements BasicTreeNode {
    protected boolean hasLoadedChildren;

    public AbstractTreeNode(Object object) {
        super(object);
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        if (!hasLoadedChildren) {
            loadChildren();
        }
        return super.getChildCount();
    }

    /**
     * subclasses override this method
     */
    protected abstract void loadChildren();

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    public Image getIcon() {
        return IconManager2.getInstance().getIcon(iconResource(false));

    }

    /**
     * Finds an icon for this node when opened. This icon should
     * represent the node only when it is opened (when it can have
     * children).
     * @return icon to use to represent the bean when opened
     */
    public Image getOpenedIcon() {
       return IconManager2.getInstance().getIcon(iconResource(true));
    }

    /**
     * @return the node name that is displayed
     */
    public abstract String getName();


    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected abstract String iconResource(boolean open);
}
