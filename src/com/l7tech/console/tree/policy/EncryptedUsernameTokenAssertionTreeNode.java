package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class EncryptedUsernameTokenAssertionTreeNode is a tree node that correspinds
 * to the <code>EncryptedUsernameTokenAssertion</code>.
 */
public class EncryptedUsernameTokenAssertionTreeNode extends LeafAssertionTreeNode {
    public EncryptedUsernameTokenAssertionTreeNode(EncryptedUsernameTokenAssertion assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        if (!assertion.getRecipientContext().localRecipient()) {
            return "Require Encrypted UsernameToken Authentication [\'" +
                   assertion.getRecipientContext().getActor() + "\' actor]";
        } else {
            return "Require Encrypted UsernameToken Authentication";
        }
    }

    public Action[] getActions() {
        java.util.List list = new ArrayList();
        list.add(new EditXmlSecurityRecipientContextAction(this));
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[]{});
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/authentication.gif";
    }

    private EncryptedUsernameTokenAssertion assertion;
}