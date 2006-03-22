package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the HTTP routing.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpRoutingNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>HttpRoutingNode</CODE> instance.
     */
    public HttpRoutingNode() {
        super("HTTP(S) Routing", "com/l7tech/console/resources/server16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new HttpRoutingAssertion();
    }
}
