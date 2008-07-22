package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the TrueAssertion node.
 */
public class TrueAssertionPaletteNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>TrueAssertionPaletteNode</CODE> instance.
     */
    public TrueAssertionPaletteNode() {
        super("Continue processing", "com/l7tech/console/resources/check16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new TrueAssertion();
    }
}
