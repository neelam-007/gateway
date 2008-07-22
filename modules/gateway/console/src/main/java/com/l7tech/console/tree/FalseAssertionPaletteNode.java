/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FalseAssertion;

/**
 * The class represents a node element in the TreeModel.
 * It represents the FalseAssertion node.
 */
public class FalseAssertionPaletteNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>FalseAssertionPaletteNode</CODE> instance.
     */
    public FalseAssertionPaletteNode() {
        super("Stop processing", "com/l7tech/console/resources/Stop16.gif");
    }


    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new FalseAssertion();
    }
}
