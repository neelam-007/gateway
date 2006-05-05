package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.FaultLevel;
import com.l7tech.console.action.FaultLevelPropertiesAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Node in the policy tree that represents the FaultLevel assertion
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 4, 2006<br/>
 *
 * @see FaultLevel
 */
public class FaultLevelTreeNode extends LeafAssertionTreeNode {
    public FaultLevelTreeNode(FaultLevel assertion) {
        super(assertion);
    }

    public String getName() {
        return "Override SOAP Fault";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/disconnect.gif";
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
        return new FaultLevelPropertiesAction(this);
    }
}
