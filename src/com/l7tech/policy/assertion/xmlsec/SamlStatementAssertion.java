package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * The <code>SamlStatementAssertion</code> assertion describes
 * the common SAML constraints shared by Authentication Statement constraints.
 */
abstract class SamlStatementAssertion extends CredentialSourceAssertion implements SecurityHeaderAddressable {
    private String[] subjectConfirmations = new String[] {};
    private String nameQualifier  = null;
    private String[] nameFormats  = new String[] {};
    private String targetRestriction;
    private String audienceRestriction;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireProofOfPosession = true;
    private boolean checkAssertionValidity  = true;

    SamlStatementAssertion() {
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
            this.subjectConfirmations = new String[] {};
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
     *  Set whther to check the assertion validity period.
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
     * @param audienceRestriction
     */
    public void setAudienceRestriction(String audienceRestriction) {
        this.audienceRestriction = audienceRestriction;
    }

    /**
     * Get the target restriction
     * @return the target restriction if set or <b>null</b> if not set
     */
    public String getTargetRestriction() {
        return targetRestriction;
    }

    public void setTargetRestriction(String targetRestriction) {
        this.targetRestriction = targetRestriction;
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
            this.nameFormats = new String[] {};
        } else {
            this.nameFormats = nameFormats;
        }
    }
}
