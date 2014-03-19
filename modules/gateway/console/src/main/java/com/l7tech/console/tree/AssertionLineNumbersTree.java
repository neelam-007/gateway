package com.l7tech.console.tree;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.gui.util.ImageCache;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * A JTree holds assertion line numbers.
 *
 * @author ghuang
 */
public class AssertionLineNumbersTree extends JTree {
    public final static String BLANK_ICON_FILE_NAME = "com/l7tech/console/resources/Transparent16x1.png";
    public final static boolean DEFAULT_VISIBILITY = false;

    private JTree policyTree;

    public AssertionLineNumbersTree( final JTree policyTree ) {
        this.policyTree = policyTree;
        initialize();
        registerPolicyTree();
    }

    private void initialize() {
        setVisible(DEFAULT_VISIBILITY);
        setRootVisible(false);
        setShowsRootHandles(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setAlignmentY(Component.TOP_ALIGNMENT);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon( new ImageIcon(ImageCache.getInstance().getIcon(BLANK_ICON_FILE_NAME)) );
        renderer.setFont( new JLabel().getFont().deriveFont( Font.PLAIN ) );
        setCellRenderer(renderer);
        setSelectionModel( null );

        updateOrdinalsDisplaying();
    }

    /**
     * Add listeners to listen PolicyTree changes in order to restructure AssertionLineNumbersTree
     */
    private void registerPolicyTree() {
        if (policyTree == null) return;
        policyTree.removeTreeExpansionListener(policyTreeExpansionListener);
        policyTree.addTreeExpansionListener(policyTreeExpansionListener);

        if ( policyTree.getModel() != null ) {
            policyTree.getModel().removeTreeModelListener(policyTreeModelListener);
            policyTree.getModel().addTreeModelListener(policyTreeModelListener);
        }

        policyTree.addPropertyChangeListener( new PropertyChangeListener(){
            @Override
            public void propertyChange( final PropertyChangeEvent evt ) {
                if ( JTree.TREE_MODEL_PROPERTY.equals(evt.getPropertyName()) ) {
                    if (policyTree.getModel() != null) {
                        policyTree.getModel().removeTreeModelListener(policyTreeModelListener);
                        policyTree.getModel().addTreeModelListener(policyTreeModelListener);
                    }

                    updateOrdinalsDisplaying();
                }
            }
        } );
    }

    /**
     * Update the assertion-line-numbers tree due to the changes of policy tree structure or policy tree nodes expanding/collapsing.
     */
    public void updateOrdinalsDisplaying() {
        // Check whether it is really needed to update the assertion line numbers tree.
        if (!this.isVisible()) {
            return;
        }

        // Get new ordinals
        List<String> ordinals = getOrdinals();

        // Generate a new tree model
        DefaultMutableTreeNode root = createRootForAssertionLineNumbersTree(ordinals);
        TreeModel newTreeModel = new DefaultTreeModel(root);

        // Update the AssertionLineNumbersTree
        setModel(newTreeModel);
    }

    private DefaultMutableTreeNode createRootForAssertionLineNumbersTree(List<String> ordinals) {
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode();
        for ( final String ordinal: ordinals ) {
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

    private TreeModelListener policyTreeModelListener = new TreeModelListener() {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            update();
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            update();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            update();
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            update();
        }

        private void update() {
            // Allow node insertion/removal to complete before updating
            SwingUtilities.invokeLater( new Runnable(){
                @Override
                public void run() {
                    updateOrdinalsDisplaying();
                }
            } );
        }
    };
}
