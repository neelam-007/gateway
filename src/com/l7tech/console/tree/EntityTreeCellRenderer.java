package com.l7tech.console.tree;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Default <CODE>TreeCellRenderer</CODE> implementaiton that handles
 * <code>AbstractTreeNode</code> nodes.
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

        AbstractTreeNode node = ((AbstractTreeNode)value);
        setText(node.getName());
        icon = expanded ?
          new ImageIcon(node.getOpenedIcon()) :
          new ImageIcon(node.getIcon());

        if (icon != null) {
            setIcon(icon);
        }
        return this;
    }

}
