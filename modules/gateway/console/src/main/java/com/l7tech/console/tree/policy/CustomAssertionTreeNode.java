/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.gui.util.ImageCache;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.console.action.CustomAssertionPropertiesAction;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;

import javax.swing.*;
import java.awt.*;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>CustomAssertionTreeNode</code> contains the custom
 * assertion element.
 */
public class CustomAssertionTreeNode extends LeafAssertionTreeNode {
    private static final Logger logger = Logger.getLogger(CustomAssertionTreeNode.class.getName());

    private CustomAssertionsRegistrar registrar;
    private Image defaultImageIcon;

    public CustomAssertionTreeNode(CustomAssertionHolder assertion) {
        super(assertion);
        registrar = Registry.getDefault().getCustomAssertionsRegistrar();

    }

    /**
     * @return the node name that is displayed
     */
    @Override
    public String getName(final boolean decorate) {
        return DefaultAssertionPolicyNode.getNameFromMeta(asAssertion(), decorate, true);
    }

    /**
     * @return the custom assertion category
     */
    public Category getCategory() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        return cha.getCategory();
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    @Override
    public Action getPreferredAction() {
        // check whether this custom assertion should be allowed editing based on whether it has properties to edit
        try {
            BeanInfo bi = Introspector.getBeanInfo(((CustomAssertionHolder)asAssertion()).getCustomAssertion().getClass());
            PropertyDescriptor[] desc = bi.getPropertyDescriptors();
            for (int i = 0; i < desc.length; i++) {
                PropertyDescriptor propertyDescriptor = desc[i];
                if (!propertyDescriptor.getName().equals("class") && !propertyDescriptor.getName().equals("name")) {
                    return new CustomAssertionPropertiesAction(this);
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException("Could not introspect custom assertion", e);
        }
        return null;
    }

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    @Override
    public Image getIcon() {
        return getCustomAssertionIcon();
    }

    /**
      * loads the icon specified by subclass iconResource()
      * implementation.
      *
      * @return the <code>ImageIcon</code> or null if not found
      */
     @Override
     public Image getOpenedIcon() {
         return getCustomAssertionIcon();
     }


    private Image getCustomAssertionIcon() {
        if (defaultImageIcon != null) {
            return defaultImageIcon;
        }

        CustomAssertion customAssertion = ((CustomAssertionHolder)asAssertion()).getCustomAssertion();
        try {
            CustomAssertionUI ui = registrar.getUI(customAssertion.getClass().getName());
            if(ui!=null) {
                ImageIcon icon = ui.getSmallIcon();
                if (icon != null) {
                    defaultImageIcon = icon.getImage();
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to get the custom assertion icon", e);
        } finally {
            if(defaultImageIcon==null)
                defaultImageIcon = ImageCache.getInstance().getIcon(iconResource(false));
        }

        return defaultImageIcon;
    }


    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return assertion.meta().get(AssertionMetadata.POLICY_NODE_ICON).toString();
    }
}
