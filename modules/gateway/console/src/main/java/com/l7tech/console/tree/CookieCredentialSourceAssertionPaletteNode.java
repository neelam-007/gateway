/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;

/**
 * The class represents a node element in the TreeModel.
 * It represents the HTTP basic authentication.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class CookieCredentialSourceAssertionPaletteNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>HttpBasicAuthNode</CODE> instance.
     */
    public CookieCredentialSourceAssertionPaletteNode() {
        super("Require HTTP Cookie", "com/l7tech/console/resources/authentication.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new CookieCredentialSourceAssertion();
    }
}
