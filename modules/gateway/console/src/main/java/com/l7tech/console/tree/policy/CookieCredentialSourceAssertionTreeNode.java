/*
 * Copyright (C) 2005-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.CookieCredentialSourceAssertionPropertiesAction;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;

import javax.swing.*;

/**
 * Policy tree node for cookie credential source.
 */
public class CookieCredentialSourceAssertionTreeNode extends LeafAssertionTreeNode {

    public CookieCredentialSourceAssertionTreeNode(CookieCredentialSourceAssertion assertion) {
        super(assertion);
    }

    public Action getPreferredAction() {
        return new CookieCredentialSourceAssertionPropertiesAction(this);
    }

    public CookieCredentialSourceAssertion getAssertion() {
        return (CookieCredentialSourceAssertion)asAssertion();
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require HTTP Cookie (name=" + getAssertion().getCookieName() + ")";
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
