package com.l7tech.console.tree;

import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.IconManager;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Console custom <CODE>TreeCellRenderer</CODE>.
 * Labels, and the image icon for tree nodes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class EntityTreeCellRenderer
  extends DefaultTreeCellRenderer {

    /**
     * default constructor
     */
    public EntityTreeCellRenderer() {
    }

    /**
     * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent
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
        if (!(value instanceof AbstractTreeNode)) return this;

        AbstractTreeNode assertionTreeNode = ((AbstractTreeNode) value);
        setText(assertionTreeNode.getName());
        icon = expanded ?
          new ImageIcon(assertionTreeNode.getOpenedIcon()) :
          new ImageIcon(assertionTreeNode.getIcon());

        if (icon != null) {
            setIcon(icon);
        }
        return this;
    }

}
