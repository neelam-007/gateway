package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;

import javax.swing.*;

/**
 * This is the tree node corresponding to the RequestWssX509Cert assertion type.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class RequestWssReplayProtectionTreeNode extends LeafAssertionTreeNode {
    public RequestWssReplayProtectionTreeNode(RequestWssReplayProtection assertion) {
        super(assertion);
    }

    public String getName() {
        return "WSS Replay Protection";
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
