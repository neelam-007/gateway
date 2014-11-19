package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;

public class HandleErrorsAssertionNode extends AbstractLeafPaletteNode {

    public HandleErrorsAssertionNode() {
        super(new HandleErrorsAssertion());
    }

    @Override
    public Assertion asAssertion() {
        return new HandleErrorsAssertion();
    }
}
