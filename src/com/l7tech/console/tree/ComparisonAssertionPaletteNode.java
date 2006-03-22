package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ComparisonAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the {@link com.l7tech.policy.assertion.ComparisonAssertion}.
 */
public class ComparisonAssertionPaletteNode extends AbstractLeafPaletteNode {
    public ComparisonAssertionPaletteNode() {
        super("Comparison", "com/l7tech/console/resources/check16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new ComparisonAssertion();
    }
}
