package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.security.saml.SamlConstants;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;

/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 */
@RequiresSOAP(wss=true)
public class RequireWssSaml extends SamlPolicyAssertion implements MessageTargetable, UsesVariables, SecurityHeaderAddressable {
    private String[] subjectConfirmations = new String[]{};
    private String[] nameFormats = new String[]{};
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireSenderVouchesWithMessageSignature = false;
    private boolean requireHolderOfKeyWithMessageSignature = false;
    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport();
    private boolean checkAssertionValidity = true;

    public RequireWssSaml() {
    }

    public RequireWssSaml(RequireWssSaml requestWssSaml) {
        copyFrom(requestWssSaml);
    }

    /**
     * Factory method that creates the Holder-Of-Key assertion
     *
     * @return the RequestWssSaml with Holder-Of-Key subject confirmation
     */
    public static RequireWssSaml newHolderOfKey() {
        RequireWssSaml ass = new RequireWssSaml();
        ass.setVersion(0);
        ass.setRequireHolderOfKeyWithMessageSignature(true);
        ass.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_HOLDER_OF_KEY});
        return ass;
    }

    /**
         * Factory method that creates the Sender-Vouches assertion
         *
         * @return the RequestWssSaml with Sender-Vouches subject confirmation
         */
    public static RequireWssSaml newSenderVouches() {
        RequireWssSaml ass = new RequireWssSaml();
        ass.setVersion(0);
        ass.setRequireSenderVouchesWithMessageSignature(true);
        ass.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_SENDER_VOUCHES});
        return ass;
    }

    public void copyFrom(RequireWssSaml requestWssSaml) {
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
        this.setTarget(requestWssSaml.getTarget());
        this.setOtherTargetMessageVariable(requestWssSaml.getOtherTargetMessageVariable());
        this.setEnabled(requestWssSaml.isEnabled());
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

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
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

    @Override
    public String getOtherTargetMessageVariable() {
        return messageTargetableSupport.getOtherTargetMessageVariable();
    }

    @Override
    public TargetMessageType getTarget() {
        return messageTargetableSupport.getTarget();
    }

    @Override
    public String getTargetName() {
        return messageTargetableSupport.getTargetName();
    }

    @Override
    public String[] getVariablesUsed() {
        return messageTargetableSupport.getVariablesUsed();
    }

    @Override
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }

    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    /**
     * The SAML assertion is a credential source.
     *
     * <P>Note that this credential source should only be trusted if proof of
     * possession is provided.</p>
     *
     * @return true
     */
    @Override
    public boolean isCredentialSource() {
        return true;
    }

    final static String baseName = "Require SAML Token Profile";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<RequireWssSaml>(){
        @Override
        public String getAssertionName( final RequireWssSaml assertion, final boolean decorate) {
            return (decorate)? assertion.describe(): baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Gateway checks for the SAML Statements Security properties");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");
        meta.put(AssertionMetadata.ASSERTION_FACTORY, new Functions.Unary<RequireWssSaml, RequireWssSaml>(){
            @Override
            public RequireWssSaml call( final RequireWssSaml requestWssSaml ) {
                return RequireWssSaml.newHolderOfKey();
            }
        });
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddRequireWssSamlAdvice");

        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Token Profile Wizard");        
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/Edit16.gif");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.EditRequireWssSamlAction");
        
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);
        meta.put(AssertionMetadata.CLIENT_ASSERTION_CLASSNAME, "com.l7tech.proxy.policy.assertion.xmlsec.ClientRequestWssSaml");
        meta.put(AssertionMetadata.CLIENT_ASSERTION_POLICY_ICON, "com/l7tech/console/resources/xmlWithCert16.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.SamlStatementValidator");

        return meta;
    }

    @Override
    public Object clone() {
        RequireWssSaml assertion = (RequireWssSaml) super.clone();

        if (assertion.getAttributeStatement() != null) {
            assertion.setAttributeStatement((SamlAttributeStatement)assertion.getAttributeStatement().clone());
        }

        if (assertion.getAuthenticationStatement() != null) {
            assertion.setAuthenticationStatement((SamlAuthenticationStatement)assertion.getAuthenticationStatement().clone());
        }

        if (assertion.getAuthorizationStatement() != null) {
            assertion.setAuthorizationStatement((SamlAuthorizationStatement)assertion.getAuthorizationStatement().clone());
        }

        assertion.messageTargetableSupport = new MessageTargetableSupport( messageTargetableSupport );

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

        return AssertionUtils.decorateName(this, "Require SAML Token " + st);
    }
}
