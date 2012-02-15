package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.external.assertions.samlpassertion.server.ProtocolRequestUtilities;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.security.saml.SamlConstants;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Assertion to process an AttributeQuery SAML Protocol request.
 */
public class ProcessSamlAttributeQueryRequestAssertion extends MessageTargetableAssertion {

    // - PUBLIC
    public static final String DEFAULT_PREFIX = "attrQuery";

    public static final Collection<String> VARIABLE_SUFFIXES_V2 = Collections.unmodifiableCollection(Arrays.asList(
            ProtocolRequestUtilities.SUFFIX_ATTRIBUTES,
            ProtocolRequestUtilities.SUFFIX_SUBJECT,
            ProtocolRequestUtilities.SUFFIX_SUBJECT_NAME_QUALIFIER,
            ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_NAME_QUALIFIER,
            ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT,
            ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_PROVIDED_ID,
            ProtocolRequestUtilities.SUFFIX_ID,
            ProtocolRequestUtilities.SUFFIX_VERSION,
            ProtocolRequestUtilities.SUFFIX_ISSUE_INSTANT,
            ProtocolRequestUtilities.SUFFIX_DESTINATION,
            ProtocolRequestUtilities.SUFFIX_CONSENT,
            ProtocolRequestUtilities.SUFFIX_ISSUER,
            ProtocolRequestUtilities.SUFFIX_ISSUER_NAME_QUALIFIER,
            ProtocolRequestUtilities.SUFFIX_ISSUER_SP_NAME_QUALIFIER,
            ProtocolRequestUtilities.SUFFIX_ISSUER_FORMAT,
            ProtocolRequestUtilities.SUFFIX_ISSUER_SP_PROVIDED_ID
    ));

