package com.l7tech.console.tree;

import com.l7tech.console.util.IconManager2;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class AbstractTreeNode extends DefaultMutableTreeNode {
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
            hasLoadedChildren = true;
        }
        return super.getChildCount();
    }


    /**
     * @return the TreeNode in this node's child array at the specified
     * index using the filter if specified
     */
    public TreeNode getChildAt(int index, NodeFilter filter) {
        if (filter == null) {
            return super.getChildAt(index);
        }

        if (children == null) {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }

        int visibleIndex = -1;
        int realIndex = -1;
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            if (filter.accept((javax.swing.tree.TreeNode)enum.nextElement())) {
                visibleIndex++;
            }
            realIndex++;
            if (visibleIndex == index) {
                return (javax.swing.tree.TreeNode)children.elementAt(realIndex);
            }
        }
        throw new ArrayIndexOutOfBoundsException("index unmatched");
    }

    /**
     * @return returns the number of children of parent. Returns 0 if the
     *         node is a leaf or if it has no children. parent must be a
     *         node previously obtained from this data source.
     */
    public int getChildCount(NodeFilter filter) {
        int realCount = getChildCount();
        if (filter == null) {
            return realCount;
        }
        if (children == null) {
            return 0;
        }

        int count = 0;
        Enumeration enum = children.elements();
        while (enum.hasMoreElements()) {
            if (filter.accept((TreeNode)enum.nextElement())) {
                count++;
            }
        }
        return count;
    }


    /**
     * subclasses override this method
     */
    protected abstract void loadChildren();

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
        return new Action[]{};
    }

    /**
     * Make a popup menu for this node.
     * The menu is constructed from the set of actions returned
     * by {@link #getActions}.
     *
     * @return the popup menu
     */
    public final JPopupMenu getPopupMenu() {
        Action[] actions = getActions();
        if (actions == null || actions.length == 0)
            return null;
        JPopupMenu pm = new JPopupMenu();
        for (int i = 0; i < actions.length; i++) {
            pm.add(actions[i]);
        }
        return pm;
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return null;
    }

    /**
     *Test if the node can be deleted. Default is <code>false</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return false;
    }

    /**
     * Gets the default action for this node.
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return null;
    };


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

    /**
     * @return the string representation of this node
     */
    public String toString() {
        return "[" + this.getClass() + ", " + getName() + "]";
    }
}
