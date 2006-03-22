/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.BridgeRoutingAssertion;

/**
 * Represents a palette tree node for a {@link com.l7tech.policy.assertion.BridgeRoutingAssertion}.
 */
public class BridgeRoutingNode extends AbstractLeafPaletteNode {
    public BridgeRoutingNode() {
        super("SecureSpan Bridge Routing", "com/l7tech/console/resources/server16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new BridgeRoutingAssertion();
    }
}
