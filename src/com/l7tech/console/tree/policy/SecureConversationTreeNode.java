package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.xmlsec.SecureConversation;

import javax.swing.*;

/**
 * This is the tree node corresponding to the SecureConversation assertion type.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationTreeNode extends LeafAssertionTreeNode {
    public SecureConversationTreeNode(SecureConversation assertion) {
        super(assertion);
    }

    public String getName() {
        return "WS Secure Conversation";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }

    public Action[] getActions() {
        return super.getActions();
    }

    /**
     *Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }
}
