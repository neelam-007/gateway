package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A JTree that displays breakpoint icons.
 */
public class AssertionBreakpointsTree extends JTree {
    private final PolicyStepDebugDialog policyStepDebugDialog;
    private final JTree policyTree;

    /**
     * Creates <code>AssertionBreakpointsTree</code>.
     *
     * @param policyStepDebugDialog the policy step debug dialog
     * @param policyTree the policy tree
     */
    AssertionBreakpointsTree(@NotNull PolicyStepDebugDialog policyStepDebugDialog, @NotNull JTree policyTree) {
        this.policyStepDebugDialog = policyStepDebugDialog;
        this.policyTree = policyTree;

        this.initialize();
    }

    /**
     * Updates the tree.
     */
    void updateTree() {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode();
        for (int row = 0; row < policyTree.getRowCount(); row++) {
            TreePath path = policyTree.getPathForRow(row);
            top.add(new DefaultMutableTreeNode(path.getLastPathComponent()));
        }

        TreeModel model = new DefaultTreeModel(top);
        this.setModel(model);
    }

    /**
     * Checks whether or not a breakpoint is set for the given node.
     *
     * @param node the node
     * @return the policy step debug dialog
     */
    boolean isBreakpointSet(@NotNull AssertionTreeNode node) {
        return policyStepDebugDialog.isBreakpointSet(node);
    }

    private void initialize() {
        this.setVisible(true);
        this.setRootVisible(false);
        this.setShowsRootHandles(false);
        this.setAlignmentX(Component.LEFT_ALIGNMENT);
        this.setAlignmentY(Component.TOP_ALIGNMENT);
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        this.setCellRenderer(new AssertionBreakpointsTreeCellRenderer());
        this.addMouseListener(mouseListener);

        policyTree.removeTreeExpansionListener(policyTreeExpansionListener);
        policyTree.addTreeExpansionListener(policyTreeExpansionListener);

        this.updateTree();
    }

    private TreeExpansionListener policyTreeExpansionListener = new TreeExpansionListener() {
        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            updateTree();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            updateTree();
        }
    };

    private MouseListener mouseListener = new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
            JTree tree = (JTree) e.getSource();
            TreePath path = tree.getSelectionPath();
            if (path == null) {
                return;
            }

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            AssertionTreeNode node = (AssertionTreeNode) treeNode.getUserObject();
            policyStepDebugDialog.onToggleBreakpoint(node);
        }

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}
    };
}