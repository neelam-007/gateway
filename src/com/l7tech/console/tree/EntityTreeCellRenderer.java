package com.l7tech.console.tree;

import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.IconManager;

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
        if (value instanceof EntityTreeNode) {
            BasicTreeNode node = (BasicTreeNode)((EntityTreeNode)value).getUserObject();
            setText(node.getName());
            icon = IconManager.getInstance().getIcon(node);

        } else if (value instanceof WsdlTreeNode) {
            WsdlTreeNode node = (WsdlTreeNode)value;
            setText(node.toString());
            icon = new ImageIcon(node.getIcon());
        }
        if (icon !=null) {
            setIcon(icon);
        }
        return this;
    }

}
