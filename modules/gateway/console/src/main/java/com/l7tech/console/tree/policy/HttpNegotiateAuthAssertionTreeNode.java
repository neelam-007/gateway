/*
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.credential.http.HttpNegotiate;

/**
 * Tree node for Windows Integrated authentication (Negotiate)
 */
public class HttpNegotiateAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpNegotiateAuthAssertionTreeNode(HttpNegotiate assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require Windows Integrated Authentication Credentials";
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/authentication.gif";
    }
}
