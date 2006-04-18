package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;

/**
 * Palette tool tree icon for the Hardcoded Response assertion.
 */
public class HardcodedResponsePaletteNode extends AbstractLeafPaletteNode {
    public HardcodedResponsePaletteNode() {
        super("Template Response", "com/l7tech/console/resources/MessageLength-16x16.gif");
    }

    public Assertion asAssertion() {
        return new HardcodedResponseAssertion();
    }
}
