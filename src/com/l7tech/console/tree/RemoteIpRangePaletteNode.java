package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;

/**
 * Tree node in the assertion palette corresponding to the ReoteIpRange assertion type.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 23, 2004<br/>
 * $Id$<br/>
 *
 */
public class RemoteIpRangePaletteNode extends AbstractLeafPaletteNode {
    public RemoteIpRangePaletteNode() {
        super("IP Address Range", "com/l7tech/console/resources/network.gif");
    }

    public Assertion asAssertion() {
        return new RemoteIpRange();
    }
}
