package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.TimeUnit;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * The <code>RequireSaml</code> assertion describes the common SAML constraints
 * about subject, general SAML Assertion conditions and Statement constraints: for
 * authentication, authorization and attribute statements.
 *
 * This class does not contain anything SOAP specific.
 *
 * Extracted from RequireWssSaml
 */
public abstract class RequireSaml extends SamlPolicyAssertion implements MessageTargetable, UsesVariables, SetsVariables {
    public static final long UPPER_BOUND_FOR_MAX_EXPIRY = 100L * 365L * 86400L * 1000L; // 100 Years
    public static final String PREFIX_SAML_ATTR = "saml.attr";
    protected String[] subjectConfirmations = new String[]{};
    protected String subjectConfirmationDataRecipient;
    protected boolean subjectConfirmationDataCheckAddress = false;
    protected boolean subjectConfirmationDataCheckValidity = true;
    protected String[] nameFormats = new String[]{};
    protected boolean checkAssertionValidity = true;
    protected long maxExpiry = 0L;
    private TimeUnit timeUnit = TimeUnit.MINUTES;
    protected MessageTargetableSupport messageTargetableSupport = new MessageTargetableSupport(false);


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
     * @param checkAssertionValidity true to check assertion validity, false otherwise
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
    public VariableMetadata[] getVariablesSet() {
        return messageTargetableSupport.getMessageTargetVariablesSet().withVariables(
                new VariableMetadata(PREFIX_SAML_ATTR, true, true, PREFIX_SAML_ATTR, false)
        ).asArray();
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

    @Override
    public void setOtherTargetMessageVariable(String otherTargetMessageVariable) {
        messageTargetableSupport.setOtherTargetMessageVariable(otherTargetMessageVariable);
    }

    @Override
    public void setTarget(TargetMessageType target) {
        messageTargetableSupport.setTarget(target);
    }

    @Override
    public Object clone() {
        RequireSaml assertion = (RequireSaml) super.clone();
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

        return AssertionUtils.decorateName(this, getAssertionDisplayName() + " " + st);
    }

    public void copyFrom(RequireSaml requestWssSaml) {
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

    // - PROTECTED
    protected abstract String getAssertionDisplayName();

}
