package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SqlAttackDialogAction;
import com.l7tech.policy.assertion.SqlAttackAssertion;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 28, 2005
 * Time: 4:08:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class SqlAttackAssertionTreeNode extends LeafAssertionTreeNode{
    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public SqlAttackAssertionTreeNode(SqlAttackAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "SQL Attack Protection";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/SQLProtection16x16.gif";
    }

    public Action getPreferredAction() {
        return new SqlAttackDialogAction(this);
    }
}
