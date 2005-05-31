/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Apr 1, 2005<br/>
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.sla.ThroughputQuota;

import javax.swing.*;

/**
 * Tree node in the assertion palette corresponding to the ThroughputQuota assertion type.
 *
 * @author flascelles@layer7-tech.com
 */
public class ThroughputQuotaPaletteNode extends AbstractTreeNode {

    public ThroughputQuotaPaletteNode() {
        super(null);
    }

    protected void loadChildren() {}

    public String getName() {
        return "Throughput Quota";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/policy16.gif";
    }

    public Action[] getActions() {
        return new Action[]{};
    }

    public Assertion asAssertion() {
        return new ThroughputQuota();
    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }
}
