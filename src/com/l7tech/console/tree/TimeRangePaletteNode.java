package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TimeRange;

/**
 * The tree node in the assertion palette corresponding to the TimeRange Assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 *
 */
public class TimeRangePaletteNode extends AbstractLeafPaletteNode {
    public TimeRangePaletteNode() {
        super("Time/Day Availability", "com/l7tech/console/resources/time.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new TimeRange();
    }
}
