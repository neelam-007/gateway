package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

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
public class SecureConversationPaletteNode extends AbstractLeafPaletteNode {
    public SecureConversationPaletteNode() {
        super("Secure Conversation", "com/l7tech/console/resources/network.gif");
    }

    public Assertion asAssertion() {
        return new SecureConversation();
    }

}
