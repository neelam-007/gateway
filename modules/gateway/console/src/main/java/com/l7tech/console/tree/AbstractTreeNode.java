package com.l7tech.console.tree;

import com.l7tech.console.action.NodeAction;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.util.Cookie;
import com.l7tech.console.util.WeakPropertyChangeSupport;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;
import java.io.*;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class AbstractTreeNode extends DefaultMutableTreeNode {
    protected static final Logger logger = Logger.getLogger(AbstractTreeNode.class.getName());

    /**
     * default comparator for the child objects
     */
    protected static final Comparator<TreeNode> DEFAULT_COMPARATOR = new DefaultTreeNodeComparator();

    protected boolean hasLoadedChildren;
    private boolean isLoadingChildren = false;
    protected WeakPropertyChangeSupport propChangeSupport = new WeakPropertyChangeSupport();
    private java.util.List<Cookie> cookies = new ArrayList<Cookie>();
    protected String tooltip = null;
    protected Comparator<TreeNode> childrenComparator = DEFAULT_COMPARATOR;
    protected boolean expanded; // Keep tracing if the node is expanded or collapsed (for gui purpose only).
    private boolean isCut = false;
    private Image openedIcon;
    private Image closedIcon;

    public boolean isCut() {
        return isCut;
    }

    /**
     * Get all child nodes of this AbstractTreeNode which are a subclass of a specific class
     * 
     * @param assignableFromClasses Classes which all return nodes must be assignable from any one of them
     * @param filter NodeFilter to filter applicable nodes
     * @return List of nodes of the type assignableFromClass
     */
    public java.util.List<? extends AbstractTreeNode> collectSearchableChildren(Class[] assignableFromClasses, NodeFilter filter) {
        int kidCount = getChildCount(filter);
        java.util.List<AbstractTreeNode> returnList = new ArrayList<AbstractTreeNode>();

        for (int i = 0; i < kidCount; ++i) {
            TreeNode kid = getChildAt(i, filter);
            if (kid instanceof AbstractTreeNode) {
                AbstractTreeNode node = (AbstractTreeNode) kid;
                if (node.isSearchable(filter)) {
                    for (Class assignableFromClass: assignableFromClasses ) {
                        if (assignableFromClass.isAssignableFrom(node.getClass())) {
                            returnList.add(node);
                            break;
                        }
                    }
                }
                    
                returnList.addAll(node.collectSearchableChildren(assignableFromClasses, filter));
            }
        }
        return returnList;
    }

    public boolean isSearchable(NodeFilter filter) {
        return false;
    }

    public void setChildrenCut(boolean cut){
        for(int i = 0; i < getChildCount(); i++){
            AbstractTreeNode node = (AbstractTreeNode)getChildAt(i);
            node.setCut(cut);
            node.setChildrenCut(cut);
        }
    }

    public void setCut(boolean cut) {
        isCut = cut;
    }

    /**
     * Instantiate the tree node with the user object
     *
     * @param object the user object
     */
    public AbstractTreeNode(Object object) {
        this(object, DEFAULT_COMPARATOR);
    }

    /**
     * Instantiate the tree node with the user object and the children
     * sorted according to the <code>Comparator</code>
     *
     * @param object
     * @param c the children omparator used for sorting
     */
    public AbstractTreeNode(Object object, Comparator<TreeNode> c) {
        super(object);
        if (c !=null) {
            childrenComparator = c;
        }else{
            childrenComparator = DEFAULT_COMPARATOR;
        }
    }


    /**
     * add a cookie to the node
     *
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

        @Override
        public Object getValue() {
            return cookieValue;
        }

        private Object cookieValue;

    }

    /**
     * test whether the node has loaded it's child nodes
     *
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
     * Check if the tree node is expanded or collapsed.
     * @return true if the node is expanded.  Otherwise, false if the node is collapsed.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Set the expanding status of the tree node.
     * @param expanded: the expanding status
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Returns the number of descendants for this node.
     */
    public int getDescendantCount() {
        int count = 0;
        for (int i = 0; i < getChildCount(); i++) {
            count++;
            AbstractTreeNode child = (AbstractTreeNode) getChildAt(i);
            count += child.getDescendantCount();
        }
        return count;
    }

    /**
     * Returns the number of children <code>TreeNode</code>s the receiver
     * contains.
     */
    @Override
    public int getChildCount() {
        checkInitChildren();
        return super.getChildCount();
    }

    @Override
    public Enumeration children() {
        checkInitChildren();
        return super.children();
    }

    private void checkInitChildren() {
        if (!hasLoadedChildren && !isLoadingChildren && getAllowsChildren()) {
            loadChildren();
            hasLoadedChildren = true;
            filterChildren();
        }
    }

    /**
     * @return the TreeNode in this node's child array at the specified
     *         index using the filter if specified
     * @throw java.lang.ArrayIndexOutOfBoundsException if a filter has been set and we can't match the node at the
     * requested index to the index's of nodes the filter's accept() method shows. This will happen if a filter is set
     * and an update operation is performed on the model. Remove any filters before any updates are done.
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
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            if (filter.accept((javax.swing.tree.TreeNode)e.nextElement())) {
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
        Enumeration e = children.elements();
        while (e.hasMoreElements()) {
            if (filter.accept((TreeNode)e.nextElement())) {
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
        filterChildren();
    }

    protected final void loadChildren() {
        isLoadingChildren = true;
        try {
            doLoadChildren();
        } finally {
            isLoadingChildren = false;
        }
    }

    /**
     * subclasses may override this method
     */
    protected void doLoadChildren(){}

    /**
     * Subclasses may override this method to take some action immediately after loadChildren is called.
     */
    protected void filterChildren(){}

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     * <p/>
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
     *         by class.
     */
    public Action[] getActions(Class cl) {
        java.util.List<Action> list = new ArrayList<Action>();
        Action[] actions = getActions();
        for (Action action : actions) {
            if (cl.isAssignableFrom(action.getClass())) {
                list.add(action);
            }
        }
        return list.toArray(new Action[list.size()]);
    }

    protected JMenu getSortMenu(){
        return null;
    }

    protected JMenu getFilterMenu(){
        return null;
    }

    /**
     * Make a popup menu for this node.
     * The menu is constructed from the set of actions returned
     * by {@link #getActions()}.
     *
     * @return the popup menu
     */
    public final JPopupMenu getPopupMenu(JTree tree) {
        Action[] actions = getActions();
        if (actions == null || actions.length == 0)
            return null;
        JPopupMenu pm = new JPopupMenu();
        for (final Action action : actions) {
            //add separator before adding refresh menu item
            //NOTE: the display of menu items depends on the order of actions (array), so care must be taken for
            //populating actions array, otherwise there's no way to determine the proper grouping to put the separator
            if (action instanceof RefreshTreeNodeAction && pm.getComponentCount() > 0) {
                pm.addSeparator();
            }            
            if (action instanceof SecureAction) {
                final SecureAction secureAction = ((SecureAction) action);
                if (secureAction.isAuthorized()) {
                    pm.add(action);
                    if (secureAction instanceof NodeAction) {
                        ((NodeAction) secureAction).setTree(tree);
                    }
                }
            } else {
                pm.add(action);
            }
        }

        JMenu filterMenu = getFilterMenu();
        if(filterMenu != null){
            pm.add(filterMenu);
        }

        JMenu sortMenu = getSortMenu();
        if(sortMenu != null){
            pm.add(sortMenu);
        }

        Utilities.removeToolTipsFromMenuItems(pm);
        return pm;
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion or <b>null</b>
     */
    public Assertion asAssertion() {
        return null;
    }

    /**
     * Return assertions representation of the node. Palette elements that
     * return multiple elements override this api.
     * <p/>
     * or <b>emtpy array</b> if the node does not have any assertins
     *
     * @return the assertions corrsponding to this node
     */
    public Assertion[] asAssertions() {
        Assertion a = asAssertion();
        if (a == null) {
            return new Assertion[]{};
        }
        return new Assertion[]{a};
    }

    /**
     * Test if the node can be deleted. Default is <code>false</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return false;
    }

    /**
     * Test if the node can be refreshed. Default is <code>false</code>
     *
     * @return true if the node children can be refreshed, false otherwise
     */
    public boolean canRefresh() {
        return false;
    }

    /**
     * Is this nodes assertion editable?
     *
     * @return false if read only
     */
    public boolean canEdit() {
        return true;
    }

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    @Nullable
    public Image getIcon() {
        if (closedIcon == null) {
            closedIcon = getIcon(false);
        }
        return closedIcon;
    }

    /**
     * Finds an icon for this node when opened. This icon should
     * represent the node only when it is opened (when it can have
     * children).
     *
     * @return icon to use to represent the bean when opened
     */
    @Nullable
    public Image getOpenedIcon() {
        if (openedIcon == null) {
            openedIcon = getIcon(true);
        }
        return openedIcon;
    }

    /**
     * Clear the currently set closed and open icons so that they are refreshed next time they are retrieved.
     */
    public void clearIcons() {
        closedIcon = null;
        openedIcon = null;
    }

    /**
     * Gets an Image for this node from a base 64 encoded string (if available) or from ImageCache.
     *
     * @param open for nodes that can be opened, can have children
     * @return an Image for this node or null if unavailable.
     */
    @Nullable
    private Image getIcon(boolean open) {
        Image image = null;
        final String iconImage = base64EncodedIconImage(open);
        if (iconImage != null) {
            final byte[] imageBytes = HexUtils.decodeBase64(iconImage);
            image = ImageCache.getInstance().createUncachedBufferedImage(imageBytes, Transparency.BITMASK);
        } else {
            image = getCachedImage(open);
        }
        return image;
    }

    private Image getCachedImage(boolean open) {
        ImageCache cache = ImageCache.getInstance();
        ClassLoader loader = iconClassLoader();
        String path = iconResource(open);
        return loader == null
               ? cache.getIcon(path)
               : cache.getIcon(path, loader);
    }

    /**
     * @return the node name that is displayed
     */
    public abstract String getName();

    /**
     * @return the node tooltip
     */
    public String getTooltipText() {
        return tooltip;
    }

    /**
     * @return custom classloader to use when loading icon resources, or null to allow image cache to just use its own
     */
    protected ClassLoader iconClassLoader() {
        Object obj = asAssertion();
        if (obj == null)
            obj = getUserObject();        
        return obj == null ? null : obj.getClass().getClassLoader();
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected abstract String iconResource(boolean open);

    /**
     * Override this method if this node icon has a base 64 encoded image that is not a resource.
     *
     * If non-null, will have priority over any resource returned by iconResource(boolean open);
     *
     * @param open for nodes that can be opened, can have children
     * @return a base 64 encoded string which represents an image (can be null)
     */
    @Nullable
    protected String base64EncodedIconImage(boolean open) {
        return null;
    }


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

    /**
     * Remove listener for changes in properties
     */
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
    @Override
    public String toString() {
        return "[" + this.getClass() + ", " + getName() + "]";
    }

    /**
     * Determine the insert position for the node according to
     * the sort order of the children. The order is defined by
     * the <code>Comparator</code> returned by the
     * {@link #getChildrenComparator()} method.
     * <p/>
     * It is important that the children are stored in an
     * unsorted collection (Vector is the default used). See
     * {@link java.util.Comparator} for the discussion about the Comparator
     * based ordering in sorted sets and {@link Object#equals(Object)}.
     *
     * @param node the node to deremine the position for
     * @return the index value in the children list where the
     *         child should go
     */
    public int getInsertPosition(MutableTreeNode node) {
        if (children == null) {
            return 0;
        }
        int size = children.size();
        Comparator<? super TreeNode> c = getChildrenComparator();
        int index = 0;

        for (; index < size; index++) {
            int res = c.compare(node, (TreeNode)children.get(index));
            if (res <= 0) return index;
        }
        return index;
    }

    /**
     * Allow the calling code to specify the comparator to use, without having to use the template
     * getChildrenComparator method which would apply to all uses of the subclass
     * @param node
     * @param c
     * @return
     */
    public int getInsertPosition(MutableTreeNode node, Comparator<? super TreeNode> c) {
        if (children == null) {
            return 0;
        }
        int size = children.size();
        int index = 0;

        for (; index < size; index++) {
            int res = c.compare(node, (TreeNode)children.get(index));
            if (res <= 0) return index;
        }
        return index;
    }

    /**
     * Insert a node in it's natural position.
     *
     * @param node The node to insert.
     */
    public void insert( final MutableTreeNode node ) {
        insert( node, getInsertPosition(node) );
    }

    /**
     * Insert a node in a position determined by the given comparator.
     *
     * @param node The node to insert.
     * @param comparator The comparator to use
     */
    public void insert( final MutableTreeNode node,
                        final Comparator<? super TreeNode> comparator ) {
        insert( node, getInsertPosition(node, comparator) );
    }

    /**
     * Get this node children comparator that defines the children
     * order.
     *
     * @return the comparator used to sort the children
     */
    protected Comparator<? super TreeNode> getChildrenComparator() {
        return childrenComparator;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return null;
    }

    private static class DefaultTreeNodeComparator implements Comparator<TreeNode>, Serializable {
        @SuppressWarnings({"unchecked"})
        @Override
        public int compare(TreeNode o1, TreeNode o2) {
            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                return ((Comparable)o1).compareTo(o2);
            }
            return 0; // no order - assume everything equal
        }

        private Object readResolve() {
            return DEFAULT_COMPARATOR;
        }
    }
}
