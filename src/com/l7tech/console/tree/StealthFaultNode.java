/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.StealthFault;

import javax.swing.*;

/**
 * Palette node for the StealthFault assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class StealthFaultNode extends AbstractTreeNode {

    public StealthFaultNode() {
        super(null);
    }
    protected void loadChildren() {}

    public String getName() {
        return "Stealth Fault";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
    }

    public Action[] getActions() {
        return new Action[]{};
    }

    public Assertion asAssertion() {
        return new StealthFault();

    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }
}
