package com.l7tech.console.tree;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.console.action.NodeAction;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.WeakPropertyChangeSupport;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class AbstractTreeNode extends DefaultMutableTreeNode {
    static Logger logger = Logger.getLogger(AbstractTreeNode.class.getName());
    protected boolean hasLoadedChildren;
    protected WeakPropertyChangeSupport propChangeSupport = new WeakPropertyChangeSupport();
    private java.util.List cookies = new ArrayList();

    /**
     * Instantiate the
     * @param object
     */
    public AbstractTreeNode(Object object) {
        super(object);
    }

    /**
     * add a cookie to the node
     * @param c
     */
    public void addCookie(Cookie c) {
        cookies.add(c);

    }

    /**
     * @return the cookies iterator
     */
    public Iterator cookies() {
        return cookies.iterator();
    }

    /**
     * node cookie class.
     */
    public static class NodeCookie implements Cookie {
        public NodeCookie(Object cookieValue) {
            this.cookieValue = cookieValue;
        }

        public Object getValue() {
            return cookieValue;
        }

        private Object cookieValue;
    }

    /**
     * test whether the node has loaded it's child nodes
     * @return true if the children have been loaded,
     *         false otherwise
     */
    public boolean hasLoadedChildren() {
        return hasLoadedChildren;
    }

    /**
     * @param b the new children loaded property
     */
    public void setHasLoadedChildren(boolean b) {
        this.hasLoadedChildren = b;
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    public int getChildCount() {
        if (!hasLoadedChildren) {
            if (getAllowsChildren()) {
                loadChildren();
                hasLoadedChildren = true;
            }
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
     * reload the children under this node
     */
    public void reloadChildren() {
        hasLoadedChildren = false;
        loadChildren();
        hasLoadedChildren = true;
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
     * By default returns the empty actions array.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{};
    }

    /**
     * Get the set of actions associated with this node that
     * are assignable by the class parameter.
     *
     * @param cl the class paremeter to test the actions against
     * @return actions appropriate to the node that are assignable
     * by class.
     */
    public Action[] getActions(Class cl) {
        java.util.List list = new ArrayList();
        Action[] actions = getActions();
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            if (cl.isAssignableFrom(action.getClass())) {
                list.add(actions[i]);
            }
        }
        return (Action[])list.toArray(new Action[]{});
    }


    /**
     * Make a popup menu for this node.
     * The menu is constructed from the set of actions returned
     * by {@link #getActions}.
     *
     * @return the popup menu
     */
    public final JPopupMenu getPopupMenu(JTree tree) {
        Action[] actions = getActions();
        if (actions == null || actions.length == 0)
            return null;
        JPopupMenu pm = new JPopupMenu();
        for (int i = 0; i < actions.length; i++) {
            //todo: consider reworking this a bit. em
            if (actions[i] instanceof NodeAction) {
                ((NodeAction)actions[i]).setTree(tree);
            }
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
     *Test if the node can be refreshed. Default is <code>false</code>
     *
     * @return true if the node children can be refreshed, false otherwise
     */
    public boolean canRefresh() {
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
        return ImageCache.getInstance().getIcon(iconResource(false));

    }

    /**
     * Finds an icon for this node when opened. This icon should
     * represent the node only when it is opened (when it can have
     * children).
     * @return icon to use to represent the bean when opened
     */
    public Image getOpenedIcon() {
        return ImageCache.getInstance().getIcon(iconResource(true));
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
     * Add  listener to listen to change of any property.
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        propChangeSupport.addPropertyChangeListener(l);
    }

    /**
     * Add listener to listen to change of the specified property.
     */
    public synchronized void addPropertyChangeListener(String propertyName,
                                                       PropertyChangeListener l) {
        propChangeSupport.addPropertyChangeListener(propertyName, l);
    }

    /** Remove listener for changes in properties */
    public synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        propChangeSupport.removePropertyChangeListener(l);
    }

    public void firePropertyChange(Object source, String propertyName,
                                   Object oldValue, Object newValue) {
        propChangeSupport.firePropertyChange(source, propertyName, oldValue, newValue);
    }


    /**
     * @return the string representation of this node
     */
    public String toString() {
        return "[" + this.getClass() + ", " + getName() + "]";
    }
}
