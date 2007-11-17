package com.l7tech.console.tree.policy;

import com.l7tech.console.action.OperationPropertiesAction;
import com.l7tech.policy.assertion.Operation;

import javax.swing.*;

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

    public Action getPreferredAction() {
        return new OperationPropertiesAction(this);
    }
}
