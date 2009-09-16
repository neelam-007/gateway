package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

/**
 * The class represents a node element in the TreeModel.
 * It represents the OneOrMoreAssertion node.
 */
public class OneOrMoreNode extends AbstractLeafPaletteNode {

    public OneOrMoreNode() {
        super(new OneOrMoreAssertion());
    }

    @Override
    public Assertion asAssertion() {
        return new OneOrMoreAssertion();
    }

}
