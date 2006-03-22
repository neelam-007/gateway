package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;

/**
 * The tree node in the assertion palette corresponding to the EmailAlertAssertion.
 */
public class MockupPaletteNode extends AbstractLeafPaletteNode {
    public MockupPaletteNode(String name) {
        super(name, "com/l7tech/console/resources/AnalyzeGatewayLog16x16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return null;
    }
}
