package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;

/**
 * This assertion requires the client to establish a secure conversation prior
 * to consuming the service and use this secure conversation context to secure
 * requests for the consumption of the service.
 * <p/>
 * It can be used in conjonction with RequestWss* and ResponseWss* assertions
 * if the the administrator wishes to specify which elements are signed and
 * or encrypted.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 4, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversation extends Assertion {
    /**
     *Secure Conversation is always credential source
     *
     * @return always true
     */
    public boolean isCredentialSource() {
        return true;
    }
}
