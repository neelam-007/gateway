package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.console.util.IconManager2;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.*;
import java.util.Iterator;
import java.awt.*;

/**
 * Class SpecificUserAssertionTreeNode.
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a> 
 */
class SslAssertionTreeNode extends LeafAssertionTreeNode {

    public SslAssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
      return "com/l7tech/console/resources/encryption.gif";
    }
}