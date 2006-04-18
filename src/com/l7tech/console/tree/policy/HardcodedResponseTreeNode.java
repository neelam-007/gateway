package com.l7tech.console.tree.policy;

import com.l7tech.console.action.HardcodedResponseDialogAction;
import com.l7tech.policy.assertion.HardcodedResponseAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Policy tree node for Hardcoded Response assertion.
 */
public class HardcodedResponseTreeNode extends LeafAssertionTreeNode{
    /**
     * Instantiate the new <code>LeafAssertionTreeNode</code>
     * with the given assertion.
     *
     * @param assertion the assertion
     */
    public HardcodedResponseTreeNode(HardcodedResponseAssertion assertion) {
        super(assertion);
    }

    public String getName() {
        return "Template Response";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/MessageLength-16x16.gif";
    }

    public Action getPreferredAction() {
        return new HardcodedResponseDialogAction(this);
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[list.size()]);
    }
}
