package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

/**
 * Node for the SecureConversation assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationNode extends AbstractLeafPaletteNode {
    public SecureConversationNode() {
        super("WS Secure Conversation", "com/l7tech/console/resources/xmlencryption.gif");
    }

    public Assertion asAssertion() {
        return new SecureConversation();
    }
}