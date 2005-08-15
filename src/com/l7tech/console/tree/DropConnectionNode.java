/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.DropConnection;

import javax.swing.*;

/**
 * Palette node for the DropConnection assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class DropConnectionNode extends AbstractTreeNode {

    public DropConnectionNode() {
        super(null);
    }
    protected void loadChildren() {}

    public String getName() {
        return "Drop Connection";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
    }

    public Action[] getActions() {
        return new Action[]{};
    }

    public Assertion asAssertion() {
        return new DropConnection();

    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }
}
