/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.credential.http.HttpBasic;

/**
 * Class HttpBasicAuthAssertionTreeNode is a tree node that correspinds
 * to the <code>HttpBasic</code> asseriton.
 */
public class HttpBasicAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpBasicAuthAssertionTreeNode(HttpBasic assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require HTTP Basic Credentials";
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