package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;


/**
 * The class represents a node element in the TreeModel.
 * It represents the HTTP basic authentication.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class WsTokenBasicAuthNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>HttpBasicAuthNode</CODE> instance.
     */
    public WsTokenBasicAuthNode() {
        super("WSS UsernameToken Basic", "com/l7tech/console/resources/authentication.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new WssBasic();
    }
}