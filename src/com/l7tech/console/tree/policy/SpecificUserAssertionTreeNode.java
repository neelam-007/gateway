package com.l7tech.console.tree.policy;


import com.l7tech.console.util.IconManager2;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * Class SpecificUserAssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
class SpecificUserAssertionTreeNode extends LeafAssertionTreeNode {

    public SpecificUserAssertionTreeNode(IdentityAssertion assertion) {
        super(assertion);
    }

    /**
     * loads the icon specified by subclass iconResource()
     * implementation.
     *
     * @return the <code>ImageIcon</code> or null if not found
     */
    public Icon getIcon() {
        Image image = IconManager2.getInstance().getIcon(iconResource(false));
        if (image !=null) {
            return new ImageIcon(image);
        }
        return null;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
      return "com/l7tech/console/resources/user16.png";
    }
}