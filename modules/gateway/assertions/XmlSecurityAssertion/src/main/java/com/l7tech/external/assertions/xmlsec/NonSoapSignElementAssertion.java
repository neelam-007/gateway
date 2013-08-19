package com.l7tech.external.assertions.xmlsec;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.ElementSelectingXpathBasedAssertionValidator;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.util.GoidUpgradeMapper;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

/**
 * Immediately sign one or more Elements in a non-SOAP XML message.
 */
public class NonSoapSignElementAssertion extends NonSoapSecurityAssertionBase implements PrivateKeyable {
    private static final String META_INITIALIZED = NonSoapSignElementAssertion.class.getName() + ".metadataInitialized";
    private static final String baseName = "(Non-SOAP) Sign XML Element";
    private static final SignatureLocation DEFAULT_SIGNATURE_LOCATION = SignatureLocation.LAST_CHILD;

    public static enum SignatureLocation {
        FIRST_CHILD("First child of signed element"),
        LAST_CHILD("Last child of signed element")
        ;

        private final String title;

        private SignatureLocation(String title) {
            this.title = title;
        }


        @Override
        public String toString() {
            return title;
        }
    }

    private final PrivateKeyableSupport privateKeyableSupport = new PrivateKeyableSupport();
    private SignatureLocation signatureLocation = DEFAULT_SIGNATURE_LOCATION;
    private String customIdAttributeQname = null;
    private String detachedSignatureVariableName = null;
    private boolean forceEnvelopedTransform = false;
    private String digestAlgName = null;
    private String refDigestAlgName = null;

    public NonSoapSignElementAssertion() {
        super(TargetMessageType.RESPONSE, true);
    }

    /**
     * @return signature location (first child or last child).
     */
    public SignatureLocation getSignatureLocation() {
        return signatureLocation;
    }

    /**
     * @param signatureLocation location of signature, or null to use default.
     */
    public void setSignatureLocation(SignatureLocation signatureLocation) {
        if (signatureLocation == null)
            signatureLocation = DEFAULT_SIGNATURE_LOCATION;
        this.signatureLocation = signatureLocation;
    }

    public String getCustomIdAttributeQname() {
        return customIdAttributeQname;
    }

    /**
     * @param customIdAttributeQname qname of custom ID attribute, or null to use default behavior.  May have an empty namespace URI and prefix if it is to be a local attribute.
     */
    public void setCustomIdAttributeQname(String customIdAttributeQname) {
        this.customIdAttributeQname = customIdAttributeQname;
    }

    public String getDetachedSignatureVariableName() {
        return detachedSignatureVariableName;
    }

    /**
     * @param detachedSignatureVariableName  name of context variable in which to create a detached signature, or null to create one Signature underneath each signed element.
     */
    public void setDetachedSignatureVariableName(String detachedSignatureVariableName) {
        this.detachedSignatureVariableName = detachedSignatureVariableName;
    }

    public boolean isForceEnvelopedTransform() {
        return forceEnvelopedTransform;
    }

    /**
     * @param forceEnvelopedTransform true to if the Enveloped transform should be included when creating a detached signature.
     *                     Ignored if {@link #getDetachedSignatureVariableName()} is null.
     */
    public void setForceEnvelopedTransform(boolean forceEnvelopedTransform) {
        this.forceEnvelopedTransform = forceEnvelopedTransform;
    }

    /**
     * @return digest algorithm name for SignatureMethod (ie, "SHA-384") or null to use default.
     */
    public String getDigestAlgName() {
        return digestAlgName;
    }

    /**
     * @param digestAlgName digest algorithm name for SignatureMethod (ie, "SHA-512") or null to use default.
     */
    public void setDigestAlgName(String digestAlgName) {
        this.digestAlgName = digestAlgName;
    }

    /**
     * @return digest algorithm name for Reference DigestMethod (ie, "SHA-1"), or null to use same as SignatureMethod.
     */
    public String getRefDigestAlgName() {
        return refDigestAlgName;
    }

    /**
     * @param refDigestAlgName digest algorithm name for Reference DigestMethod (ie, "SHA-256"), or null to use same as SignatureMethod.
     */
    public void setRefDigestAlgName(String refDigestAlgName) {
        this.refDigestAlgName = refDigestAlgName;
    }

    @Override
    public String getDefaultXpathExpressionString() {
        return "//ElementsToSign";
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        return super.doGetVariablesSet().withVariables(
            detachedSignatureVariableName != null ? new VariableMetadata(detachedSignatureVariableName) : null
        );
    }

    @Override
    public XpathExpression createDefaultXpathExpression(boolean soapPolicy, SoapVersion soapVersion) {
        return null;
    }

    @Override
    public String getDisplayName() {
        return baseName;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(META_PROP_VERB, "sign");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately sign one or more elements of the message.  " +
                                                "This does not require a SOAP Envelope and does not accumulate WS-Security decoration requirements.  " +
                                                "Instead, this assertion changes the target message immediately.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1100);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapSignElementAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "(Non-SOAP) XML Element Signature Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, ElementSelectingXpathBasedAssertionValidator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(SignatureLocation.class, "signatureLocation")
        )));

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privateKeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        privateKeyableSupport.setUsesDefaultKeyStore(usesDefault);
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
    public Goid getNonDefaultKeystoreId() {
        return privateKeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(Goid nonDefaultId) {
        privateKeyableSupport.setNonDefaultKeystoreId(nonDefaultId);
    }

    @Deprecated
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        privateKeyableSupport.setNonDefaultKeystoreId(GoidUpgradeMapper.mapOid(EntityType.SSG_KEYSTORE, nonDefaultId));
    }

    @Override
    public String getKeyAlias() {
        return privateKeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyid) {
        privateKeyableSupport.setKeyAlias(keyid);
    }
}
