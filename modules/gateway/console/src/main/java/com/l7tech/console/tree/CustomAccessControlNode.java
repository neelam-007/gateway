package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class represents an custom access control gui node element in the
 * policy tree.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.1
 */
public class CustomAccessControlNode extends DefaultAssertionPaletteNode<CustomAssertionHolder> implements Comparable<AbstractTreeNode> {
    private static final Logger logger = Logger.getLogger(CustomAccessControlNode.class.getName());

    private CustomAssertionsRegistrar registrar;
    private Image defaultImageIcon;
    private String name;
    private String description;
    private final Assertion assertion;

    /**
     * construct the <CODE>CustomAccessControlNode</CODE> instance for a given
     * <CODE>CustomAssertionHolder</CODE>
     *
     * @param ca the e represented by this <CODE>EntityHeaderNode</CODE>
     */
    public CustomAccessControlNode(CustomAssertionHolder ca) {
        super(ca);
        assertion = ca;
        setAllowsChildren(false);
        this.registrar = Registry.getDefault().getCustomAssertionsRegistrar();
    }

    /**
     * @return always false, custom assertions cannot be deleted from the palette
     */
    @Override
    public boolean isLeaf() {
        return true;
    }

    /**
     * Test if the node can be deleted. Default for entites
     * is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    @Override
    public boolean canDelete() {
        return false;
    }

    /**
     * @return the assertion this node represents
     */
    @Override
    public Assertion asAssertion() {
        return assertion;
    }

    /**
     * subclasses override this method
     */
    @Override
    protected void doLoadChildren() {
    }

    @Override
    public String getName() {
        if ( name == null ) {
            CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
            final CustomAssertion ca = cha.getCustomAssertion();
            String assName = ca.getName();
            if (assName == null) {
                assName = "Unspecified custom assertion (class '" + ca.getClass() + "')";
            }
            name = assName;
        }
        return name;
    }

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    @Override
    public Image getIcon() {
        return getCutomAssertionIcon();
    }

    private Image getCutomAssertionIcon() {
        if (defaultImageIcon !=null) {
            return defaultImageIcon;
        }
        CustomAssertion customAssertion = ((CustomAssertionHolder)asAssertion()).getCustomAssertion();
        CustomAssertionUI ui = registrar.getUI(customAssertion.getClass().getName());
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
    @Override
    public Image getOpenedIcon() {
        return getCutomAssertionIcon();
    }

    @Override
    protected String iconResource(boolean open) {
        return assertion.meta().get(AssertionMetadata.PALETTE_NODE_ICON).toString();
    }

    /**
     * Override toString
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName()).append("\n").
          append(super.toString());
        return sb.toString();
    }

    public String getDescriptionText() {
        if ( description == null ) {
            String assDesc = ((CustomAssertionHolder)asAssertion()).getDescriptionText();
            description = assDesc == null ? "" : assDesc;
        }
        return description;
    }

    @Override
    public int compareTo( final AbstractTreeNode treeNode ) {
        return String.CASE_INSENSITIVE_ORDER.compare(getName(),treeNode.getName());
    }
}
