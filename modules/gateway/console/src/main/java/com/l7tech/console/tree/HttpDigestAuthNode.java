package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.HttpDigest;


/**
 * The class represents a node element in the TreeModel.
 * It represents the HTTP digest authentication.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpDigestAuthNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>HttpDigestAuthNode</CODE> instance.
     */
    public HttpDigestAuthNode() {
        super("Require HTTP Digest Credentials", "com/l7tech/console/resources/authentication.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new HttpDigest();
    }
}