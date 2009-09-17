/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.samlpassertion;


import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.*;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.*;

/**
 * This assertion contains the configuration properties for building a SAML-Protocol (SAMLP) request message.
 *
 * The request will consist of one of three possible message payloads:
 * <ul>
 * <il>Authentication Request</il>
 * <il>Authorization Request</il>
 * <il>Attribute Query Request</il>
 * </ul>
 *
 * @author vchan
 */
public class SamlpRequestBuilderAssertion extends SamlProtocolAssertion implements SetsVariables, UsesVariables, PrivateKeyable {
    private int conditionsNotBeforeSecondsInPast = -1;
    private int conditionsNotOnOrAfterExpirySeconds = -1;
    /**
     * True if the assertion should be signed with an enveloped signature (i.e. within the assertion); unrelated to {@link #decorationTypes}.
     */
    private boolean signRequest = true;
    private EnumSet<DecorationType> decorationTypes;
    private String subjectConfirmationMethodUri;
    private NameIdentifierInclusionType nameIdentifierType = NameIdentifierInclusionType.FROM_CREDS;
    private String nameIdentifierFormat;
    private String nameIdentifierValue;
    private KeyInfoInclusionType signatureKeyInfoType = KeyInfoInclusionType.CERT;
    private KeyInfoInclusionType subjectConfirmationKeyInfoType = KeyInfoInclusionType.CERT;


    // new stuff
    private Integer requestId;
    private String requestIdVariable;
    private String destinationAttribute;
    private String consentAttribute;
    private Integer evidence;
    private String evidenceVariable;

