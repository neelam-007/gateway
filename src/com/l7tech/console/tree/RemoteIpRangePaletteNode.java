package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RemoteIpRange;

import javax.swing.*;

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
public class RemoteIpRangePaletteNode extends AbstractTreeNode {
    public RemoteIpRangePaletteNode() {
        super(null);
    }

    protected void loadChildren() {}

    public String getName() {
        return "IP Address Range";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/network.gif";
    }

    public Action[] getActions() {
        return new Action[]{};
    }

    public Assertion asAssertion() {
        return new RemoteIpRange();
    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }
}
