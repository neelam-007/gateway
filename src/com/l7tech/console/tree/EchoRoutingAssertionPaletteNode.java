/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.EchoRoutingAssertion;

/**
 * The tree node in the assertion palette corresponding to the EchoRoutingAssertion Assertion.
 */
public class EchoRoutingAssertionPaletteNode extends AbstractLeafPaletteNode {
    public EchoRoutingAssertionPaletteNode() {
        super("Echo Routing Assertion", "com/l7tech/console/resources/Edit16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new EchoRoutingAssertion();
    }
}
