package com.l7tech.console.tree;

import javax.swing.tree.TreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * FilteredTreeModel extends DirTreeModel and provides basic
 * filtering.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class FilteredTreeModel extends DefaultTreeModel {
    /**
     * Creates a new instance of FilteredTreeModel with root set
     * to the root of this model.
     *
     * @param root   the new root
     */
    public FilteredTreeModel(TreeNode root) {
        super(root);
    }

    /**
     * associate the filter with this <CODE>TreeModel</CODE>
     *
     * @param filter new NodeFilter
     */
    public void setFilter(NodeFilter filter) {
        this.filter = filter;
    }

    /**
     * clear the filter associated with this <CODE>TreeModel</CODE>
     */
    public void clearFilter() {
        filter = null;
    }

    /**
     * Returns the child of <I>parent</I> at index <I>index</I> in the parent's
     * child array.
     * The method uses the filter if specified (not null)
     *
     * @param parent a node in the tree, obtained from this data source
     * @param index
     * @return the child of <I>parent</I> at index <I>index</I>
     */
    public Object getChild(Object parent, int index) {
        if (filter != null) {
            if (parent instanceof AbstractTreeNode) {
                return ((AbstractTreeNode) parent).getChildAt(index, filter);
            }
        }
        return ((TreeNode) parent).getChildAt(index);
    }

    /**
     * Returns the number of children of <I>parent</I>.  Returns 0 if the node
     * is a leaf or if it has no children.  <I>parent</I> must be a node
     * previously obtained from this data source.
     *
     * The method uses the filter if specified (not null)
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <I>parent</I>
     */
    public int getChildCount(Object parent) {
        if (filter != null) {
            if (parent instanceof AbstractTreeNode) {
                return ((AbstractTreeNode)parent).getChildCount(filter);
            }
        }
        return ((TreeNode) parent).getChildCount();
    }

    private NodeFilter filter;
}

