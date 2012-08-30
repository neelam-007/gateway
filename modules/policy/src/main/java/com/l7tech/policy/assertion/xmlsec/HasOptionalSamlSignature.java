package com.l7tech.policy.assertion.xmlsec;

/**
 * Implemented if a SAML token allows the requirement for an embedded signature covering the Assertion to be optional.
 */
public interface HasOptionalSamlSignature {

    public boolean isRequireDigitalSignature();

    public void setRequireDigitalSignature(boolean requireDigitalSignature);
}
