package com.l7tech.console.tree;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A JTree holds assertion line numbers.
 *
 * @author ghuang
 */
public class AssertionLineNumbersTree extends JTree {
    public final static String BLANK_ICON_FILE_NAME = "com/l7tech/console/resources/Transparent16.png";
    public final static boolean DEFAULT_VISIBILITY = false;

    private PolicyTree policyTree;
    private Functions.Nullary<Boolean> checkingFunction;

    public AssertionLineNumbersTree(PolicyTree policyTree) {
        this.policyTree = policyTree;
        initialize();
        registerPolicyTree();
        setVisible(DEFAULT_VISIBILITY);
    }

    private void initialize() {
        setRootVisible(false);
        setShowsRootHandles(false);
        setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setAlignmentY(Component.TOP_ALIGNMENT);
        setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value,  sel, expanded, leaf, row, hasFocus);

                Font plainFont = new JLabel().getFont().deriveFont(Font.PLAIN);
                setFont(plainFont);

                ImageIcon icon = new ImageIcon(ImageCache.getInstance().getIcon(BLANK_ICON_FILE_NAME));
                setIcon(icon);

                return this;
            }
        });

        updateOrdinalsDisplaying(true);
    }

    /**
     * Add listeners to listen PolicyTree changes in order to restructure AssertionLineNumbersTree
     */
    public void registerPolicyTree() {
        if (policyTree == null) return;
        policyTree.removeTreeExpansionListener(policyTreeExpansionListener);
        policyTree.addTreeExpansionListener(policyTreeExpansionListener);

        if (policyTree.getModel() == null) return;
        policyTree.getModel().removeTreeModelListener(policyTreeModelListener);
        policyTree.getModel().addTreeModelListener(policyTreeModelListener);
    }

    /**
     * Update the assertion-line-numbers tree due to the changes of policy tree structure or policy tree nodes expanding/collapsing.
     * @param forInitial: a flag to indicate whether the tree is to be initialized.  If it is true, then the tree will be restructured.
     */
    public void updateOrdinalsDisplaying(boolean forInitial) {
        // Check whether it is really needed to update the assertion line numbers tree.
        if (!forInitial && checkingFunction != null) {
            boolean lnShown = checkingFunction.call();
            if (! lnShown) return;
        }

        // Get new ordinals
        List<String> ordinals = getOrdinals();

        // Generate a new tree model
        DefaultMutableTreeNode root = createRootForAssertionLineNumbersTree(ordinals);
        TreeModel newTreeModel = new DefaultTreeModel(root);

        // Update the AssertionLineNumbersTree
        setModel(newTreeModel);
    }

    /**
     * Set a checking function to check whether the tree needs updating or not.
     * @param checkingFunction: a function implemented in PolicyEditorPanel.
     */
    public void setCheckingLineNumbersShownFunction(Functions.Nullary<Boolean> checkingFunction) {
        this.checkingFunction = checkingFunction;
    }

    private DefaultMutableTreeNode createRootForAssertionLineNumbersTree(List<String> ordinals) {
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode();
        for (String ordinal: ordinals) {
            newRoot.add(new DefaultMutableTreeNode(ordinal));
        }
        return newRoot;
    }

    private List<String> getOrdinals() {
        List<String> lineNumbersList = new ArrayList<String>();
        if (policyTree == null) return lineNumbersList;

        for (int row = 0; row < policyTree.getRowCount(); row++) {
            TreePath path = policyTree.getPathForRow(row);
            AssertionTreeNode node = (AssertionTreeNode) path.getLastPathComponent();

            lineNumbersList.add(AssertionTreeNode.getVirtualOrdinalString(node));
        }
        return lineNumbersList;
    }

    private TreeExpansionListener policyTreeExpansionListener = new TreeExpansionListener() {
        @Override
        public void treeExpanded(TreeExpansionEvent event) {
            updateOrdinalsDisplaying(false);
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            updateOrdinalsDisplaying(false);
        }
    };

    private TreeModelListener policyTreeModelListener = new TreeModelListener() {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            updateOrdinalsDisplaying(false);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            updateOrdinalsDisplaying(false);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            updateOrdinalsDisplaying(false);
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            updateOrdinalsDisplaying(false);
        }
    };
}
