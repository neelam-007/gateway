package com.l7tech.console.tree.policy;


import com.l7tech.console.action.XpathBasedAssertionPropertiesAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertionBase;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Tree node for XpathBasedAssertion.
 *
 * @author flascell
 */
public abstract class XpathBasedAssertionTreeNode extends LeafAssertionTreeNode {

    public XpathBasedAssertionTreeNode(Assertion assertion) {
        super(assertion);
        this.assertion = (XpathBasedAssertion)assertion;
    }

    /** Get the basic name of this node, ie "XML Request Security". */
    public abstract String getBaseName();

    public String getName() {
        return getBaseName();
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = XpathBasedAssertionPropertiesAction.actionForNode(this);
        list.add(a);
        if (assertion instanceof XmlSecurityAssertionBase) {
            list.add(new EditXmlSecurityRecipientContextAction(this));
        }
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[]{});
    }

    /**
       * Gets the default action for this node.
       *
       * @return <code>null</code> indicating there should be none default action
       */
      public Action getPreferredAction() {
          return XpathBasedAssertionPropertiesAction.actionForNode(this);
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

    private XpathBasedAssertion assertion;
}