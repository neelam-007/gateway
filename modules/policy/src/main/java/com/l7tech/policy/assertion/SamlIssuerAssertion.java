/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion;

import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.util.Functions;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.validator.SamlIssuerAssertionValidator;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.Java5EnumSetTypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

import java.util.*;

/**
 * @author alex
 */
public class SamlIssuerAssertion extends SamlPolicyAssertion implements PrivateKeyable, SetsVariables, UsesVariables {
    private int conditionsNotBeforeSecondsInPast = -1;
    private int conditionsNotOnOrAfterExpirySeconds = -1;
    /**
     * True if the assertion should be signed with an enveloped signature (i.e. within the assertion); unrelated to {@link #decorationTypes}.
     */
    private boolean signAssertion = true;
    private EnumSet<DecorationType> decorationTypes;
    private String subjectConfirmationMethodUri;
    private NameIdentifierInclusionType nameIdentifierType = NameIdentifierInclusionType.FROM_CREDS;
    private String nameIdentifierFormat;
    private String nameIdentifierValue;
    private KeyInfoInclusionType signatureKeyInfoType = KeyInfoInclusionType.CERT;
    private KeyInfoInclusionType subjectConfirmationKeyInfoType = KeyInfoInclusionType.CERT;
    private boolean usesDefaultKeyStore = true;
    private long nonDefaultKeystoreId = -1;
    private String keyAlias = "SSL";

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

    public SamlIssuerAssertion() {
    }

    public SamlIssuerAssertion(SamlAuthenticationStatement authnStmt) {
        this.authenticationStatement = authnStmt;
    }

    public SamlIssuerAssertion(SamlAttributeStatement attrStmt) {
        this.attributeStatement = attrStmt;
    }

    public SamlIssuerAssertion(SamlAuthorizationStatement authzStmt) {
        this.authorizationStatement = authzStmt;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefaultKeyStore) {
        this.usesDefaultKeyStore = usesDefaultKeyStore;
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultKeystoreId) {
        this.nonDefaultKeystoreId = nonDefaultKeystoreId;
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
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

    public boolean isSignAssertion() {
        return signAssertion;
    }

    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
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
        if (attributeStatement != null) {
            for (SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes()) {
                collectVars(varNames, attr.getNamespace());
                collectVars(varNames, attr.getNameFormat());
                collectVars(varNames, attr.getName());
                collectVars(varNames, attr.getValue());
            }
        }

        if (authorizationStatement != null) {
            collectVars(varNames, authorizationStatement.getAction());
            collectVars(varNames, authorizationStatement.getActionNamespace());
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.saml.SamlIssuerAssertionPropertiesEditor");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.console.tree.policy.advice.AddSamlIssuerAssertionAdvice");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, SamlIssuerAssertion>() {
            @Override
            public String call(SamlIssuerAssertion sia) {
                StringBuilder sb = new StringBuilder();
                sb.append("Issue ");
                if (sia.isSignAssertion()) sb.append("signed ");
                final String uri = sia.getSubjectConfirmationMethodUri();
                if (uri != null) {
                    if (SamlIssuerAssertion.HOK_URIS.contains(uri)) {
                        sb.append("Holder-of-Key ");
                    } else if (SamlIssuerAssertion.SV_URIS.contains(uri)) {
                        sb.append("Sender-Vouches ");
                    } else if (SamlIssuerAssertion.BEARER_URIS.contains(uri)) {
                        sb.append("Bearer-Token ");
                    }
                }
                sb.append("SAML Assertion");
                EnumSet<DecorationType> dts = sia.getDecorationTypes();
                if (dts == null || dts.isEmpty()) return AssertionUtils.decorateName(sia, sb);

                if (dts.contains(DecorationType.ADD_ASSERTION)) {
                    sb.append(" and add to ");
                    if (dts.contains(DecorationType.SIGN_BODY)) sb.append("signed ");
                    if (dts.contains(DecorationType.REQUEST))
                        sb.append("request");
                    else
                        sb.append("response");
                }
                return AssertionUtils.decorateName(sia, sb);
            }
        });
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(NameIdentifierInclusionType.class, "nameIdentifierType"),
            new Java5EnumTypeMapping(KeyInfoInclusionType.class, "subjectConfirmationKeyInfoType"),
            new Java5EnumSetTypeMapping(EnumSet.class, DecorationType.class, "decorationTypes")
        )));
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, SamlIssuerAssertionValidator.class.getName());
        meta.put(AssertionMetadata.SHORT_NAME, "SAML Issuer");
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] { new VariableMetadata("issuedSamlAssertion", false, false, null, false, DataType.STRING) };
    }
}
