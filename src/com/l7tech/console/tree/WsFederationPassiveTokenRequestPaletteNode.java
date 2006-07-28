package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;


/**
 * The class represents the WS-Federation Passive Request node element in the
 * assertion palette.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenRequestPaletteNode extends AbstractLeafPaletteNode {

    /**
     *
     */
    public WsFederationPassiveTokenRequestPaletteNode() {
        super("WS-Federation Passive Credential", "com/l7tech/console/resources/xmlWithCert16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new WsFederationPassiveTokenRequest();
    }

}
