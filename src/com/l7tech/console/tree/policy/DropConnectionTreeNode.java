/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Aug 15, 2005<br/>
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.DropConnection;

import javax.swing.*;

/**
 * Policy node for the DropConnection assertion.
 *
 * @author flascelles@layer7-tech.com
 */
public class DropConnectionTreeNode extends LeafAssertionTreeNode {
    public DropConnectionTreeNode(DropConnection assertion) {
        super(assertion);
    }

    public String getName() {
        return "Drop Connection";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        return super.getActions();
    }

}
