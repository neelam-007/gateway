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
     * Arbitrarily chose a subclass of WsFederationPassiveTokenAssertion to create with this palette node. The actual
     * assertion class may change depending on how the user interacts with the assertions property dialog.
     * See comment in AccessControlFolderNode.
     */
    public WsFederationPassiveTokenRequestPaletteNode() {
        super(new WsFederationPassiveTokenRequest());
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the assertion this node represents
     */
    public Assertion asAssertion() {
        return new WsFederationPassiveTokenRequest();
    }

}
