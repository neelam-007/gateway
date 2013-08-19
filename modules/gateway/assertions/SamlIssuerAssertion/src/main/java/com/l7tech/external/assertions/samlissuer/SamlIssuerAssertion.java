package com.l7tech.external.assertions.samlissuer;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumSetTypeMapping;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.util.GoidUpgradeMapper;

import java.util.*;

import static com.l7tech.objectmodel.ExternalEntityHeader.ValueType.TEXT_ARRAY;

/**
 * @author alex
 */
public class SamlIssuerAssertion extends SamlPolicyAssertion implements PrivateKeyable, SetsVariables, UsesVariables, SamlElementGenericConfig {
    private int conditionsNotBeforeSecondsInPast = -1;
    private int conditionsNotOnOrAfterExpirySeconds = -1;
    /**
     * True if the assertion should be signed with an enveloped signature (i.e. within the assertion); unrelated to {@link #decorationTypes}.
     */
    private boolean signAssertion = true;
    private EnumSet<DecorationType> decorationTypes;
    private String subjectConfirmationMethodUri;
    private String subjectConfirmationDataRecipient;
    private String subjectConfirmationDataAddress;
    private String subjectConfirmationDataInResponseTo;
    private int subjectConfirmationDataNotBeforeSecondsInPast = -1;
    private int subjectConfirmationDataNotOnOrAfterExpirySeconds = -1;
    private NameIdentifierInclusionType nameIdentifierType = NameIdentifierInclusionType.FROM_CREDS;
    private String nameIdentifierFormat;
    private String nameIdentifierValue;
    private KeyInfoInclusionType signatureKeyInfoType = KeyInfoInclusionType.CERT;
    private KeyInfoInclusionType subjectConfirmationKeyInfoType = KeyInfoInclusionType.CERT;
    private boolean usesDefaultKeyStore = true;
    private Goid nonDefaultKeystoreId = GoidEntity.DEFAULT_GOID;
    private String keyAlias = "SSL";
    private String customIssuerValue;
    private String customIssuerFormat;
    private String customIssuerNameQualifier;

    private static final String META_INITIALIZED = SamlIssuerAssertion.class.getName() + ".metadataInitialized";


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
    public boolean samlProtocolUsage() {
        return false;
    }

    @Override
    public boolean includeIssuer() {
        return true;
    }

    @Override
    public void includeIssuer(boolean includeIssuer) {
        // nothing to do, always required.
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
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultKeystoreId) {
        this.nonDefaultKeystoreId = nonDefaultKeystoreId;
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultKeystoreId) {
        this.nonDefaultKeystoreId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultKeystoreId);
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    @Override
    public String getCustomIssuerValue() {
        return customIssuerValue;
    }

    @Override
    public void setCustomIssuerValue(String customIssuerValue) {
        this.customIssuerValue = customIssuerValue;
    }

    @Override
    public String getCustomIssuerFormat() {
        return customIssuerFormat;
    }

    @Override
    public void setCustomIssuerFormat(String customIssuerFormat) {
        this.customIssuerFormat = customIssuerFormat;
    }

    @Override
    public String getCustomIssuerNameQualifier() {
        return customIssuerNameQualifier;
    }

    @Override
    public void setCustomIssuerNameQualifier(String customIssuerNameQualifier) {
        this.customIssuerNameQualifier = customIssuerNameQualifier;
    }

    @Override
    public NameIdentifierInclusionType getNameIdentifierType() {
        return nameIdentifierType;
    }

    @Override
    public void setNameIdentifierType(NameIdentifierInclusionType nameIdentifierType) {
        this.nameIdentifierType = nameIdentifierType;
    }

    @Override
    public String getSubjectConfirmationMethodUri() {
        return subjectConfirmationMethodUri;
    }

    @Override
    public void setSubjectConfirmationMethodUri(String subjectConfirmationMethodUri) {
        this.subjectConfirmationMethodUri = subjectConfirmationMethodUri;
    }

    @Override
    public boolean isSignAssertion() {
        return signAssertion;
    }

    @Override
    public void setSignAssertion(boolean signAssertion) {
        this.signAssertion = signAssertion;
    }

    @Override
    public EnumSet<DecorationType> getDecorationTypes() {
        return decorationTypes;
    }

