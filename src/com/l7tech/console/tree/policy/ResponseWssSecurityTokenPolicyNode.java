package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.ResponseWssSecurityTokenPropertiesAction;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;

import javax.swing.*;

/**
 * Specifies the policy element that represents the soap response security token requirement.
 */
public class ResponseWssSecurityTokenPolicyNode extends LeafAssertionTreeNode {
    private final ResponseWssSecurityToken assertion;

    public ResponseWssSecurityTokenPolicyNode(ResponseWssSecurityToken assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    public String getName() {
        return "Add Signed " + assertion.getTokenType().getName() + " to Response";
    }

    public Action getPreferredAction() {
        return new ResponseWssSecurityTokenPropertiesAction(this);
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
