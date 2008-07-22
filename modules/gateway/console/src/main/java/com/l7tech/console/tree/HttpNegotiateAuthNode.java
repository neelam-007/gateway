package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;

/**
 * Palette node for intgrated windows authentication (Negotiate)
 *
 * @author $Author$
 * @version $Revision$
 */
public class HttpNegotiateAuthNode extends AbstractLeafPaletteNode {

    /**
     * construct the <CODE>HttpBasicAuthNode</CODE> instance.
     */
    public HttpNegotiateAuthNode() {
        super("Windows Integrated", "com/l7tech/console/resources/authentication.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new HttpNegotiate();
    }
}
