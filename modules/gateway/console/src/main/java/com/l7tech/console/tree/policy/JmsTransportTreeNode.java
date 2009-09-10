/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.JmsTransportAssertion;

/**
 * @author alex
 */
public class JmsTransportTreeNode extends LeafAssertionTreeNode<JmsTransportAssertion> {
    public JmsTransportTreeNode(JmsTransportAssertion assertion) {
        super(assertion);
    }

    public String getName(final boolean decorate) {
        return "Message received using JMS";
    }

    protected String iconResource( boolean open ) {
        return "com/l7tech/console/resources/interface.gif";
    }
}
