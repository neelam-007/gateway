package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.Assertion;


/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 */
public class RequestWssSaml extends Assertion implements SecurityHeaderAddressable {
    private String[] subjectConfirmations = new String[]{};
    private boolean noSubjectConfirmation = false;
    private String nameQualifier = null;
    private String[] nameFormats = new String[]{};
    private String audienceRestriction;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireProofOfPosession = true;
    private boolean checkAssertionValidity = true;

    private SamlAuthenticationStatement authenticationStatement;
    private SamlAuthorizationStatement authorizationStatement;
    private SamlAttributeStatement attributeStatement;

    public RequestWssSaml() {
    }

    /**
     * Get the Subject confirmations specified in this assertion
     *
     * @return the array of subject confirmations specified
     * @see com.l7tech.common.security.saml.SamlConstants.CONFIRMATION_HOLDER_OF_KEY
     * @see com.l7tech.common.security.saml.SamlConstants.CONFIRMATION_SENDER_VOUCHES
     */
    public String[] getSubjectConfirmations() {
        return subjectConfirmations;
    }

    /**
     * Set the subject confirmations that are required
     *
     * @param subjectConfirmations
     */
    public void setSubjectConfirmations(String[] subjectConfirmations) {
        if (subjectConfirmations == null) {
            this.subjectConfirmations = new String[]{};
        } else {
            this.subjectConfirmations = subjectConfirmations;
        }
    }

    /**
     * @return the boolean flad indicating whether the assertion validity was requested
     */
    public boolean isCheckAssertionValidity() {
        return checkAssertionValidity;
    }

    /**
         * Set whther to check the assertion validity period.
         *
         * @param checkAssertionValidity true t ocheck assertion validity, false otherwise
         */
    public void setCheckAssertionValidity(boolean checkAssertionValidity) {
        this.checkAssertionValidity = checkAssertionValidity;
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

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public boolean isRequireProofOfPosession() {
        return requireProofOfPosession;
    }

    public void setRequireProofOfPosession(boolean requireProofOfPosession) {
        this.requireProofOfPosession = requireProofOfPosession;
    }

    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier(String nameQualifier) {
        this.nameQualifier = nameQualifier;
    }

    public String[] getNameFormats() {
        return nameFormats;
    }

    public void setNameFormats(String[] nameFormats) {
        if (nameFormats == null) {
            this.nameFormats = new String[]{};
        } else {
            this.nameFormats = nameFormats;
        }
    }

    public boolean isNoSubjectConfirmation() {
        return noSubjectConfirmation;
    }

    public void setNoSubjectConfirmation(boolean noSubjectConfirmation) {
        this.noSubjectConfirmation = noSubjectConfirmation;
    }

    /**
     * The SAML assertion is a credential source if the proof of posession has
     * been requested.
     *
     * @return true if credential source, false otherwise
     */
    public boolean isCredentialSource() {
        return isRequireProofOfPosession();
    }

    /**
     * @return the authentication statement constraints or <b>null</b> if no
     *          authentication statemement constraints have been sent
     */
    public SamlAuthenticationStatement getAuthenticationStatement() {
        return authenticationStatement;
    }

    /**
     * Set the authentication statement constraints
     * @param authenticationStatement the authentication statement constraints
     */
    public void setAuthenticationStatement(SamlAuthenticationStatement authenticationStatement) {
        this.authenticationStatement = authenticationStatement;
    }

    /**
     * @return the authorization statement constraints or <b>null</b> if no
     *          authorization statemement constraints have been sent
     */
    public SamlAuthorizationStatement getAuthorizationStatement() {
        return authorizationStatement;
    }

    /**
     * Set the authorization statement constraints
     * @param authorizationStatement the authorization statement constraints
     */
    public void setAuthorizationStatement(SamlAuthorizationStatement authorizationStatement) {
        this.authorizationStatement = authorizationStatement;
    }

    /**
     * @return the attribute statement constraints or <b>null</b> if no
     *          attribute statemement constraints have been sent
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
