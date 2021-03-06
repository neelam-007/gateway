package com.l7tech.console.panels.policydiff;

import com.l7tech.console.policy.PolicyTransferable;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeHidingTransferHandler;
import com.l7tech.console.tree.policy.PolicyTreeCellRenderer;
import com.l7tech.console.util.PopUpMouseListener;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.policy.assertion.BlankAssertion;
import com.l7tech.util.Pair;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import static com.l7tech.console.panels.policydiff.PolicyDiffWindow.*;

/**
 * Class to create a tree to show policy diff result.
 */
class PolicyDiffTree extends JTree {
    private static final Color COLOR_FOR_SELECTION = new Color(115,164,209); // Dark blue color used to highlight selected rows

    private final PolicyDiffWindow diffWindow;
    private JScrollPane parentScrollPane;
    private Map<Integer, DiffType> diffResultMap;

    public PolicyDiffTree(final PolicyDiffWindow diffWindow) {
        this.diffWindow = diffWindow;
        initialize();
    }

    public void setParentScrollPane(JScrollPane parentScrollPane) {
        this.parentScrollPane = parentScrollPane;
    }

    public Map<Integer, DiffType> getDiffResultMap() {
        return diffResultMap;
    }

    public void setDiffResultMap(Map<Integer, DiffType> diffResultMap) {
        this.diffResultMap = diffResultMap;
    }