    public static final Set<String> SUPPORTED_SUBJECT_FORMATS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    SamlConstants.NAMEIDENTIFIER_UNSPECIFIED,
                    SamlConstants.NAMEIDENTIFIER_EMAIL,
                    SamlConstants.NAMEIDENTIFIER_X509_SUBJECT,
                    SamlConstants.NAMEIDENTIFIER_WINDOWS,
                    SamlConstants.NAMEIDENTIFIER_KERBEROS,
                    SamlConstants.NAMEIDENTIFIER_ENTITY
                    ))
    );

    public static final Set<String> SUPPORTED_ATTRIBUTE_NAMEFORMATS = Collections.unmodifiableSet(
            new LinkedHashSet<String>(Arrays.asList(
                    SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED,
                    SamlConstants.ATTRIBUTE_NAME_FORMAT_URIREFERENCE,
                    SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC
                    ))
    );

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(final String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    public SamlVersion getSamlVersion() {
        return samlVersion;
    }

    public void setSamlVersion(SamlVersion samlVersion) {
        this.samlVersion = samlVersion;
    }

    public boolean isSoapEncapsulated() {
        return soapEncapsulated;
    }

    public void setSoapEncapsulated(boolean soapEncapsulated) {
        this.soapEncapsulated = soapEncapsulated;
    }

    public boolean isRequireIssuer() {
        return requireIssuer;
    }

    public void setRequireIssuer(boolean requireIssuer) {
        this.requireIssuer = requireIssuer;
    }

    public boolean isRequireSignature() {
        return requireSignature;
    }

    public void setRequireSignature(boolean requireSignature) {
        this.requireSignature = requireSignature;
    }

    public boolean isRequireId() {
        return requireId;
    }

    public void setRequireId(boolean requireId) {
        this.requireId = requireId;
    }

    public boolean isRequireVersion() {
        return requireVersion;
    }

    public void setRequireVersion(boolean requireVersion) {
        this.requireVersion = requireVersion;
    }

    public boolean isRequireIssueInstant() {
        return requireIssueInstant;
    }

    public void setRequireIssueInstant(boolean requireIssueInstant) {
        this.requireIssueInstant = requireIssueInstant;
    }

    public boolean isRequireConsent() {
        return requireConsent;
    }

    public void setRequireConsent(boolean requireConsent) {
        this.requireConsent = requireConsent;
    }

    public boolean isRequireDestination() {
        return requireDestination;
    }

    public void setRequireDestination(boolean requireDestination) {
        this.requireDestination = requireDestination;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(@Nullable String destination) {
        this.destination = destination;
    }

    public boolean isAllowNameId() {
        return allowNameId;
    }

    public void setAllowNameId(boolean allowNameId) {
        this.allowNameId = allowNameId;
    }

    public boolean isAllowEncryptedId() {
        return allowEncryptedId;
    }

    public void setAllowEncryptedId(boolean allowEncryptedId) {
        this.allowEncryptedId = allowEncryptedId;
    }

    public boolean isDecryptEncryptedId() {
        return decryptEncryptedId;
    }

    public void setDecryptEncryptedId(boolean decryptEncryptedId) {
        this.decryptEncryptedId = decryptEncryptedId;
    }

    public boolean isRequireSubjectFormat() {
        return isRequireSubjectFormat;
    }

    public void setRequireSubjectFormat(boolean specificNameFormat) {
        isRequireSubjectFormat = specificNameFormat;
    }

    public String getSubjectFormats() {
        return subjectFormats;
    }

    /**
     * Set the configured subject format URIs.
     *
     * @param subjectFormats a space separated string of supported format URIs
     */
    public void setSubjectFormats(String subjectFormats) {
        this.subjectFormats = subjectFormats;
    }

    public String getCustomSubjectFormats() {
        return customSubjectFormats;
    }

    /**
     * Set the custom Subject Format URIs.
     * <p/>
     * This value will be expanded at runtime.
     *
     * @param customSubjectFormats a space separated string of custom Subject Format URIs, may include variable references.
     */
    public void setCustomSubjectFormats(String customSubjectFormats) {
        this.customSubjectFormats = customSubjectFormats;
    }

    public boolean isRequireAttributes() {
        return requireAttributes;
    }

    public void setRequireAttributes(boolean requireAttributes) {
        this.requireAttributes = requireAttributes;
    }

    public boolean isRequireAttributeNameFormat() {
        return isRequireAttributeNameFormat;
    }

    public void setRequireAttributeNameFormat(boolean requireAttributeNameFormat) {
        isRequireAttributeNameFormat = requireAttributeNameFormat;
    }

    public boolean isVerifyAttributesAreUnique() {
        return verifyAttributesAreUnique;
    }

    public void setVerifyAttributesAreUnique(boolean verifyAttributesAreUnique) {
        this.verifyAttributesAreUnique = verifyAttributesAreUnique;
    }

    public String getAttributeNameFormats() {
        return attributeNameFormats;
    }

    public void setAttributeNameFormats(String attributeNameFormats) {
        this.attributeNameFormats = attributeNameFormats;
    }

    public String getCustomAttributeNameFormats() {
        return customAttributeNameFormats;
    }

    /**
     * Set the custom Attribute NameFormat URIs.
     * <p/>
     * This value will be expanded at runtime.
     *
     * @param customSupportedNameFormats a space separated string of custom Attribute NameFormat URIs, may include variable references.
     */
    public void setCustomAttributeNameFormats(@Nullable String customSupportedNameFormats) {
        this.customAttributeNameFormats = customSupportedNameFormats;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xml"});

        // Set description for GUI
        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(AssertionMetadata.DESCRIPTION, "Process a SAML Attribute Query, context variables are set for extracted values.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlsignature.gif");
        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "auto");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "SAML Attribute Query Request Properties");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.samlpassertion.console.ProcessSamlAttributeQueryRequestPropertiesDialog");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new AssertionNodeNameFactory<ProcessSamlAttributeQueryRequestAssertion>(){
            @Override
            public String getAssertionName( final ProcessSamlAttributeQueryRequestAssertion assertion, boolean decorate) {
                if(!decorate) return baseName;
                final StringBuilder sb = new StringBuilder(baseName);
                return AssertionUtils.decorateName(assertion, sb);
            }
        });

        meta.put(AssertionMetadata.WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
                new Java5EnumTypeMapping(SamlVersion.class, "samlVersion")
        )));

        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    // - PROTECTED

    @Override
    protected VariablesSet doGetVariablesSet() {
        final VariablesSet variablesSet;

        if (variablePrefix == null || samlVersion != SamlVersion.SAML2) {// Update when SAML 1.1 is supported.
            variablesSet = super.doGetVariablesSet();
        } else {
            variablesSet = super.doGetVariablesSet().withVariables(
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ATTRIBUTES, false, false, null, false, DataType.ELEMENT),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_SUBJECT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_SUBJECT_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_PROVIDED_ID, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ID, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_VERSION, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ISSUE_INSTANT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_DESTINATION, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_CONSENT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ISSUER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ISSUER_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ISSUER_SP_NAME_QUALIFIER, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ISSUER_FORMAT, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ISSUER_SP_PROVIDED_ID, false, false, null, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ELEMENTS_DECRYPTED, false, true, variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ELEMENTS_DECRYPTED, false, DataType.ELEMENT),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ENCRYPTION_METHOD_URIS, false, true, variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_ENCRYPTION_METHOD_URIS, false, DataType.STRING),
                new VariableMetadata(variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_RECIPIENT_CERTIFICATES, false, true, variablePrefix+"."+ ProtocolRequestUtilities.SUFFIX_RECIPIENT_CERTIFICATES, false, DataType.CERTIFICATE)
            );
        }

        return variablesSet;
    }

    @Override
    protected MessageTargetableAssertion.VariablesUsed doGetVariablesUsed() {
        final VariablesUsed vars = super.doGetVariablesUsed();

        vars.withExpressions(destination);
        vars.withExpressions(customSubjectFormats);
        vars.withExpressions(customAttributeNameFormats);

        return vars;
    }


    // - PRIVATE

    private static final String META_INITIALIZED = ProcessSamlAttributeQueryRequestAssertion.class.getName() + ".metadataInitialized";
    private static final String baseName = "Process SAML Attribute Query Request";

    private String variablePrefix = DEFAULT_PREFIX;
    private SamlVersion samlVersion = SamlVersion.SAML2;

    private boolean soapEncapsulated;
    private boolean requireIssuer = true;
    private boolean requireId = true;
    private boolean requireVersion = true;
    private boolean requireIssueInstant = true;
    private boolean requireConsent;
    private boolean requireDestination;
    private String destination;

    private boolean allowNameId = true;
    private boolean allowEncryptedId;
    private boolean decryptEncryptedId;
    private boolean isRequireSubjectFormat;
    private String subjectFormats = SamlConstants.NAMEIDENTIFIER_UNSPECIFIED;
    private String customSubjectFormats;

    private boolean requireAttributes;
    private boolean verifyAttributesAreUnique = true;

    private boolean isRequireAttributeNameFormat;
    private String attributeNameFormats = SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;
    private String customAttributeNameFormats;
    private boolean requireSignature;
}
