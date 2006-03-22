package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AuditDetailAssertion;

/**
 * The tree node in the assertion palette corresponding to the AuditAssertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 19, 2006<br/>
 */
public class AuditDetailAssertionPaletteNode extends AbstractLeafPaletteNode {
    public AuditDetailAssertionPaletteNode() {
        super("Audit Detail Assertion", "com/l7tech/console/resources/Edit16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new AuditDetailAssertion();
    }
}
