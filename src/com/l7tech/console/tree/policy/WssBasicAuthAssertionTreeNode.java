package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class HttpBasicAuthAssertionTreeNode is a tree node that correspinds
 * to the <code>HttpBasic</code> asseriton.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class WssBasicAuthAssertionTreeNode extends LeafAssertionTreeNode {
    public WssBasicAuthAssertionTreeNode(WssBasic assertion) {
        super(assertion);
        this.assertion = assertion;
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        if (!assertion.getRecipientContext().localRecipient()) {
            return "Require WSS UsernameToken Basic Authentication [\'" +
                   assertion.getRecipientContext().getActor() + "\' actor]";
        } else {
            return "Require WSS UsernameToken Basic Authentication";
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

    private WssBasic assertion;
}