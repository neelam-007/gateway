/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.credential.http.HttpClientCert;

/**
 * Tree node corresponding to HttpClientCert policy assertion.
 * User: mike
 * Date: Aug 20, 2003
 * Time: 9:26:20 AM
 */
public class HttpClientCertAssertionTreeNode extends LeafAssertionTreeNode {
    public HttpClientCertAssertionTreeNode(HttpClientCert assertion) {
        super(assertion);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/authentication.gif";
    }

    public boolean canDelete() {
        return true;
    }

    public String getName() {
        return "Require HTTP Client Certificate";
    }
}
