package com.l7tech.console.tree.policy;

import com.l7tech.console.util.IconManager;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Console custom <CODE>AssertionNodeCellRenderer</CODE>.
 * Labels, and the image icon for tree nodes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class AssertionNodeCellRenderer
  extends DefaultTreeCellRenderer {

    /**
     * default constructor
     */
    public AssertionNodeCellRenderer() {
    }

    /**
     * @see DefaultTreeCellRenderer#getTreeCellRendererComponent
     */
    public Component
      getTreeCellRendererComponent(JTree tree, Object value,
                                   boolean sel, boolean expanded,
                                   boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value,
          sel, expanded,
          leaf, row, hasFocus);

        this.setBackgroundNonSelectionColor(tree.getBackground());
        Icon icon = null;
        if (value instanceof AssertionTreeNode) {
            Assertion ass = (Assertion)((AssertionTreeNode)value).getUserObject();
            setText(ass.getClass().getName());
            icon = IconManager.getInstance().getIcon(ass);
        }
        if (icon !=null) {
            setIcon(icon);
        }
        return this;
    }

}
