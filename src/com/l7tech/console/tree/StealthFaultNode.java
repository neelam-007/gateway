/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.StealthFault;

/**
 * Palette node for the StealthFault assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class StealthFaultNode extends AbstractLeafPaletteNode {

    public StealthFaultNode() {
        super("Stealth Fault", "com/l7tech/console/resources/disconnect.gif");
    }

    public Assertion asAssertion() {
        return new StealthFault();

    }
}
