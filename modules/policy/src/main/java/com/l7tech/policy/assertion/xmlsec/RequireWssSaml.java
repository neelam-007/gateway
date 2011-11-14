package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.validator.ValidatorFlag;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.Functions;
import com.l7tech.util.TimeUnit;

import java.util.regex.Pattern;
import java.util.Set;
import java.util.EnumSet;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * The <code>RequestWssSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 */
@RequiresSOAP(wss=true)
public class RequireWssSaml extends SamlPolicyAssertion implements MessageTargetable, UsesVariables, SetsVariables, SecurityHeaderAddressable {
    public static final String PREFIX_SAML_ATTR = "saml.attr";
    public static final long UPPER_BOUND_FOR_MAX_EXPIRY = 100L * 365L * 86400L * 1000L; // 100 Years

    private String[] subjectConfirmations = new String[]{};
    private String subjectConfirmationDataRecipient;
    private boolean subjectConfirmationDataCheckAddress = false;
    private boolean subjectConfirmationDataCheckValidity = true;
    private String[] nameFormats = new String[]{};
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean requireSenderVouchesWithMessageSignature = false;
    private boolean requireHolderOfKeyWithMessageSignature = false;
    private MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport(false);
    private boolean checkAssertionValidity = true;
    private long maxExpiry = 0L;
    private TimeUnit timeUnit = TimeUnit.MINUTES;

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
        ass.setSubjectConfirmationDataCheckAddress(true);
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
        ass.setSubjectConfirmationDataCheckAddress(true);
        return ass;
    }

    public void copyFrom(RequireWssSaml requestWssSaml) {
        this.setAssertionComment(requestWssSaml.getAssertionComment());
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
        this.setSubjectConfirmationDataCheckAddress(requestWssSaml.isSubjectConfirmationDataCheckAddress());
        this.setSubjectConfirmationDataCheckValidity(requestWssSaml.isSubjectConfirmationDataCheckValidity());
        this.setSubjectConfirmationDataRecipient(requestWssSaml.getSubjectConfirmationDataRecipient());
        this.setMaxExpiry(requestWssSaml.getMaxExpiry());
        this.setTimeUnit(requestWssSaml.getTimeUnit());
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

    /**
     * @return an integer value for the maximum lifetime of the SAML assertion (unit: milliseconds)
     */
    public long getMaxExpiry() {
        return maxExpiry;
    }

    /**
     * Set the maximum lifetime of the SAML assertion (unit: milliseconds)
     * 
     * @param maxExpiry: an integer value for the maximum lifetime of the SAML assertion.
     */
    public void setMaxExpiry(long maxExpiry) {
        this.maxExpiry = maxExpiry;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
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

    public String getSubjectConfirmationDataRecipient() {
        return subjectConfirmationDataRecipient;
    }

    public void setSubjectConfirmationDataRecipient( final String subjectConfirmationDataRecipient ) {
        this.subjectConfirmationDataRecipient = subjectConfirmationDataRecipient;
    }

    public boolean isSubjectConfirmationDataCheckAddress() {
        return subjectConfirmationDataCheckAddress;
    }

    public void setSubjectConfirmationDataCheckAddress( final boolean subjectConfirmationDataCheckAddress ) {
        this.subjectConfirmationDataCheckAddress = subjectConfirmationDataCheckAddress;
    }

    public boolean isSubjectConfirmationDataCheckValidity() {
        return subjectConfirmationDataCheckValidity;
    }

    public void setSubjectConfirmationDataCheckValidity( final boolean subjectConfirmationDataCheckValidity ) {
        this.subjectConfirmationDataCheckValidity = subjectConfirmationDataCheckValidity;
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
    public boolean isTargetModifiedByGateway() {
        return messageTargetableSupport.isTargetModifiedByGateway();
    }

    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    @Override
    public String[] getVariablesUsed() {
        return messageTargetableSupport.getMessageTargetVariablesUsed().withExpressions(
                subjectConfirmationDataRecipient,
                nameQualifier,
                audienceRestriction,
                (authenticationStatement != null)? authenticationStatement.getCustomAuthenticationMethods(): ""
        ).asArray();
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

    @Override
    public VariableMetadata[] getVariablesSet() {
        return messageTargetableSupport.getMessageTargetVariablesSet().withVariables(
                new VariableMetadata(PREFIX_SAML_ATTR, true, true, PREFIX_SAML_ATTR, false)
        ).asArray();
    }

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