    private void initialize() {
        setOpaque(false);
        setCellRenderer(new PolicyDiffTreeCellRenderer());

        addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent e) {}
            @Override
            public void treeWillCollapse(TreeExpansionEvent e) throws ExpandVetoException {
                throw new ExpandVetoException(e, "not allow to collapse the tree");
            }
        });

        addMouseListener(new PolicyDiffTreeMouseListener());

        addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                final JTree policyTree = (JTree) e.getSource();

                // We don't handle multiple selections
                final int row = policyTree.getMinSelectionRow();
                if (row == -1) return;

                // Tell diff window row selection has changed
                diffWindow.policyTreeSelectionChanged(PolicyDiffTree.this, row);

                // Show an assertion diff panel
                diffWindow.showAssertionDiff(row);

                // Check if there are any prev and next diffs available
                diffWindow.enableOrDisableDiffNavigationButtons(row);
            }
        });

        // Disable cut (and ctrl-X)
        putClientProperty(ClipboardActions.CUT_HINT, Boolean.FALSE);

        ClipboardActions.replaceClipboardActionMap(this);

        // To support "Copy All", need to register a "copyAll" action that does equivalent of Select All followed by Copy.
        getActionMap().put("copyAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getSelectionModel().clearSelection();
                ClipboardActions.getCopyAction().actionPerformed(e);
            }
        });

        setTransferHandler(new PolicyDiffTreeTransferHandler());
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Highlight some rows with a diff result by four different colors, light green, light red, light blue, and light grey.
        highlightDiffTree(g, this, diffResultMap, getVisibleRowRange(parentScrollPane, this));
        super.paintComponent(g);
    }

    /**
     * Find the first and last visible row in the tree embedded in the scroll pane.
     *
     * @param treeScrollPane: the scroll pane holds the tree
     * @param tree: the tree to be checked
     *
     * @return a pair of integers contains the first visible row and the last visible row of the tree in the scroll pane.
     */
    private Pair<Integer, Integer> getVisibleRowRange(JScrollPane treeScrollPane, JTree tree){
        if (treeScrollPane == null || tree == null) return null;

        final Rectangle visibleRectangle = treeScrollPane.getViewport().getViewRect();
        final int firstRow = tree.getClosestRowForLocation(visibleRectangle.x, visibleRectangle.y);
        final int lastRow  = tree.getClosestRowForLocation(visibleRectangle.x, visibleRectangle.y + visibleRectangle.height);
        return new Pair<>(firstRow, lastRow);
    }

    /**
     * A sub-class of PolicyTreeCellRenderer is used by the Policy Diff feature to customizes setting row background color.
     */
    private class PolicyDiffTreeCellRenderer extends PolicyTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            setBackgroundNonSelectionColor(null);
            setBackgroundSelectionColor(null);

            return this;
        }

        @Override
        public Color getBackground() {
            return null;
        }
    }

    /**
     * A class handles policy tree mouse events such as right click to pop up a menu (with three items, "Compare Assertion",
     * "Copy", and "Copy All") and double click to pop up an assertion comparison window,
     */
    private class PolicyDiffTreeMouseListener extends PopUpMouseListener {
        @Override
        protected void popUpMenuHandler(MouseEvent mouseEvent) {
            final JTree policyTree = (JTree) mouseEvent.getSource();
            policyTree.requestFocus();

            final int closestRow = policyTree.getClosestRowForLocation(mouseEvent.getX(), mouseEvent.getY());
            if (closestRow == -1) return;

            int[] rows = policyTree.getSelectionRows();
            boolean found = false;

            for (int i = 0; rows != null && i < rows.length; i++) {
                if (rows[i] == closestRow) {
                    found = true;
                    break;
                }
            }
            if (! found) {
                policyTree.setSelectionRow(closestRow);
            }

            final JPopupMenu menu = new JPopupMenu();

            menu.add(diffWindow.getAssertionDiffMenuAction(closestRow));
            menu.add(ClipboardActions.getGlobalCopyAction());
            if (ClipboardActions.getGlobalCopyAllAction().isEnabled())
                menu.add(ClipboardActions.getGlobalCopyAllAction());

            menu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());

            // If right clicking on a BlankAssertion, disable all actions.
            AbstractTreeNode node = (AbstractTreeNode) policyTree.getPathForRow(closestRow).getLastPathComponent();
            if (node.asAssertion() instanceof BlankAssertion) {
                for (MenuElement menuElement: menu.getSubElements())  {
                    JMenuItem item = (JMenuItem) menuElement;
                    item.getAction().setEnabled(false);
                }
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            final JTree policyTree = (JTree) e.getSource();
            final int row = policyTree.getClosestRowForLocation(e.getX(), e.getY());

            // Handle double click
            if (e.getClickCount() != 2) return;

            diffWindow.getAssertionDiffMenuAction(row).actionPerformed(null);
        }
    }

    /**
     * A transfer handler is used by the policy diff tree for Copy and Copy All.
     * Paste is disabled.
     */
    private class PolicyDiffTreeTransferHandler extends TreeNodeHidingTransferHandler {
        @Override
        protected Transferable createTransferable(JComponent c) {
            PolicyDiffTree diffTree = c instanceof PolicyDiffTree? (PolicyDiffTree)c : PolicyDiffTree.this;
            return createTransferable(diffTree);
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            // Pastes not accepted
            return false;
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            // Pastes not accepted
            return false;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }

        private Transferable createTransferable(PolicyDiffTree policyDiffTree) {
            TreePath[] paths = policyDiffTree.getSelectionPaths();
            if (paths != null && paths.length > 0) {
                final java.util.List<TreePath> treePathsWithoutDuplicates = new ArrayList<>();
                for (TreePath path : paths) {
                    if (treePathsWithoutDuplicates.size() == 0) {
                        treePathsWithoutDuplicates.add(path);
                        continue;
                    }
                    // Filter out those paths which are contained in some parent paths.
                    boolean duplicate = false;
                    for (TreePath path1: treePathsWithoutDuplicates) {
                        if (path1.isDescendant(path)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (! duplicate) treePathsWithoutDuplicates.add(path);
                }
                final java.util.List<AbstractTreeNode> nodeList = new ArrayList<>();
                for (TreePath path : paths) {
                    AbstractTreeNode node = (AbstractTreeNode) path.getLastPathComponent();
                    if (node.asAssertion() instanceof BlankAssertion) {
                        continue;
                    }
                    nodeList.add(node);
                }
                return new PolicyTransferable(nodeList.toArray(new AbstractTreeNode[nodeList.size()]));
            } else {
                // No selection, so copy entire policy
                Object node = PolicyDiffTree.this.getModel().getRoot();
                if (node == null) return null;

                if (node instanceof AbstractTreeNode) {
                    return new PolicyTransferable(new AbstractTreeNode[] {(AbstractTreeNode)node});
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Highlight some rows in the policy tree with four different colors based on the diff results.
     * Here is color and DiffType mapping:
     * Light Green: DiffType.INSERTED
     * Light Red  : DiffType.DELETED
     * Light Blue : DiffType.MATCHED_WITH_DIFFERENCES
     * Light Grey : null
     * Note: DiffType.IDENTICAL will map to no color highlight.
     *
     * @param g: the graphic object to paint colors for some rows
     * @param tree: the policy diff tree
     * @param diffResultMap: the diff result map
     * @param visibleRowRange: the row range with visibility.  Any row out of this range will not be highlighted.
     */
    private void highlightDiffTree(final Graphics g, final JTree tree, final Map<Integer, DiffType> diffResultMap, final Pair<Integer, Integer> visibleRowRange) {
        if (g == null || tree == null || diffResultMap == null || visibleRowRange == null) return;

        g.setColor(tree.getBackground());
        g.fillRect(0,0,getWidth(),getHeight());

        DiffType diffType;
        Rectangle rectangle;
        Color color;

        // Any row out of this range will not be processed.
        for (int row = visibleRowRange.left; row <= visibleRowRange.right; row++) {
            diffType = diffResultMap.get(row);
            if (diffType == null) {
                color = COLOR_FOR_BLANK_ASSERTION;
            } else {
                switch (diffType) {
                    case DELETED:
                        color = COLOR_FOR_DELETION;
                        break;
                    case INSERTED:
                        color = COLOR_FOR_INSERTION;
                        break;
                    case IDENTICAL:
                        color = null;
                        break;
                    case MATCHED_WITH_DIFFERENCES:
                        color = COLOR_FOR_MATCH_WITH_DIFFERENCES;
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid diff type: " + diffType.toString());
                }
            }

            // Use the diff color to draw highlight
            rectangle = tree.getRowBounds(row);
            if (color != null && rectangle != null) {
                g.setColor(color);
                g.fillRect(0, rectangle.y, getWidth(), rectangle.height);
            }
        }

        // Use the selection color to draw selection
        if (tree.getSelectionCount() > 0) {
            for(int row: tree.getSelectionRows()) {
                rectangle = tree.getRowBounds(row);

                g.setColor(COLOR_FOR_SELECTION);
                g.fillRect(0, rectangle.y, getWidth(), rectangle.height);
            }
        }
    }
}