    @Override
    public void setDecorationTypes(EnumSet<DecorationType> decorationTypes) {
        this.decorationTypes = decorationTypes;
    }

    @Override
    public String getSubjectConfirmationDataRecipient() {
        return subjectConfirmationDataRecipient;
    }

    @Override
    public void setSubjectConfirmationDataRecipient( final String subjectConfirmationDataRecipient ) {
        this.subjectConfirmationDataRecipient = subjectConfirmationDataRecipient;
    }

    @Override
    public String getSubjectConfirmationDataAddress() {
        return subjectConfirmationDataAddress;
    }

    @Override
    public void setSubjectConfirmationDataAddress( final String subjectConfirmationDataAddress ) {
        this.subjectConfirmationDataAddress = subjectConfirmationDataAddress;
    }

    @Override
    public String getSubjectConfirmationDataInResponseTo() {
        return subjectConfirmationDataInResponseTo;
    }

    @Override
    public void setSubjectConfirmationDataInResponseTo( final String subjectConfirmationDataInResponseTo ) {
        this.subjectConfirmationDataInResponseTo = subjectConfirmationDataInResponseTo;
    }

    @Override
    public int getSubjectConfirmationDataNotBeforeSecondsInPast() {
        return subjectConfirmationDataNotBeforeSecondsInPast;
    }

    @Override
    public void setSubjectConfirmationDataNotBeforeSecondsInPast( final int subjectConfirmationDataNotBeforeSecondsInPast ) {
        this.subjectConfirmationDataNotBeforeSecondsInPast = subjectConfirmationDataNotBeforeSecondsInPast;
    }

    @Override
    public int getSubjectConfirmationDataNotOnOrAfterExpirySeconds() {
        return subjectConfirmationDataNotOnOrAfterExpirySeconds;
    }

