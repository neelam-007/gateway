package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.Functions;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.EnumSet;


/**
 * Additional information required for WS-Security SOAP processing.
 *
 * This class and sub classes represent configuration for SAML tokens acquired via WS-Security.
 */
@RequiresSOAP(wss=true)
public class RequireWssSaml extends RequireSaml implements SecurityHeaderAddressable {

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireSenderVouchesWithMessageSignature = false;
    private boolean requireHolderOfKeyWithMessageSignature = false;

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
        setNewHolderOfKeyProperties(ass);
        return ass;
    }

    protected static void setNewHolderOfKeyProperties(RequireWssSaml ass) {
        ass.setVersion(0);
        ass.setRequireHolderOfKeyWithMessageSignature(true);
        ass.setSubjectConfirmations(new String[]{SamlConstants.CONFIRMATION_HOLDER_OF_KEY});
        ass.setSubjectConfirmationDataCheckAddress(true);
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
        ass.setSubjectConfirmationDataCheckAddress(true);
        return ass;
    }

    public void copyFrom(RequireWssSaml requestWssSaml) {
        super.copyFrom(requestWssSaml);
        this.setRecipientContext(requestWssSaml.getRecipientContext());
        this.setRequireHolderOfKeyWithMessageSignature(requestWssSaml.isRequireHolderOfKeyWithMessageSignature());
        this.setRequireSenderVouchesWithMessageSignature(requestWssSaml.isRequireSenderVouchesWithMessageSignature());
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
        meta.put(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY, new Functions.Unary<Set<ValidatorFlag>, RequireWssSaml>() {
            @Override
            public Set<ValidatorFlag> call(RequireWssSaml saml) {
                if (saml != null) {
                    final String[] scs = saml.getSubjectConfirmations();
                    // RequestWssSaml is only a cert-based credential source if it's Holder of Key and has the signature constraint
                    if (scs.length == 1 && SamlPolicyAssertion.HOK_URIS.contains(scs[0]) && saml.isRequireHolderOfKeyWithMessageSignature()) {
                        return EnumSet.of(ValidatorFlag.GATHERS_X509_CREDENTIALS);
                    }
                }
                return EnumSet.noneOf(ValidatorFlag.class);
            }
        });

        return meta;
    }

    // - PROTECTED

    @Override
    protected String getAssertionDisplayName() {
        return "Require SAML Token";
    }

    // - PRIVATE

    private static final Pattern converter = Pattern.compile("[^a-zA-Z0-9]");

    /**
     * Convert a SAML attribute name into a String suitable for use as a context variable suffix.  This
     * just converts any non-alphanumeric characters into an underscore, and prepends an "n" if the name
     * starts with a digit.
     *
     * @param attributeName the name to convert, ie "Ranking 1.2 Person of Rank Level"
     * @return the value converted into a context variable suffix, ie "ranking_1_2_person_of_rank_level".  Never null.
     */
    public static String toContextVariableName(String attributeName) {
        if (attributeName == null) throw new NullPointerException();
        attributeName = attributeName.trim();
        if (attributeName.length() < 1) throw new IllegalArgumentException();
        String ret = converter.matcher(attributeName).replaceAll("_");
        return Character.isDigit(ret.charAt(0)) ? "n" + ret : ret;
    }
}
