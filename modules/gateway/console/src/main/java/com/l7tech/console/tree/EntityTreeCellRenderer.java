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
    private final Font italicsFont;

    /**
     * default constructor
     */
    public EntityTreeCellRenderer() {
        JLabel l = new JLabel();
        boldFont = l.getFont().deriveFont(Font.BOLD);
        plainFont =  l.getFont().deriveFont(Font.PLAIN);
        italicsFont = l.getFont().deriveFont(Font.ITALIC);
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

        this.setBackgroundNonSelectionColor(tree.getBackground());
        Icon icon;
        if (!(value instanceof AbstractTreeNode)) return this;

        AbstractTreeNode node = ((AbstractTreeNode)value);
        if(node.isCut()){
            setFont(italicsFont);
        }else{
            setFont(plainFont);
        }

        setText(node.getName());

        setToolTipText(node.getTooltipText());

        Image image = expanded ? node.getOpenedIcon() : node.getIcon();
        icon = image == null ? null : new ImageIcon(image);
        if (icon != null) {
            setIcon(icon);
        }
        if (node instanceof HttpRoutingAssertionTreeNode || node instanceof JmsRoutingAssertionTreeNode) {
            setFont(boldFont);
        }

        int labelHeight = this.getPreferredSize().height;
        int treeRowHeight = tree.getRowHeight();
        if (labelHeight > treeRowHeight) tree.setRowHeight(labelHeight);

        return this;
    }
}