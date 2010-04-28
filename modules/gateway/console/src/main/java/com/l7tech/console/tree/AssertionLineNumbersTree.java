package com.l7tech.console.tree;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.gui.util.ImageCache;

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
    public final static String NAME = "assertion.line.numbers.tree";
    public final static String BLANK_ICON_FILE_NAME = "com/l7tech/console/resources/Transparent16.png";
    public final static boolean DEFAULT_VISIBILITY = false;

    private PolicyTree policyTree;

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

        updateOrdinalsDisplaying();
    }

    public void registerPolicyTree() {
        if (policyTree == null) return;

        policyTree.removeTreeExpansionListener(policyTreeExpansionListener);
        policyTree.removeTreeSelectionListener(treeSelectionListener);
        policyTree.addTreeExpansionListener(policyTreeExpansionListener);
        policyTree.addTreeSelectionListener(treeSelectionListener);

        if (policyTree.getModel() == null) return;

        policyTree.getModel().removeTreeModelListener(policyTreeModelListener);
        policyTree.getModel().addTreeModelListener(policyTreeModelListener);
    }

    public void updateOrdinalsDisplaying() {
        List<String> ordinals = getOrdinals();
        DefaultMutableTreeNode root = createRootForAssertionLineNumbersTree(ordinals);
        TreeModel newTreeModel = new DefaultTreeModel(root);
        setModel(newTreeModel);
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
            updateOrdinalsDisplaying();
        }

        @Override
        public void treeCollapsed(TreeExpansionEvent event) {
            updateOrdinalsDisplaying();
        }
    };

    private TreeSelectionListener treeSelectionListener = new TreeSelectionListener() {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            updateOrdinalsDisplaying();
        }
    };

    private TreeModelListener policyTreeModelListener = new TreeModelListener() {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            updateOrdinalsDisplaying();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            updateOrdinalsDisplaying();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            updateOrdinalsDisplaying();
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            updateOrdinalsDisplaying();
        }
    };
}
