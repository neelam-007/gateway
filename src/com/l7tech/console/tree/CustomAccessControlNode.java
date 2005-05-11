package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * The class represents an custom access control gui node element in the
 * policy tree.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class CustomAccessControlNode extends AbstractTreeNode {
    private CustomAssertionsRegistrar registrar;
    private Image defaultImageIcon;

    /**
     * construct the <CODE>CustomAccessControlNode</CODE> instance for a given
     * <CODE>CustomAssertionHolder</CODE>
     * 
     * @param ca the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public CustomAccessControlNode(CustomAssertionHolder ca) {
        super(ca);
        setAllowsChildren(false);
        this.registrar = Registry.getDefault().getCustomAssertionsRegistrar();
    }

    /**
     * @return always false, custom assertions cannot be deleted from the palette
     */
    public boolean isLeaf() {
        return true;
    }

    /**
     * Get the set of actions associated with this node.
     * This returns actions that are used buy entity nodes
     * such .
     * 
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{};
    }

    /**
     * Test if the node can be deleted. Default for entites
     * is <code>true</code>
     * 
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return false;
    }

    /**
     * @return the assertion this node represents
     */
    public Assertion asAssertion() {
        return (Assertion)super.getUserObject();
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {
    }

    public String getName() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        final CustomAssertion ca = cha.getCustomAssertion();
        String name = ca.getName();
        if (name == null) {
            name = "Unspecified custom assertion (class '" + ca.getClass() + "')";
        }
        return name;
    }

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    public Image getIcon() {
        return getCutomAssertionIcon();
    }

    private Image getCutomAssertionIcon() {
        if (defaultImageIcon !=null) {
            return defaultImageIcon;
        }
        CustomAssertion customAssertion = ((CustomAssertionHolder)asAssertion()).getCustomAssertion();
        CustomAssertionUI ui = registrar.getUI(customAssertion.getClass());
        if (ui == null) {
            defaultImageIcon = ImageCache.getInstance().getIcon(iconResource(false));
        } else {
            try {
                ImageIcon icon = ui.getSmallIcon();
                if (icon !=null) {
                    defaultImageIcon = icon.getImage();
                    return defaultImageIcon;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to get the custom assertion icon", e);
            }
            defaultImageIcon = ImageCache.getInstance().getIcon(iconResource(false));
        }

        return defaultImageIcon;
    }

    /**
     * Finds an icon for this node when opened. This icon should
     * represent the node only when it is opened (when it can have
     * children).
     *
     * @return icon to use to represent the bean when opened
     */
    public Image getOpenedIcon() {
        return getCutomAssertionIcon();
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/custom.gif";
    }

    /**
     * Override toString
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName() + "\n").
          append(super.toString());
        return sb.toString();
    }
}
