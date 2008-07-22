package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Operation;

/**
 * Palette node for the Operation Assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 * @see com.l7tech.policy.assertion.Operation
 */
public class OperationPaletteNode extends AbstractLeafPaletteNode {
    public OperationPaletteNode() {
        super("WSDL Operation", "com/l7tech/console/resources/Information16.gif");
    }

    public Assertion asAssertion() {
        return new Operation();
    }
}
