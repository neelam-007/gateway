package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssKerberos;

/**
 * @author $Author$
 * @version $Revision$
 */
public class KerberosPaletteNode  extends AbstractLeafPaletteNode {

    /**
     * construct the <CODE>HttpDigestAuthNode</CODE> instance.
     */
    public KerberosPaletteNode() {
        super("WSS Kerberos", "com/l7tech/console/resources/authentication.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new RequestWssKerberos();
    }
}
