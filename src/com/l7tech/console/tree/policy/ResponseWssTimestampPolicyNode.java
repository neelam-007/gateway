/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;
import com.l7tech.console.action.ResponseWssTimestampPropertiesAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;

import javax.swing.*;

/**
 * Specifies the policy element that represents the soap response timestamp requirement.
 */
public class ResponseWssTimestampPolicyNode extends LeafAssertionTreeNode {
    public ResponseWssTimestampPolicyNode(ResponseWssTimestamp assertion) {
        super(assertion);
    }

    public String getName() {
        return "Add Signed Timestamp to Response";
    }

    public boolean canDelete() {
        return true;
    }

    public Action getPreferredAction() {
        return new ResponseWssTimestampPropertiesAction(this);
    }

    public Action[] getActions() {
        Action[] superActions = super.getActions();
        Action[] newActions = new Action[superActions.length+2];
        newActions[0] = new ResponseWssTimestampPropertiesAction(this);
        newActions[1] = new EditXmlSecurityRecipientContextAction(this);
        System.arraycopy(superActions, 0, newActions, 2, superActions.length);
        return newActions;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}
