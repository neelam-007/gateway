package com.l7tech.console.tree.policy;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Default <CODE>TreeCellRenderer</CODE> implementaiton that handles
 * <code>AbstractTreeNode</code> nodes.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyTreeCellRenderer extends DefaultTreeCellRenderer {
    private final Font boldFont;
    private final Font plainFont;
    private boolean validated = true;
    /**
     * default constructor
     */
    public PolicyTreeCellRenderer() {
        JLabel l = new JLabel();
        boldFont = l.getFont().deriveFont(Font.BOLD);
        plainFont = l.getFont().deriveFont(Font.PLAIN);
    }

    /**
     * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent
     */
    public Component
      getTreeCellRendererComponent(JTree tree, Object value,
                                   boolean sel, boolean expanded,
                                   boolean leaf, int row, boolean hasFocus) {
        validated = true;
        super.getTreeCellRendererComponent(tree, value,  sel, expanded, leaf, row, hasFocus);

        setFont(plainFont);
        this.setBackgroundNonSelectionColor(tree.getBackground());
        Icon icon = null;
        if (!(value instanceof AssertionTreeNode)) return this;
        AssertionTreeNode node = ((AssertionTreeNode)value);
        setText(node.getName());
        validated = node.getValidatorMessages().isEmpty();
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

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!validated) {
            squigglyText(g);
        }
    }

    protected void underlineText(Graphics g) {
        Insets i = getInsets();
        FontMetrics fm = g.getFontMetrics();

        Rectangle textRect = new Rectangle();
        Rectangle viewRect = new Rectangle(i.left, i.top, getWidth() -
          (i.right + i.left), getHeight() - (i.bottom + i.top));

        String text = SwingUtilities.layoutCompoundLabel(
          this, fm, getText(), getIcon(),
          getVerticalAlignment(), getHorizontalAlignment(),
          getVerticalTextPosition(),
          getHorizontalTextPosition(), viewRect, new
            Rectangle(), textRect,
          getText() == null ? 0 :
          ((Integer)UIManager.get("Button.textIconGap")).intValue());

        g.fillRect(textRect.x + ((Integer)UIManager.get("Button.textShiftOffset")).intValue() - 4,
          textRect.y + fm.getAscent() + ((Integer)UIManager.get("Button.textShiftOffset")).intValue() + 2,
          textRect.width, 1);
    }


    protected void squigglyText(Graphics g) {
        Insets i = getInsets();
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.RED);
        Rectangle textRect = new Rectangle();
        Rectangle viewRect = new Rectangle(i.left, i.top, getWidth() -
          (i.right + i.left), getHeight() - (i.bottom + i.top));

        String text = SwingUtilities.layoutCompoundLabel(
          this, fm, getText(), getIcon(),
          getVerticalAlignment(), getHorizontalAlignment(),
          getVerticalTextPosition(),
          getHorizontalTextPosition(), viewRect, new
            Rectangle(), textRect,
          getText() == null ? 0 :
          ((Integer)UIManager.get("Button.textIconGap")).intValue());

        int x = textRect.x;
        int y = textRect.y + textRect.height;
        boolean up = false;
        int a = 0;
        while (x < textRect.width +textRect.x) {
            if (up) a++; else a--;
            if (a != 0) up = !up;
            y += a;
            g.drawLine(x, y, x, y);
            x++;
        }
    }
}