    @Override
    public void setSubjectConfirmationDataNotOnOrAfterExpirySeconds( final int subjectConfirmationDataNotOnOrAfterExpirySeconds ) {
        this.subjectConfirmationDataNotOnOrAfterExpirySeconds = subjectConfirmationDataNotOnOrAfterExpirySeconds;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.NONE, mapValue = MigrationMappingSelection.REQUIRED, export = false, valueType = TEXT_ARRAY, resolver = PropertyResolver.Type.SERVER_VARIABLE)
    public String[] getVariablesUsed() {
        Set<String> varNames = new HashSet<String>();
        collectVars(varNames, customIssuerValue);
        collectVars(varNames, customIssuerNameQualifier);
        collectVars(varNames, nameIdentifierFormat);
        collectVars(varNames, nameIdentifierValue);
        collectVars(varNames, subjectConfirmationMethodUri);
        collectVars(varNames, audienceRestriction);
        collectVars(varNames, nameQualifier);
        collectVars(varNames, subjectConfirmationDataRecipient);
        collectVars(varNames, subjectConfirmationDataAddress);
        collectVars(varNames, subjectConfirmationDataInResponseTo);
        if (attributeStatement != null) {
            for (SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes()) {
                collectVars(varNames, attr.getNamespace());
                collectVars(varNames, attr.getNameFormat());
                collectVars(varNames, attr.getName());
                collectVars(varNames, attr.getValue());
            }
            collectVars(varNames, attributeStatement.getFilterExpression());
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

    @Override
    public int getConditionsNotBeforeSecondsInPast() {
        return conditionsNotBeforeSecondsInPast;
    }

    @Override
    public void setConditionsNotBeforeSecondsInPast(int conditionsNotBeforeSecondsInPast) {
        this.conditionsNotBeforeSecondsInPast = conditionsNotBeforeSecondsInPast;
    }

    @Override
    public int getConditionsNotOnOrAfterExpirySeconds() {
        return conditionsNotOnOrAfterExpirySeconds;
    }

    @Override
    public void setConditionsNotOnOrAfterExpirySeconds(int conditionsNotOnOrAfterExpirySeconds) {
        this.conditionsNotOnOrAfterExpirySeconds = conditionsNotOnOrAfterExpirySeconds;
    }

    @Override
    public void setNameIdentifierFormat(String formatUri) {
        this.nameIdentifierFormat = formatUri;
    }

    @Override
    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    @Override
    public void setNameIdentifierValue(String value) {
        this.nameIdentifierValue = value;
    }

    @Override
    public String getNameIdentifierValue() {
        return nameIdentifierValue;
    }

    @Override
    public KeyInfoInclusionType getSubjectConfirmationKeyInfoType() {
        return subjectConfirmationKeyInfoType;
    }

    @Override
    public void setSubjectConfirmationKeyInfoType(KeyInfoInclusionType subjectConfirmationKeyInfoType) {
        this.subjectConfirmationKeyInfoType = subjectConfirmationKeyInfoType;
    }

    public KeyInfoInclusionType getSignatureKeyInfoType() {
        return signatureKeyInfoType;
    }

    public void setSignatureKeyInfoType(KeyInfoInclusionType signatureKeyInfoType) {
        this.signatureKeyInfoType = signatureKeyInfoType;
    }

    final static String baseName = "Create SAML Token";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<SamlIssuerAssertion>(){
        @Override
        public String getAssertionName(SamlIssuerAssertion assertion, boolean decorate) {
            if(!decorate) return baseName;

            StringBuilder sb = new StringBuilder();
            sb.append("Create ");
            if (assertion.isSignAssertion()) sb.append("Signed ");
            final String uri = assertion.getSubjectConfirmationMethodUri();
            if (uri != null) {
                if (SamlIssuerAssertion.HOK_URIS.contains(uri)) {
                    sb.append("Holder-of-Key ");
                } else if (SamlIssuerAssertion.SV_URIS.contains(uri)) {
                    sb.append("Sender-Vouches ");
                } else if (SamlIssuerAssertion.BEARER_URIS.contains(uri)) {
                    sb.append("Bearer-Token ");
                }
            }

            sb.append("SAML Token");

            EnumSet<DecorationType> dts = assertion.getDecorationTypes();
            if (dts == null || dts.isEmpty()) return AssertionUtils.decorateName(assertion, sb);

            if (dts.contains(DecorationType.ADD_ASSERTION)) {
                sb.append(" and add to ");
                if (dts.contains(DecorationType.SIGN_BODY)) sb.append("Signed ");
                if (dts.contains(DecorationType.REQUEST))
                    sb.append("request");
                else
                    sb.append("response");
            }
            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Create and optionally sign a SAML token.");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Token Creation Wizard");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlissuer.console.SamlIssuerAssertionPropertiesEditor");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.samlissuer.AddSamlIssuerAssertionAdvice");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(NameIdentifierInclusionType.class, "nameIdentifierType"),
            new Java5EnumTypeMapping(KeyInfoInclusionType.class, "subjectConfirmationKeyInfoType"),
            new Java5EnumSetTypeMapping(EnumSet.class, DecorationType.class, "decorationTypes")
        )));
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, SamlIssuerAssertionValidator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        final SamlAttributeStatement attStmt = getAttributeStatement();
        final List<VariableMetadata> allVars = new ArrayList<VariableMetadata>();
        allVars.add(new VariableMetadata("issuedSamlAssertion", false, false, null, false, DataType.STRING));

        if (attStmt != null) {
            final String variablePrefix = attStmt.getVariablePrefix();
            allVars.addAll(Arrays.asList(
                    new VariableMetadata(variablePrefix + "." + SamlAttributeStatement.SUFFIX_UNKNOWN_ATTRIBUTE_NAMES, false, false, null, false, DataType.STRING),
                    new VariableMetadata(variablePrefix + "." + SamlAttributeStatement.SUFFIX_MISSING_ATTRIBUTE_NAMES, false, false, null, false, DataType.STRING),
                    new VariableMetadata(variablePrefix + "." + SamlAttributeStatement.SUFFIX_NO_ATTRIBUTES_ADDED, false, false, null, false, DataType.BOOLEAN),
                    new VariableMetadata(variablePrefix + "." + SamlAttributeStatement.SUFFIX_FILTERED_ATTRIBUTES, false, false, null, false, DataType.STRING)));

            if (getVersion() == 2) {
                allVars.add(new VariableMetadata(variablePrefix + "." + SamlAttributeStatement.SUFFIX_EXCLUDED_ATTRIBUTES, false, false, null, false, DataType.STRING));
            }
        }

        return allVars.toArray(new VariableMetadata[allVars.size()]);
    }
}
