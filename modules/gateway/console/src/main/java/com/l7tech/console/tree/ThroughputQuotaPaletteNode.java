/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;

/**
 * Tree node in the assertion palette corresponding to the ThroughputQuota assertion type.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaPaletteNode extends AbstractLeafPaletteNode {

    public ThroughputQuotaPaletteNode() {
        super("Throughput Quota", "com/l7tech/console/resources/policy16.gif");
    }

    public Assertion asAssertion() {
        return new ThroughputQuota();
    }
}
