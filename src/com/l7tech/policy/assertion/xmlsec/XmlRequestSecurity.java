package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 2:08:45 PM
 * $Id$
 *
 * Enforces XML digital signature on the entire envelope of the response and maybe XML encryption on the body
 * element of the response.
 *
 * Whether XML encryption is used depends on the property encryption
 */
public class XmlRequestSecurity extends CredentialSourceAssertion implements XmlSecurityAssertion {
    public static final String SESSION_STATUS_HTTP_HEADER = "l7-session-status";
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
