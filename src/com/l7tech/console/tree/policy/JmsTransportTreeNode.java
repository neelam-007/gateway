/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;
import com.l7tech.policy.assertion.JmsTransportAssertion;

import javax.swing.*;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsTransportTreeNode extends LeafAssertionTreeNode {
    public JmsTransportTreeNode( Assertion assertion ) {
        super( assertion );
        this.assertion = (JmsTransportAssertion)assertion;
    }

    public String getName() {
        return "Message received using JMS";  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Action[] getActions() {
        return new Action[0];
    }

    public boolean canDelete() {
        return true;
    }

    protected String iconResource( boolean open ) {
        return "com/l7tech/console/resources/interface.gif";
    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }

    private JmsTransportAssertion assertion;

}
