/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpTransportAssertion;

import javax.swing.*;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpTransportTreeNode extends LeafAssertionTreeNode {
    public HttpTransportTreeNode( Assertion assertion ) {
        super( assertion );
        this.assertion = (HttpTransportAssertion)assertion;
    }

    public String getName() {
        return "Message received using HTTP(S)";
    }

    public Action[] getActions() {
        return new Action[0];
    }

    public boolean canDelete() {
        return true;
    }

    protected String iconResource( boolean open ) {
        return "com/l7tech/console/resources/server16.gif";
    }

    private HttpTransportAssertion assertion;
}
