package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.FaultLevel;

/**
 * Palette node representing the Fault Level assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 * 
 * @see FaultLevel
 */
public class FaultLevelPaletteNode extends AbstractLeafPaletteNode {
    public FaultLevelPaletteNode() {
        super("Fault Level", "com/l7tech/console/resources/disconnect.gif");
    }

    public Assertion asAssertion() {
        return new FaultLevel();
    }
}
