package com.l7tech.console.tree.policy;

import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.gui.util.ImageCache;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.*;


/**
 * Default <CODE>TreeCellRenderer</CODE> implementaiton that handles
 * <code>AbstractTreeNode</code> nodes.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyTreeCellRenderer extends DefaultTreeCellRenderer {
    private final WeakHashMap<AssertionTreeNode, Boolean> includedCache = new WeakHashMap<AssertionTreeNode, Boolean>();

    private final Font boldFont;
    private final Font plainFont;
    private final Font italicFont;

    private boolean validated = true;
    private boolean enabled = true;

    // The policy version will be used to get policy goid and policy version number to find a property determining if showing assertion comments.
    private PolicyVersion policyVersion;

    /**
     * default constructor
     */
    public PolicyTreeCellRenderer() {
        JLabel l = new JLabel();
        boldFont = l.getFont().deriveFont(Font.BOLD);
        plainFont = l.getFont().deriveFont(Font.PLAIN);
        italicFont = l.getFont().deriveFont(Font.ITALIC);
    }

    public void setPolicyVersion(PolicyVersion policyVersion) {
        this.policyVersion = policyVersion;
    }

    /**
     * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent
     */
    public Component
      getTreeCellRendererComponent(JTree tree, Object value,
                                   boolean sel, boolean expanded,
                                   boolean leaf, int row, boolean hasFocus) {
        validated = true;
        enabled = true;
        super.getTreeCellRendererComponent(tree, value,  sel, expanded, leaf, row, hasFocus);

        setFont(plainFont);
        this.setBackgroundNonSelectionColor(tree.getBackground());
        if (!(value instanceof AssertionTreeNode)) return this;
        AssertionTreeNode node = ((AssertionTreeNode)value);
        setText(getNodeName(node));
        validated = node.getValidatorMessages().isEmpty();
        enabled = isIncluded(node)? node.asAssertion().isEnabled(): node.isAssertionEnabled();
        setToolTipText(node.getTooltipText());

        Image image =expanded ? node.getOpenedIcon() : node.getIcon();
        setIcon(image == null ? null : new ImageIcon(image));

        if ((!enabled) || isIncluded(node)) {
            setForeground(Color.GRAY);
            if ((! node.asAssertion().isEnabled()) || isIncluded(node)) {
                setFont(italicFont);
            }
        } else {
            setForeground( sel ? getTextSelectionColor() : getTextNonSelectionColor() );
            setFont(isRoutingAssertionNode(node) ? boldFont : plainFont);
        }

        return this;
    }

    public void paint(Graphics g) {
        super.paint(g);

        // If the assertion is disabled, then draw the red cross sign.
        if (!enabled) {
            Image image = new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/RedCrossSign16.gif")).getImage();
            g.drawImage(image, 0, 0, null);
        }
    }

    /**
     * Get the node name, which contains assertion comments depending on a property of showing comments or not.
     * If policyVersion is defined, then the property will be retrieved by the policy version.  Otherwise, the
     * property will be retrieved by a policy editor panel, which has a policy version information.
     *
     * @param node: the tree node whose name will be generated.
     * @return the node name
     */
    private String getNodeName(AssertionTreeNode node) {
        if (policyVersion != null) {
            // Get node name. Pass in decorate = false, so that it won't be decorated and comments won't be added.
            String name = node.getName(false);
            // Decorate without adding comments.
            name = AssertionUtils.decorateName(node.asAssertion(), name);
            // Add comment based on policy version.
            return DefaultAssertionPolicyNode.addCommentToDisplayTextByPolicyVersion(node.asAssertion(), name, policyVersion);
        } else {
            return node.getName();
        }
    }

    private boolean isIncluded(AssertionTreeNode node) {
        Boolean included = includedCache.get(node);
        if (included == null) {
            included = false;
            for (TreeNode treeNode : node.getPath()) {
                if (treeNode instanceof IncludeAssertionPolicyNode && treeNode != node) {
                    included = true;
                    break;
                }         
            }
            includedCache.put(node, included);
        }
        return included;
    }

    private boolean isRoutingAssertionNode(AssertionTreeNode node) {
        if  (node instanceof HttpRoutingAssertionTreeNode || node instanceof JmsRoutingAssertionTreeNode)
            return true;

        Assertion ass = node.asAssertion();
        return ass instanceof RoutingAssertion;
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

        SwingUtilities.layoutCompoundLabel(
          this, fm, getText(), getIcon(),
          getVerticalAlignment(), getHorizontalAlignment(),
          getVerticalTextPosition(),
          getHorizontalTextPosition(), viewRect, new
            Rectangle(), textRect,
          getText() == null ? 0 :
          (Integer)UIManager.get("Button.textIconGap"));

        g.fillRect(textRect.x + (Integer)UIManager.get("Button.textShiftOffset") - 4,
          textRect.y + fm.getAscent() + (Integer)UIManager.get("Button.textShiftOffset") + 2,
          textRect.width, 1);
    }


    protected void squigglyText(Graphics g) {
        Insets i = getInsets();
        FontMetrics fm = g.getFontMetrics();
        g.setColor(Color.RED);
        Rectangle textRect = new Rectangle();
        Rectangle viewRect = new Rectangle(i.left, i.top, getWidth() -
          (i.right + i.left), getHeight() - (i.bottom + i.top));

        SwingUtilities.layoutCompoundLabel(
          this, fm, getText(), getIcon(),
          getVerticalAlignment(), getHorizontalAlignment(),
          getVerticalTextPosition(),
          getHorizontalTextPosition(), viewRect, new
            Rectangle(), textRect,
          getText() == null ? 0 :
          (Integer)UIManager.get("Button.textIconGap"));

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
