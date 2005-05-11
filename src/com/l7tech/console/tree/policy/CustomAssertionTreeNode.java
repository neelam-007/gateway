package com.l7tech.console.tree.policy;


import com.l7tech.console.action.CustomAssertionPropertiesAction;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.common.gui.util.ImageCache;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.awt.*;

/**
 * Class <code>CustomAssertionTreeNode</code> contains the custom
 * assertion element.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertionTreeNode extends LeafAssertionTreeNode {
    private CustomAssertionsRegistrar registrar;
    private Image defaultImageIcon;

    public CustomAssertionTreeNode(CustomAssertionHolder assertion) {
        super(assertion);
        registrar = Registry.getDefault().getCustomAssertionsRegistrar();

    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        final CustomAssertion ca = cha.getCustomAssertion();
        String name = ca.getName();
        if (name == null) {
            name = "Unspecified custom assertion (class '" + ca.getClass() + "'";
        }
        return name;
    }

    /**
     * @return the custom assertion category
     */
    public Category getCategory() {
        CustomAssertionHolder cha = (CustomAssertionHolder)asAssertion();
        return cha.getCategory();
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
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
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = getPreferredAction();
        if (a != null) {
            list.add(a);
        }
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
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
    /**
      * loads the icon specified by subclass iconResource()
      * implementation.
      *
      * @return the <code>ImageIcon</code> or null if not found
      */
     public Image getOpenedIcon() {
         return getCutomAssertionIcon();
     }


    private Image getCutomAssertionIcon() {
        if (defaultImageIcon != null) {
            return defaultImageIcon;
        }
        CustomAssertion customAssertion = ((CustomAssertionHolder)asAssertion()).getCustomAssertion();
        CustomAssertionUI ui = registrar.getUI(customAssertion.getClass());
        if (ui == null) {
            defaultImageIcon = ImageCache.getInstance().getIcon(iconResource(false));
        } else {
            try {
                ImageIcon icon = ui.getSmallIcon();
                if (icon != null) {
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
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/custom.gif";
    }
}