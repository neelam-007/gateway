package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.console.action.DeleteAssertionAction;
import com.l7tech.console.action.XmlEncPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class <code>XmlEncAssertionTreeNode</code> specifies the policy
 * element that represents the XML encryption.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XmlEncAssertionTreeNode extends LeafAssertionTreeNode {

    public XmlEncAssertionTreeNode(Assertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "XML message encryption";
    }

    /**
       * Get the set of actions associated with this node.
       * This may be used e.g. in constructing a context menu.
       *
       * @return actions appropriate to the node
       */
      public Action[] getActions() {
          java.util.List list = new ArrayList();
          Action a = new XmlEncPropertiesAction(this);
          list.add(a);
          list.addAll(Arrays.asList(super.getActions()));
          return (Action[])list.toArray(new Action[]{});
      }


    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}