package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.ConfidentialityAssertion;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 2:10:35 PM
 * $Id$
 *
 * Assertion that enforces xml digital signature on the soap envelope and potentially xml encryption on the soap body
 */
public abstract class XmlSecurityAssertion extends ConfidentialityAssertion {

    /**
     * This property describes whether or not the body should be encrypted as opposed to only signed
     */
    public boolean isEncryption() {
        return encryption;
    }

    /**
     * This property describes whether or not the body should be encrypted as opposed to only signed
     */
    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    private boolean encryption = false;
}
