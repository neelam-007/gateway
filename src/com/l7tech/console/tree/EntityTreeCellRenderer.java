package com.l7tech.console.tree;

import com.l7tech.console.tree.policy.HttpRoutingAssertionTreeNode;
import com.l7tech.console.tree.policy.JmsRoutingAssertionTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Default <CODE>TreeCellRenderer</CODE> implementaiton that handles
 * <code>AbstractTreeNode</code> nodes.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class EntityTreeCellRenderer
  extends DefaultTreeCellRenderer {
    private final Font boldFont;
    private final Font plainFont;

    /**
     * default constructor
     */
    public EntityTreeCellRenderer() {
        JLabel l = new JLabel();
        boldFont = l.getFont().deriveFont(Font.BOLD);
        plainFont =  l.getFont().deriveFont(Font.PLAIN);
    }

    /**
     * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent
     */
    public Component
      getTreeCellRendererComponent(JTree tree, Object value,
                                   boolean sel, boolean expanded,
                                   boolean leaf, int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value,
                                           sel, expanded, leaf, row, hasFocus);

        setFont(plainFont);
        this.setBackgroundNonSelectionColor(tree.getBackground());
        Icon icon = null;
        if (!(value instanceof AbstractTreeNode)) return this;

        AbstractTreeNode node = ((AbstractTreeNode)value);
        setText(node.getName());

        setToolTipText(node.getTooltipText());

        icon = expanded ?
          new ImageIcon(node.getOpenedIcon()) :
          new ImageIcon(node.getIcon());

        if (icon != null) {
            setIcon(icon);
        }
        if (node instanceof HttpRoutingAssertionTreeNode || node instanceof JmsRoutingAssertionTreeNode) {
            setFont(boldFont);
        }
        return this;
    }

}
