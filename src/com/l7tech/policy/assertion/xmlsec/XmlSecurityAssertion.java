package com.l7tech.policy.assertion.xmlsec;

/**
 * The <code>XmlSecurityAssertion</code> that sets the encryption toggle.
 * <p>
 * For example for the xml digital signature the xml encryption may be set.
 * In the case of <code>SamlAssertion</code>
 */
public interface XmlSecurityAssertion {
    /**
     * This property describes whether or not the encryption is set on the assertion.
     * The exact meaning of the encryption depends of the implementing class.
     *
     * In the body should be encrypted as opposed to only signed
     *
     * @return whether the encryption is used or not
     */
    boolean isEncryption();

    /**
     * Enable or disable the encryption on the assertion. The exact meaning of the
     * encryption flag depends on the implementing class.
     *
     * @param b toggle the encryption on the assertion
     */
    void setEncryption(boolean b);
}
