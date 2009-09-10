/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.HttpTransportAssertion;

public class HttpTransportTreeNode extends LeafAssertionTreeNode<HttpTransportAssertion> {
    public HttpTransportTreeNode(HttpTransportAssertion assertion) {
        super(assertion);
    }

    public String getName(final boolean decorate) {
        return "Message received using HTTP(S)";
    }

    protected String iconResource( boolean open ) {
        return "com/l7tech/console/resources/server16.gif";
    }
}
