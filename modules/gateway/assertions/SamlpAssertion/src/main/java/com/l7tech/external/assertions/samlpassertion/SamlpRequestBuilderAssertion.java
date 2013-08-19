package com.l7tech.external.assertions.samlpassertion;


import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.wsp.*;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public class SamlpRequestBuilderAssertion extends SamlProtocolAssertion
        implements UsesVariables, PrivateKeyable, SamlElementGenericConfig {
    private static final String META_INITIALIZED = SamlpRequestBuilderAssertion.class.getName() + ".metadataInitialized";

    private int conditionsNotBeforeSecondsInPast = -1;
    private int conditionsNotOnOrAfterExpirySeconds = -1;
    /**
     * True if the assertion should be signed with an enveloped signature (i.e. within the assertion).
     */
    private boolean signAssertion = true;
    private String subjectConfirmationMethodUri;
    private String subjectConfirmationDataRecipient;
    private String subjectConfirmationDataAddress;
    private String subjectConfirmationDataInResponseTo;
    private int subjectConfirmationDataNotBeforeSecondsInPast = -1;
    private int subjectConfirmationDataNotOnOrAfterExpirySeconds = -1;
    private NameIdentifierInclusionType nameIdentifierType = NameIdentifierInclusionType.FROM_CREDS;
    private String nameIdentifierFormat;
    private String customNameIdentifierFormat;
    private String nameIdentifierValue;
    private boolean encryptNameIdentifier;
    private XmlElementEncryptionConfig xmlEncryptConfig = new XmlElementEncryptionConfig();
    private KeyInfoInclusionType signatureKeyInfoType = KeyInfoInclusionType.CERT;
    private KeyInfoInclusionType subjectConfirmationKeyInfoType = KeyInfoInclusionType.CERT;

    // new stuff
    private Integer requestId;
    private String requestIdVariable;
    private String destinationAttribute;
    private String consentAttribute;
    private Integer evidence;
    private String evidenceVariable;

    private String customIssuerValue;
    private String customIssuerFormat;
    private String customIssuerNameQualifier;
    private boolean addIssuer = true;

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
        super(true);
        initTargetMessage();
    }

    public SamlpRequestBuilderAssertion(SamlAuthenticationStatement authnStmt) {
        super(true);
        this.authenticationStatement = authnStmt;
    }

    public SamlpRequestBuilderAssertion(SamlAttributeStatement attrStmt) {
        super(true);
        this.attributeStatement = attrStmt;
    }

    public SamlpRequestBuilderAssertion(SamlpAuthorizationStatement authzStmt) {
        super(true);
        this.authorizationStatement = authzStmt;
    }

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public Object clone() {
        final SamlpRequestBuilderAssertion clone = (SamlpRequestBuilderAssertion) super.clone();
        clone.xmlEncryptConfig = (XmlElementEncryptionConfig) xmlEncryptConfig.clone();
        return clone;
    }

    private void initTargetMessage() {
        this.setTarget(TargetMessageType.OTHER);
        this.setOtherTargetMessageVariable("samlpRequest.message");
        this.setSourceUsedByGateway(false);
    }

    @Override
    public boolean samlProtocolUsage() {
        return true;
    }

    /**
     * Independent property separate from the interface defining includeIssuer()
     * @return if user configured the Issuer to be added. Will never apply to SAML 1.1
     */
    public boolean isAddIssuer() {
        return addIssuer;
    }

    public void setAddIssuer(boolean addIssuer) {
        this.addIssuer = addIssuer;
    }

    @Override
    public boolean includeIssuer() {
        return getVersion() == 2 && addIssuer;
    }

    @Override
    public void includeIssuer(boolean includeIssuer) {
        addIssuer = includeIssuer;
    }

    @Override
    public String getCustomIssuerValue() {
        return customIssuerValue;
    }

    @Override
    public void setCustomIssuerValue(@Nullable String customIssuerValue) {
        this.customIssuerValue = customIssuerValue;
    }

    @Override
    public String getCustomIssuerFormat() {
        return customIssuerFormat;
    }

    @Override
    public void setCustomIssuerFormat(@Nullable String customIssuerFormat) {
        this.customIssuerFormat = customIssuerFormat;
    }

    @Override
    public String getCustomIssuerNameQualifier() {
        return customIssuerNameQualifier;
    }

    @Override
    public void setCustomIssuerNameQualifier(@Nullable String customIssuerNameQualifier) {
        this.customIssuerNameQualifier = customIssuerNameQualifier;
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

    /**
     * Clear properties only related to an authorization decision request.
     */
    public void removeAuthorizationOnlyProperties() {
        evidence = null;
        evidenceVariable = null;
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

    /**
     * Left in for backwards compatibility.
     * @param signRequest
     */
    @Deprecated
    public void setSignRequest(boolean signRequest) {
        this.signAssertion = signRequest;
    }

    @Deprecated
    @Override
    public EnumSet<SamlElementGenericConfig.DecorationType> getDecorationTypes() {
        return null;
    }

    @Deprecated
    @Override
    public void setDecorationTypes(EnumSet<SamlElementGenericConfig.DecorationType> decorationTypes) {
        throw new UnsupportedOperationException("Not supported yet in SAML Protocol");
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

    public boolean isEncryptNameIdentifier() {
        return encryptNameIdentifier;
    }

    public void setEncryptNameIdentifier(boolean encryptNameIdentifier) {
        this.encryptNameIdentifier = encryptNameIdentifier;
    }

    @NotNull
    public XmlElementEncryptionConfig getXmlEncryptConfig() {
        return xmlEncryptConfig;
    }

    public void setXmlEncryptConfig(@NotNull XmlElementEncryptionConfig xmlEncryptConfig) {
        this.xmlEncryptConfig = xmlEncryptConfig;
    }

    @Override
    protected VariablesUsed doGetVariablesUsed() {
        final VariablesUsed variablesUsed = super.doGetVariablesUsed().withExpressions(
                nameIdentifierFormat,
                nameIdentifierValue,
                subjectConfirmationMethodUri,
                subjectConfirmationDataAddress,
                subjectConfirmationDataRecipient,
                subjectConfirmationDataInResponseTo,
                audienceRestriction,
                nameQualifier,
                // new stuff
                requestIdVariable,
                destinationAttribute,
                consentAttribute,
                customNameIdentifierFormat,
                customIssuerValue,
                customIssuerFormat,
                customIssuerNameQualifier
        ).withVariables( evidenceVariable );

        variablesUsed.addVariables(xmlEncryptConfig.getVariablesUsed());

        if ( attributeStatement != null ) {
            for ( final SamlAttributeStatement.Attribute attr : attributeStatement.getAttributes() ) {
                variablesUsed.addExpressions(
                        attr.getNamespace(),
                        attr.getNameFormat(),
                        attr.getName(),
                        attr.getValue(),
                        attr.getFriendlyName()
                );
            }
        }

        if ( authorizationStatement != null ) {
            variablesUsed.addExpressions( authorizationStatement.getActions() );
            variablesUsed.addExpressions( authorizationStatement.getResource() );
        }

        // TODO how could one parameterize the authentication statement at all?
        return variablesUsed;
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
    public void setNameIdentifierFormat(@Nullable String formatUri) {
        this.nameIdentifierFormat = formatUri;
    }

    @Override
    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    public void setCustomNameIdentifierFormat(@Nullable String customNameIdentifierFormat) {
        this.customNameIdentifierFormat = customNameIdentifierFormat;
    }

    public String getCustomNameIdentifierFormat() {
        return customNameIdentifierFormat;
    }

    @Override
    public void setNameIdentifierValue(@Nullable String value) {
        this.nameIdentifierValue = value;
    }

    @Override
    @Nullable
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

    protected boolean usesDefaultKeyStore = true;
    protected Goid nonDefaultKeystoreId;
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
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultId);
    }

    @Override
    public String getKeyAlias() {
        return keyId;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    final static String baseName = "Build SAML Protocol Request";

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

            if (assertion.isSignAssertion()) {
                sb.append("; Sign Request");
            }
            return AssertionUtils.decorateName(assertion, sb);
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        // Add to palette folder(s)
        //   accessControl, transportLayerSecurity, xmlSecurity, xml, routing,
        //   misc, audit, policyLogic, threatProtection
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Build a SAML Protocol Request.");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Protocol Request Wizard");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.SamlpRequestBuilderAssertionPropertiesEditor");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(NameIdentifierInclusionType.class, "nameIdentifierType"),
            new Java5EnumTypeMapping(KeyInfoInclusionType.class, "subjectConfirmationKeyInfoType"),
                /*Note this property should never be serialized*/
            new Java5EnumSetTypeMapping(EnumSet.class, SamlElementGenericConfig.DecorationType.class, "decorationTypes"),
            new BeanTypeMapping(SamlpAuthorizationStatement.class, "samlpAuthorizationInfo"),
            new ArrayTypeMapping(new String[0], "actions")
        )));

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, SamlpRequestBuilderAssertionValidator.class.getName());
        // request default feature set name for our class name, since we are a known optional module
        // that is, we want our required feature set to be "assertion:FtpCredential" rather than "set:modularAssertions"
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
