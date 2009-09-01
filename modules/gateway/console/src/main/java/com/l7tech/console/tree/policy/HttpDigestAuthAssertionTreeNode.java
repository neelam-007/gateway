/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.credential.http.HttpDigest;

/**
 * Class HttpDigestAuthAssertionTreeNode is a tree node that corresponds
 * to the <code>HttpDigest</code> asseriton.
 */
public class HttpDigestAuthAssertionTreeNode extends LeafAssertionTreeNode {

    public HttpDigestAuthAssertionTreeNode(HttpDigest assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require HTTP Digest Credentials";
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