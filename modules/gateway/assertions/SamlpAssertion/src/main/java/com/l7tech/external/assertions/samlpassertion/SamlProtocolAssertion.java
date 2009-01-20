/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;


/**
 * @author megery - migrated from the 4.x branches where this exists in the main tree.
 */
public class SamlProtocolAssertion extends MessageTargetableAssertion {
    private Integer samlVersion;
    private Integer soapVersion;
    private boolean noSubjectConfirmation = false;
    protected String nameQualifier = null;
    protected String audienceRestriction;
    protected SamlAuthenticationStatement authenticationStatement;
    protected SamlpAuthorizationStatement authorizationStatement;
    protected SamlAttributeStatement attributeStatement;

    /**
     * Get the SAML version for this assertion
     *
     * <p>The value 0 means any version, null means unspecified (in which case 1 should
     * be used for backwards compatibility).</p>
     *
     * @return The saml version (0/1/2) or null.
     */
    public Integer getSamlVersion() {
        return samlVersion;
    }

    /**
     * Set the SAML version for this assertion.
     *
     * @param version (may be null)
     */
    public void setSamlVersion(Integer version) {
        this.samlVersion = version;
    }

    /**
     * Gets the SOAP version for this assertion.  Expected values:
     * <ul>
     * <li>0 = Default</li>
     * <li>1 = SOAP 1.1</li>
     * <li>2 = SOAP 1.2</li>
     * </ul>
     *
     * @return SOAP Version code
     */
    public Integer getSoapVersion() {
        return soapVersion;
    }

    /**
     * Sets the SOAP version for this assertion.  Expected values:
     * <ul>
     * <li>0 = Default</li>
     * <li>1 = SOAP 1.1</li>
     * <li>2 = SOAP 1.2</li>
     * </ul>
     *
     * @param soapVersion the soap version code to set
     */
    public void setSoapVersion(Integer soapVersion) {
        this.soapVersion = soapVersion;
    }

    /**
     * @return the audience restriction string or <b>null</b> if not set
     */
    public String getAudienceRestriction() {
        return audienceRestriction;
    }

    /**
     * Set the audience restrictions SAML property
     *
     * @param audienceRestriction
     */
    public void setAudienceRestriction(String audienceRestriction) {
        this.audienceRestriction = audienceRestriction;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = checkEmpty(nameQualifier);
    }

    /**
     * This flag requires the lack of a subject confirmation.  A request that has a subject confirmation will
     * not be accepted if this flag is set.
     *
     * @return If this is set, the request will be REQUIRED to have no subject confirmation.
     */
    public boolean isNoSubjectConfirmation() {
        return noSubjectConfirmation;
    }

    /**
     * This flag requires the lack of a subject confirmation.  A request that has a subject confirmation will
     * not be accepted if this flag is set.
     *
     * @param noSubjectConfirmation If this is set, the request will be REQUIRED to have no subject confirmation.
     */
    public void setNoSubjectConfirmation(boolean noSubjectConfirmation) {
        this.noSubjectConfirmation = noSubjectConfirmation;
    }

    /**
         * @return the authentication statement constraints or <b>null</b> if no
         *         authentication statemement constraints have been sent
         */
    public SamlAuthenticationStatement getAuthenticationStatement() {
        return authenticationStatement;
    }

    /**
         * Set the authentication statement constraints
         *
         * @param authenticationStatement the authentication statement constraints
         */
    public void setAuthenticationStatement(SamlAuthenticationStatement authenticationStatement) {
        this.authenticationStatement = authenticationStatement;
    }

    /**
         * @return the authorization statement constraints or <b>null</b> if no
         *         authorization statemement constraints have been sent
         */
    public SamlpAuthorizationStatement getAuthorizationStatement() {
        return authorizationStatement;
    }

    /**
         * Set the authorization statement constraints
         *
         * @param authorizationStatement the authorization statement constraints
         */
    public void setAuthorizationStatement(SamlpAuthorizationStatement authorizationStatement) {
        this.authorizationStatement = authorizationStatement;
    }

    /**
         * @return the attribute statement constraints or <b>null</b> if no
         *         attribute statemement constraints have been sent
         */
    public SamlAttributeStatement getAttributeStatement() {
        return attributeStatement;
    }

    /**
     * Set the attribute statement constraints
     * @param attributeStatement the atribute statement constraints
     */
    public void setAttributeStatement(SamlAttributeStatement attributeStatement) {
        this.attributeStatement = attributeStatement;
    }


    protected String checkEmpty(String value) {
        if (value != null && value.trim().length() == 0)
            return null;
        return value;
    }
}