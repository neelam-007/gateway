package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;


/**
 * The class represents the WS-Federation Passive Exchange node element in the
 * assertion palette.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsFederationPassiveTokenExchangePaletteNode extends AbstractLeafTreeNode {

    /**
     *
     */
    public WsFederationPassiveTokenExchangePaletteNode() {
        super("WS-Federation Passive Credential Exchange", "com/l7tech/console/resources/xmlWithCert16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represnts
     */
    public Assertion asAssertion() {
        return new WsFederationPassiveTokenExchange();
    }

}
