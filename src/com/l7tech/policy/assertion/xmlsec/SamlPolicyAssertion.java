/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;

/**
 * @author alex
 */
public class SamlPolicyAssertion extends Assertion {
    private Integer version;
    private boolean noSubjectConfirmation = false;
    protected String nameQualifier = null;
    protected String audienceRestriction;
    protected SamlAuthenticationStatement authenticationStatement;
    protected SamlAuthorizationStatement authorizationStatement;
    protected SamlAttributeStatement attributeStatement;

    /**
     * Get the SAML version for this assertion
     *
     * <p>The value 0 means any version, null means unspecified (in which case 1 should
     * be used for backwards compatibility).</p>
     *
     * @return The saml version (0/1/2) or null.
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the SAML version for this assertion.
     *
     * @param version (may be null)
     */
    public void setVersion(Integer version) {
        this.version = version;
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
        this.nameQualifier = nameQualifier;
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
    public SamlAuthorizationStatement getAuthorizationStatement() {
        return authorizationStatement;
    }

    /**
         * Set the authorization statement constraints
         *
         * @param authorizationStatement the authorization statement constraints
         */
    public void setAuthorizationStatement(SamlAuthorizationStatement authorizationStatement) {
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
}
