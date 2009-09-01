package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the SSL transport node.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SslTransportNode extends AbstractLeafPaletteNode {
    private boolean requireClientAuthentication;

    /**
     * construct the <CODE>SslTransportNode</CODE> instance indicating
     * whether the client Certificate Auth4ntication is required or not.
     *
     * @param requireClientAuthentication true if the client authentication is required,
     *                                    false otherwise
     */
    public SslTransportNode(boolean requireClientAuthentication){
        super("Set SSL or TLS Transport", "com/l7tech/console/resources/ssl.gif");
        this.requireClientAuthentication = requireClientAuthentication;
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new SslAssertion(requireClientAuthentication);

    }
}