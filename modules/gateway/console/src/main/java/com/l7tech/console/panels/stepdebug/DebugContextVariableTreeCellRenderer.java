package com.l7tech.console.panels.stepdebug;

import com.l7tech.gateway.common.stepdebug.DebugContextVariableData;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * The tree cell renderer for the context variable tree.
 */
public class DebugContextVariableTreeCellRenderer extends DefaultTreeCellRenderer {
    /**
     * Creates <code>DebugContextVariableTreeCellRenderer</code>.
     */
    DebugContextVariableTreeCellRenderer() {
        super();
        this.setClosedIcon(null);
        this.setOpenIcon(null);
        this.setLeafIcon(null);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value,  sel, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) value;
        if (!(treeNode.getUserObject() instanceof DebugContextVariableData)) {
            return this;
        }

        // Display user added context variable in a different color.
        //
        DebugContextVariableData data = (DebugContextVariableData) treeNode.getUserObject();
        if (!data.getIsUserAdded()) {
            this.setForeground(null);
        } else {
            this.setForeground(Color.BLUE);
        }

        return this;
    }
}