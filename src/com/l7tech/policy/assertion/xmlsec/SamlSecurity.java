package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;

/**
 * The <code>SamlSecurity</code> class specifies the SAML constraints.
 */
public class SamlSecurity extends Assertion /*implements XmlSecurityAssertion*/ {

    /**
     * Tests whether or not the saml validity period is tested.
     * 
     * @return the validity period toggle value
     */
    public boolean isValidateValidityPeriod() {
        return validateValidityPeriod;
    }

    /**
     * Sets the saml validity period flag.
     * 
     * @param b the new validity period toggle value
     */
    public void setValidateValidityPeriod(boolean b) {
        this.validateValidityPeriod = b;
    }

    /**
     * Tests whether or not the saml signature will be validated.
     * 
     * @return the validity period toggle value
     */
    public boolean isValidateSignature() {
        return validateSignature;
    }

    /**
     * Sets the saml assertion signature validition toggle.
     * 
     * @param b the signature toggle value
     */
    public void setValidateSignature(boolean b) {
        this.validateSignature = b;
    }

    private boolean validateValidityPeriod = false;
    private boolean validateSignature = false;
}
