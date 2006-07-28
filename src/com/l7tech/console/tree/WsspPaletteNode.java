package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.WsspAssertion;

/**
 * Tree node in the assertion palette corresponding to the WSSP assertion type.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsspPaletteNode extends AbstractLeafPaletteNode {

    public WsspPaletteNode() {
        super("WS-Security Policy Compliance", "com/l7tech/console/resources/policy16.gif");
    }

    public Assertion asAssertion() {
        return new WsspAssertion();
    }
}
