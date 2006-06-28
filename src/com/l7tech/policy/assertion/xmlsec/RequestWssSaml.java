package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.policy.assertion.Assertion;

/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 */
public class RequestWssSaml extends Assertion implements SecurityHeaderAddressable {
    private Integer version;
    private String[] subjectConfirmations = new String[]{};
    private boolean noSubjectConfirmation = false;
    private String nameQualifier = null;
    private String[] nameFormats = new String[]{};
    private String audienceRestriction;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireSenderVouchesWithMessageSignature = false;
    private boolean requireHolderOfKeyWithMessageSignature = false;

    private boolean checkAssertionValidity = true;

    private SamlAuthenticationStatement authenticationStatement;
    private SamlAuthorizationStatement authorizationStatement;
    private SamlAttributeStatement attributeStatement;

    public RequestWssSaml() {
    }

    public RequestWssSaml(RequestWssSaml requestWssSaml) {
        copyFrom(requestWssSaml);
    }

    /**
     * Factory method that creates the Holder-Of-Key assertion
     *
     * @return the RequestWssSaml with Holder-Of-Key subject confirmation
     */
    public static RequestWssSaml newHolderOfKey() {
        RequestWssSaml ass = new RequestWssSaml();
        ass.setVersion(Integer.valueOf(0));
        ass.setRequireHolderOfKeyWithMessageSignature(true);
        ass.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_HOLDER_OF_KEY});
        return ass;
    }

    /**
         * Factory method that creates the Sender-Vouches assertion
         *
         * @return the RequestWssSaml with Sender-Vouches subject confirmation
         */
    public static RequestWssSaml newSenderVouches() {
        RequestWssSaml ass = new RequestWssSaml();
        ass.setVersion(Integer.valueOf(0));
        ass.setRequireSenderVouchesWithMessageSignature(true);
        ass.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SENDER_VOUCHES});
        return ass;
    }

    public void copyFrom(RequestWssSaml requestWssSaml) {
        this.setAttributeStatement(requestWssSaml.getAttributeStatement());
        this.setAudienceRestriction(requestWssSaml.getAudienceRestriction());
        this.setAuthenticationStatement(requestWssSaml.getAuthenticationStatement());
        this.setAuthorizationStatement(requestWssSaml.getAuthorizationStatement());
        this.setCheckAssertionValidity(requestWssSaml.isCheckAssertionValidity());
        this.setNameFormats(requestWssSaml.getNameFormats());
        this.setNameQualifier(requestWssSaml.getNameQualifier());
        this.setNoSubjectConfirmation(requestWssSaml.isNoSubjectConfirmation());
        this.setParent(requestWssSaml.getParent());
        this.setRecipientContext(requestWssSaml.getRecipientContext());
        this.setRequireHolderOfKeyWithMessageSignature(requestWssSaml.isRequireHolderOfKeyWithMessageSignature());
        this.setRequireSenderVouchesWithMessageSignature(requestWssSaml.isRequireSenderVouchesWithMessageSignature());
        this.setSubjectConfirmations(requestWssSaml.getSubjectConfirmations());
        this.setVersion(requestWssSaml.getVersion());
    }

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

    /**
     * This is not really useful since possession may be proven by something external to this
     * assertion (e.g. SSL client certificate).
     *
     * Also having no proof of possession does not mean this is not a credential source, it just
     * means it is a less reliable source.
     *
     * NOTE: if this is re-enabled you must also update WspConstants#ignoreAssertionProperties
     * /
    private boolean isRequireProofOfPosession() {
        final boolean hasHolderOfKey = hasHolderOfKey();
        final boolean hasSenderVouches = hasSenderVouches();
        if (!(hasHolderOfKey || hasSenderVouches)) return false;

        // proof of possession is not a requirement if other confirmation methods are allowed
        if(subjectConfirmations.length > 2 ||
            (subjectConfirmations.length > 1 && (!(hasHolderOfKey && hasSenderVouches)))) {
            return false;
        }
        
        boolean result = true;
        if (hasHolderOfKey) result = result && requireHolderOfKeyWithMessageSignature;
        if (hasSenderVouches) result = result && requireSenderVouchesWithMessageSignature;

        return result;
    }

    /**
     *
     */
    private boolean hasHolderOfKey() {
        for (int i = subjectConfirmations.length - 1; i >= 0; i--) {
            String subjectConfirmation = subjectConfirmations[i];
            if (SamlConstants.CONFIRMATION_HOLDER_OF_KEY.equals(subjectConfirmation)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSenderVouches() {
        for (int i = subjectConfirmations.length - 1; i >= 0; i--) {
            String subjectConfirmation = subjectConfirmations[i];
            if (SamlConstants.CONFIRMATION_SENDER_VOUCHES.equals(subjectConfirmation)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRequireHolderOfKeyWithMessageSignature() {
        return requireHolderOfKeyWithMessageSignature;
    }

    public void setRequireHolderOfKeyWithMessageSignature(boolean requireHolderOfKeyWithMessageSignature) {
        this.requireHolderOfKeyWithMessageSignature = requireHolderOfKeyWithMessageSignature;
    }

    public boolean isRequireSenderVouchesWithMessageSignature() {
        return requireSenderVouchesWithMessageSignature;
    }

    public void setRequireSenderVouchesWithMessageSignature(boolean requireSenderVouchesWithMessageSignature) {
        this.requireSenderVouchesWithMessageSignature = requireSenderVouchesWithMessageSignature;
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
     * The SAML assertion is a credential source.
     *
     * <P>Note that this credential source should only be trusted if proof of
     * possession is provided.</p>
     *
     * @return true
     */
    public boolean isCredentialSource() {
        return true;
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

    public Object clone() {
        RequestWssSaml assertion = (RequestWssSaml) super.clone();

        if (assertion.getAttributeStatement() != null) {
            assertion.setAttributeStatement((SamlAttributeStatement)assertion.getAttributeStatement().clone());
        }

        if (assertion.getAuthenticationStatement() != null) {
            assertion.setAuthenticationStatement((SamlAuthenticationStatement)assertion.getAuthenticationStatement().clone());
        }

        if (assertion.getAuthorizationStatement() != null) {
            assertion.setAuthorizationStatement((SamlAuthorizationStatement)assertion.getAuthorizationStatement().clone());
        }

        return assertion;
    }
}
