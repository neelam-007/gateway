/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;

import javax.swing.*;

/**
 * @author alex
 * @version $Revision$
 */
public class JmsTransportPaletteNode extends AbstractTreeNode {
    public JmsTransportPaletteNode() {
        super(null);
    }

    protected void loadChildren() { }

    public String getName() {
        return "Message received via JMS";
    }

    protected String iconResource( boolean open ) {
        return "com/l7tech/console/resources/interface.gif";
    }

    public Action[] getActions() {
        return new Action[]{};
    }

    public Assertion asAssertion() {
        return new RemoteIpRange();
    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }
}
