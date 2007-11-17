/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.XpathBasedAssertionPropertiesAction;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Abstract superclass for XpathBasedAssertion policy nodes
 */
public abstract class XpathBasedAssertionTreeNode extends LeafAssertionTreeNode<XpathBasedAssertion> {

    public XpathBasedAssertionTreeNode(XpathBasedAssertion assertion) {
        super(assertion);
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
        java.util.List<Action> list = new ArrayList<Action>();
        if (assertion instanceof PrivateKeyable) {
            list.add(new EditKeyAliasForAssertion(this));
        }
        if (assertion instanceof SecurityHeaderAddressable) {
            list.add(new EditXmlSecurityRecipientContextAction(this));
        }
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[]{});
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
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}