package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.alert.SnmpTrapAssertion;

/**
 * The tree node in the assertion palette corresponding to the SnmpTrapAssertion.
 */
public class SnmpTrapAssertionPaletteNode extends AbstractLeafPaletteNode {
    public SnmpTrapAssertionPaletteNode() {
        super("Send SNMP Trap", "com/l7tech/console/resources/Edit16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new SnmpTrapAssertion();
    }
}
