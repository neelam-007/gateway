package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * The <code>SamlStatementAssertion</code> assertion describes
 * the common SAML constraints shared by Authentication Statement constraints.
 */
abstract class SamlStatementAssertion extends CredentialSourceAssertion {
    private String[] subjectConfirmations = new String[] {};
    private String targetRestriction;
    private String audienceRestriction;

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
        }
        this.subjectConfirmations = subjectConfirmations;
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
}
