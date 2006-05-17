package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Operation;
import com.l7tech.console.action.OperationPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Policy tree node for the Operation assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 17, 2006<br/>
 *
 * @see com.l7tech.policy.assertion.Operation
 */
public class OperationTreeNode extends LeafAssertionTreeNode {
    private final Operation assertion;
    public OperationTreeNode(Operation assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    public String getName() {
        String tmp = assertion.getOperationName();
        if (tmp == null) tmp = "undefined";
        return "WSDL Operation \'" + tmp + "\'";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/Information16.gif";
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(getPreferredAction());
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[])list.toArray(new Action[]{});
    }

    public Action getPreferredAction() {
        return new OperationPropertiesAction(this);
    }
}
