/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SetVariableAssertion;

/**
 * The class represents a node element in the TreeModel.
 * It represents the {@link com.l7tech.policy.assertion.SetVariableAssertion}.
 */
public class SetVariableAssertionPaletteNode extends AbstractLeafPaletteNode {
    public SetVariableAssertionPaletteNode() {
        super("Set Variable", "com/l7tech/console/resources/check16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new SetVariableAssertion();
    }
}
