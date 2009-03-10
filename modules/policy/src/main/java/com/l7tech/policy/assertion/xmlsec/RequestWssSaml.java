package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 */
@ProcessesRequest
@RequiresSOAP(wss=true)
public class RequestWssSaml extends SamlPolicyAssertion implements SecurityHeaderAddressable {
    private String[] subjectConfirmations = new String[]{};
    private String[] nameFormats = new String[]{};
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireSenderVouchesWithMessageSignature = false;
    private boolean requireHolderOfKeyWithMessageSignature = false;

    private boolean checkAssertionValidity = true;

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
     * Get the Subject confirmations specified in this assertion
     *
     * @return the array of subject confirmations specified
     * @see com.l7tech.security.saml.SamlConstants#CONFIRMATION_HOLDER_OF_KEY
     * @see com.l7tech.security.saml.SamlConstants#CONFIRMATION_SENDER_VOUCHES
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

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
        this.recipientContext = recipientContext;
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

    public String describe() {
        String st = "Unknown Statement";
        if (getAuthenticationStatement() != null) {
            st = "Authentication Statement";
        } else if (getAttributeStatement() !=null) {
            st = "Attribute Statement";
        } else if (getAuthorizationStatement() !=null) {
            st = "Authorization Decision Statement";
        }

        if (this.getVersion() != null && this.getVersion() != 0) {
            st = "v" + this.getVersion() + " " + st;
        }

        return "SAML " + st + SecurityHeaderAddressableSupport.getActorSuffix(this);
    }
}
