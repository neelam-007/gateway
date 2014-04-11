package com.l7tech.console.panels.stepdebug;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.gui.util.ImageCache;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * The tree cell renderer for {@link AssertionBreakpointsTree}. Displays breakpoint icon if breakpoint
 * is set for a given assertion in policy.
 */
public class AssertionBreakpointsTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final Icon BREAKPOINT_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Stop16.gif"));
    private static final Icon BLANK_ICON = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/Transparent16.png"));

    /**
     * Creates <code>AssertionBreakpointsTreeCellRenderer</code>.
     */
    AssertionBreakpointsTreeCellRenderer() {
        super();
        this.setTextSelectionColor(null);
        this.setBackgroundSelectionColor(null);
        this.setBorderSelectionColor(null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value,  sel, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
        if (!(treeNode.getUserObject() instanceof AssertionTreeNode)) {
            return this;
        }

        AssertionTreeNode node = (AssertionTreeNode) treeNode.getUserObject();
        AssertionBreakpointsTree castTree = (AssertionBreakpointsTree) tree;
        if (castTree.isBreakpointSet(node)) {
            setIcon(BREAKPOINT_ICON);
        } else {
            setIcon(BLANK_ICON);
        }
        setText(null);

        return this;
    }
}