package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

import javax.swing.*;

/**
 * Tree node in the assertion palette corresponding to the SecureConversation assertion type.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 *
 */
public class SecureConversationPaletteNode extends AbstractTreeNode {
    public SecureConversationPaletteNode() {
        super(null);
    }

    protected void loadChildren() {}

    public String getName() {
        return "Secure Conversation";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/network.gif";
    }

    public Action[] getActions() {
        return new Action[]{};
    }

    public Assertion asAssertion() {
        return new SecureConversation();
    }

    public boolean isLeaf() {
        return true;
    }

    public boolean getAllowsChildren() {
        return false;
    }
}
