package com.l7tech.policy.assertion.xmlsec;



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
public interface XmlSecurityAssertion {
    /**
     * This property describes whether or not the body should be encrypted as opposed to only signed
     */
    abstract boolean isEncryption();

    /**
     * This property describes whether or not the body should be encrypted as opposed to only signed
     */
    abstract void setEncryption(boolean encryption);
}