    public static final Set<String> HOK_URIS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        SamlConstants.CONFIRMATION_HOLDER_OF_KEY,
        SamlConstants.CONFIRMATION_SAML2_HOLDER_OF_KEY
    )));

    public static final Set<String> SV_URIS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        SamlConstants.CONFIRMATION_SENDER_VOUCHES,
        SamlConstants.CONFIRMATION_SAML2_SENDER_VOUCHES
    )));

    public static final Set<String> BEARER_URIS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        SamlConstants.CONFIRMATION_BEARER,
        SamlConstants.CONFIRMATION_SAML2_BEARER
    )));

    public SamlpRequestBuilderAssertion() {
        initTargetMessage();
    }

    public SamlpRequestBuilderAssertion(SamlAuthenticationStatement authnStmt) {
        this.authenticationStatement = authnStmt;
    }

    public SamlpRequestBuilderAssertion(SamlAttributeStatement attrStmt) {
        this.attributeStatement = attrStmt;
    }

    public SamlpRequestBuilderAssertion(SamlpAuthorizationStatement authzStmt) {
        this.authorizationStatement = authzStmt;
    }

    private void initTargetMessage() {
        this.setTarget(TargetMessageType.OTHER);
        this.setOtherTargetMessageVariable("samlpRequest.message");
    }

    public static enum DecorationType {
        /** Apply decorations to request */
        REQUEST,

        /** Apply decorations to response */
        RESPONSE,

        /** Insert the assertion into the message as a child of the Security header, but don't sign it */
        ADD_ASSERTION,

        /** Insert the assertion into the message, and ensure it's signed with a message-level signature */
        SIGN_ASSERTION,

        /** Sign the SOAP Body */
        SIGN_BODY,
    }

    public Integer getRequestId() {
        return this.requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public String getRequestIdVariable() {
        return this.requestIdVariable;
    }

    public void setRequestIdVariable(String requestIdVariable) {
        this.requestIdVariable = requestIdVariable;
    }

    public String getDestinationAttribute() {
        return destinationAttribute;
    }

    public void setDestinationAttribute(String destinationAttribute) {
        this.destinationAttribute = checkEmpty(destinationAttribute);
    }

    public String getConsentAttribute() {
        return consentAttribute;
    }

    public void setConsentAttribute(String consentAttribute) {
        this.consentAttribute = checkEmpty(consentAttribute);
    }

    public Integer getEvidence() {
        return evidence;
    }

    public void setEvidence(Integer evidence) {
        this.evidence = evidence;
    }

    public String getEvidenceVariable() {
        return evidenceVariable;
    }

    public void setEvidenceVariable(String evidenceVariable) {
        this.evidenceVariable = evidenceVariable;
    }

    public NameIdentifierInclusionType getNameIdentifierType() {
        return nameIdentifierType;
    }

    public void setNameIdentifierType(NameIdentifierInclusionType nameIdentifierType) {
        this.nameIdentifierType = nameIdentifierType;
    }

    public String getSubjectConfirmationMethodUri() {
        return subjectConfirmationMethodUri;
    }

    public void setSubjectConfirmationMethodUri(String subjectConfirmationMethodUri) {
        this.subjectConfirmationMethodUri = subjectConfirmationMethodUri;
    }

    public boolean isSignRequest() {
        return signRequest;
    }

    public void setSignRequest(boolean signRequest) {
        this.signRequest = signRequest;
    }

    public EnumSet<DecorationType> getDecorationTypes() {
        return decorationTypes;
    }

    public void setDecorationTypes(EnumSet<DecorationType> decorationTypes) {
        this.decorationTypes = decorationTypes;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        Set<String> varNames = new HashSet<String>();
        collectVars(varNames, nameIdentifierFormat);
        collectVars(varNames, nameIdentifierValue);
        collectVars(varNames, subjectConfirmationMethodUri);
        collectVars(varNames, audienceRestriction);
        collectVars(varNames, nameQualifier);
        // new stuff
        collectVars(varNames, requestIdVariable);
        collectVars(varNames, destinationAttribute);
        collectVars(varNames, consentAttribute);
        if (evidenceVariable != null && !evidenceVariable.isEmpty())
            collectVars(varNames, "${"+evidenceVariable+"}");
        collectVars(varNames, getTargetName());
        if (attributeStatement != null) {
            for (SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes()) {
                collectVars(varNames, attr.getNamespace());
                collectVars(varNames, attr.getNameFormat());
                collectVars(varNames, attr.getName());
                collectVars(varNames, attr.getValue());
                collectVars(varNames, attr.getFriendlyName());
            }
        }

        if (authorizationStatement != null) {
            for (String action : authorizationStatement.getActions()) {
                collectVars(varNames, action);
            }
            collectVars(varNames, authorizationStatement.getResource());
        }

        // TODO how could one parameterize the authentication statement at all?
        return varNames.toArray(new String[varNames.size()]);
    }

    private void collectVars(Set<String> varNames, String s) {
        if (s == null || s.length() == 0) return;
        String[] vars = Syntax.getReferencedNames(s);
        varNames.addAll(Arrays.asList(vars));
    }

    public int getConditionsNotBeforeSecondsInPast() {
        return conditionsNotBeforeSecondsInPast;
    }

    public void setConditionsNotBeforeSecondsInPast(int conditionsNotBeforeSecondsInPast) {
        this.conditionsNotBeforeSecondsInPast = conditionsNotBeforeSecondsInPast;
    }

    public int getConditionsNotOnOrAfterExpirySeconds() {
        return conditionsNotOnOrAfterExpirySeconds;
    }

    public void setConditionsNotOnOrAfterExpirySeconds(int conditionsNotOnOrAfterExpirySeconds) {
        this.conditionsNotOnOrAfterExpirySeconds = conditionsNotOnOrAfterExpirySeconds;
    }

    public void setNameIdentifierFormat(String formatUri) {
        this.nameIdentifierFormat = formatUri;
    }

    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    public void setNameIdentifierValue(String value) {
        this.nameIdentifierValue = value;
    }

    public String getNameIdentifierValue() {
        return nameIdentifierValue;
    }

    public KeyInfoInclusionType getSubjectConfirmationKeyInfoType() {
        return subjectConfirmationKeyInfoType;
    }

    public void setSubjectConfirmationKeyInfoType(KeyInfoInclusionType subjectConfirmationKeyInfoType) {
        this.subjectConfirmationKeyInfoType = subjectConfirmationKeyInfoType;
    }

    public KeyInfoInclusionType getSignatureKeyInfoType() {
        return signatureKeyInfoType;
    }

    public void setSignatureKeyInfoType(KeyInfoInclusionType signatureKeyInfoType) {
        this.signatureKeyInfoType = signatureKeyInfoType;
    }

    protected boolean usesDefaultKeyStore = true;
    protected long nonDefaultKeystoreId;
    protected String keyId;

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Override
    public String getKeyAlias() {
        return keyId;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    final static String baseName = "Build SAML Protocol Message";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SamlpRequestBuilderAssertion>(){
        @Override
        public String getAssertionName( final SamlpRequestBuilderAssertion assertion, boolean decorate) {
            if(!decorate) return baseName;
            StringBuilder sb = new StringBuilder(baseName);

            if (assertion.getAuthenticationStatement() != null)
                sb.append(" (Authentication)");
            else if (assertion.getAuthorizationStatement() != null)
                sb.append(" (Authorization Decision)");
            else if (assertion.getAttributeStatement() != null)
                sb.append(" (Attribute Query)");

            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Create a SAML Protocol request.");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Protocol Message Wizard");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.SamlpRequestBuilderAssertionPropertiesEditor");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(NameIdentifierInclusionType.class, "nameIdentifierType"),
            new Java5EnumTypeMapping(KeyInfoInclusionType.class, "subjectConfirmationKeyInfoType"),
            new Java5EnumSetTypeMapping(EnumSet.class, DecorationType.class, "decorationTypes"),
            new BeanTypeMapping(SamlpAuthorizationStatement.class, "samlpAuthorizationInfo"),
            new ArrayTypeMapping(new String[0], "actions")
        )));

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, SamlpRequestBuilderAssertionValidator.class.getName());
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:FtpCredential" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        if (getTarget() == TargetMessageType.OTHER && getOtherTargetMessageVariable() != null) {
            return new VariableMetadata[] { new VariableMetadata(getOtherTargetMessageVariable(), false, false, null, false, DataType.MESSAGE) };
        }
        return new VariableMetadata[0];
    }
}