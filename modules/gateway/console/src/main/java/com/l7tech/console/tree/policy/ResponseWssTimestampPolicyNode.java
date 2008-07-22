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
        String qualifier = "";
        if (((ResponseWssTimestamp)this.getUserObject()).isSignatureRequired()) {
            qualifier = "Signed ";
        }
        return "Add " + qualifier + "Timestamp to Response";
    }

    public Action getPreferredAction() {
        return new ResponseWssTimestampPropertiesAction(this);
    }

    public Action[] getActions() {
        Action[] superActions = super.getActions();
        Action[] newActions = new Action[superActions.length+1];
        newActions[0] = new EditXmlSecurityRecipientContextAction(this);
        System.arraycopy(superActions, 0, newActions, 1, superActions.length);
        return newActions;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }
}
