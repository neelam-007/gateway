package com.l7tech.console.tree;

import javax.swing.JTree;
import javax.swing.tree.*;

public class DirectoryBrowser extends JTree {

    public DirectoryBrowser(EntityTreeNode root, boolean asksAllowsChildren) {
        setDefaults();
        FilteredTreeModel m = new FilteredTreeModel(root);
        m.setAsksAllowsChildren(asksAllowsChildren);
        setModel(m);
    }

    public DirectoryBrowser(EntityTreeNode root) {
        this(root, true);
    }

    public void selectNode(EntityTreeNode node) {
        TreePath path = new TreePath(node.getPath());
        if (path != null) {
            setSelectionPath(path);
        }
    }

    /**
     * refresh the children under the node
     *
     * @param node   the node to refresh
     */
    private void refreshNode(EntityTreeNode node) {
        node.removeAllChildren();
        TreePath path = new TreePath(node.getPath());
        expandPath(path);
        setSelectionPath(path);
    }

    private void setDefaults() {
        setShowsRootHandles(true);
        setLargeModel(true);
        setCellRenderer(new EntityTreeCellRenderer());
        putClientProperty("JTree.lineStyle", "Angled");
        setUI(CustomTreeUI.getTreeUI());
    }
}
