package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.transport.PreemptiveCompression;

/**
 * The palette node for this assertion
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 3, 2008<br/>
 */
public class PreemptiveCompressionPaletteNode extends AbstractLeafPaletteNode {
    public PreemptiveCompressionPaletteNode() {
        super("Preemptive Compression", "com/l7tech/console/resources/authentication.gif");
    }

    public Assertion asAssertion() {
        return new PreemptiveCompression();
    }
}
