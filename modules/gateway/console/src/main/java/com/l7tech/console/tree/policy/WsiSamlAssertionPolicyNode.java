/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.WsiSamlAssertionPropertiesAction;
import com.l7tech.policy.assertion.WsiSamlAssertion;

import javax.swing.*;

/**
 * Policy node for WSI-SAML Token Profile assertion.
 */
public class WsiSamlAssertionPolicyNode extends LeafAssertionTreeNode<WsiSamlAssertion> {

    //- PUBLIC

    public WsiSamlAssertionPolicyNode(WsiSamlAssertion assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "WS-I SAML Compliance";
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new WsiSamlAssertionPropertiesAction(this);
    }

    //- PROTECTED

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }
}
