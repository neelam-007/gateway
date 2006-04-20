package com.l7tech.console.tree.policy;

import javax.swing.*;

import com.l7tech.console.action.RequestWssTimestampPropertiesAction;
import com.l7tech.console.action.WssTimestampPropertiesAction;
import com.l7tech.policy.assertion.xmlsec.WssTimestamp;

/**
 * Specifies the policy element that represents the soap response timestamp requirement.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WssTimestampPolicyNode extends LeafAssertionTreeNode {
    public WssTimestampPolicyNode(WssTimestamp assertion) {
        super(assertion);
    }

    public String getName() {
        return "Request and response timestamps";
    }

    public boolean canDelete() {
        return true;
    }

    public Action getPreferredAction() {
        return new WssTimestampPropertiesAction(this);
    }

    public Action[] getActions() {
        Action[] superActions = super.getActions();
        Action[] newActions = new Action[superActions.length+1];
        System.arraycopy(superActions, 0, newActions, 0, superActions.length);
        newActions[superActions.length] = new WssTimestampPropertiesAction(this);
        return newActions;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}
