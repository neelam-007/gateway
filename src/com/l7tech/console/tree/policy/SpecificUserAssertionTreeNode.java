package com.l7tech.console.tree.policy;


import com.l7tech.console.util.IconManager2;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;

import javax.swing.*;
import java.awt.*;

/**
 * Class SpecificUserAssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
class SpecificUserAssertionTreeNode extends LeafAssertionTreeNode {

    public SpecificUserAssertionTreeNode(SpecificUser assertion) {
        super(assertion);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/user16.png";
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Identity is "+ ((SpecificUser)getUserObject()).getUser().getName();
    }
}