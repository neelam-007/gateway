package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.alert.EmailAlertAssertion;

/**
 * The tree node in the assertion palette corresponding to the EmailAlertAssertion.
 */
public class EmailAlertAssertionPaletteNode extends AbstractLeafPaletteNode {
    public EmailAlertAssertionPaletteNode() {
        super("Send Email Message", "com/l7tech/console/resources/Edit16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new EmailAlertAssertion();
    }
}
