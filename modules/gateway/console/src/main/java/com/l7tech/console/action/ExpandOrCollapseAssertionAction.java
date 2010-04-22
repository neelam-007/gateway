package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * An action is to expand/collapse a non-leaf assertion node.
 * If multiple nodes are selected, then expand/collapse them at the same time.
 *
 * @author ghuang
 */
public class ExpandOrCollapseAssertionAction extends NodeAction {
    private final JTree policyTree = TopComponents.getInstance().getPolicyTree();
    private boolean expand;

    public ExpandOrCollapseAssertionAction(boolean expand) {
        super(null);
        this.expand = expand;
    }

    /**
     *  Expand/collapse a selected assertion node.  If there are multiple assertion nodes selected, then these selected nodes will be expanded/collapsed.
     */
    @Override
    protected void performAction() {
        // Get all selected assertion nodes.
        List<AbstractTreeNode> nodes = getSelectedNodes();

        // Case 1: There is no any nodes selected in the policy tree.  This means that the expand/collapse action
        //             will act on the whole policy tree.  Then, all assertion nodes will be expanded/collapsed.
        if (nodes.isEmpty()) {
            // Note: In this case, we use expandRow or collapseRow, which are much faster than
            // recursively expanding/collapsing assertion nodes in their path from bottom to up.
            for (int i = 0; i < policyTree.getRowCount(); i++) {
                // Expand/collapse all assertion nodes in the row
                if (expand) {
                    policyTree.expandRow(i);
                } else {
                    policyTree.collapseRow(i);
                }
                // Update the expand status of all assertion node in the row
                TreePath path = policyTree.getPathForRow(i);
                for (Object node: path.getPath()) {
                    if (node instanceof AbstractTreeNode) {
                        ((AbstractTreeNode)node).setExpanded(expand);
                    }
                }
            }
            // Find the top root of the policy tree and update its expanding status.
            TreePath path = policyTree.getPathForRow(0);
            AbstractTreeNode root = (AbstractTreeNode) ((AbstractTreeNode) path.getLastPathComponent()).getRoot();
            root.setExpanded(expand);
        }
        // Case 2: Otherwise, expand/collapse all selected assertion node.
        else {
            for (AbstractTreeNode node: nodes) {
                // Expand/collapse the specific node
                if (node.getChildCount() > 0) {
                    TreePath path = new TreePath(node.getPath());
                    expandOrCollapsePath(path);
                }
                // Update the expanding status of the specific node.
                node.setExpanded(expand);
            }
        }
    }

    /**
     * Get all selected nodes to be expanded/collapsed.
     * @return a list of selected nodes.
     */
    private List<AbstractTreeNode> getSelectedNodes() {
        List<AbstractTreeNode> nodes = new ArrayList<AbstractTreeNode>();

        TreePath[] paths = policyTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath path : paths) {
                nodes.add((AbstractTreeNode) path.getLastPathComponent());
            }
        }

        return nodes;
    }

    /**
     * Recursively expand/collapse all tree nodes in the path from bottom to up.
     * @param path: a node path in which all nodes will be expanded/collapsed.
     */
    private void expandOrCollapsePath(TreePath path) {
        // Traverse children
        TreeNode node = (TreeNode)path.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e=node.children(); e.hasMoreElements(); ) {
                TreeNode nodeObj = (TreeNode)e.nextElement();

                if (nodeObj instanceof AbstractTreeNode) {
                    ((AbstractTreeNode)nodeObj).setExpanded(expand);
                }

                TreePath newPath = path.pathByAddingChild(nodeObj);
                expandOrCollapsePath(newPath);
            }
        }
        // Expansion/collapse must be done bottom-up
        if (expand) {
            policyTree.expandPath(path);
        } else {
            policyTree.collapsePath(path);
        }
    }
}
