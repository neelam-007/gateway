package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.xpath.XpathExpression;

/**
 * Immediately encrypt one or more elements of the message, which need not use WS-Security or even SOAP. 
 */
public class NonSoapEncryptElementAssertion extends NonSoapSecurityAssertionBase {
    private static final String META_INITIALIZED = NonSoapEncryptElementAssertion.class.getName() + ".metadataInitialized";

    /**
     * Interface replicated here for backwards compatibility. This assertion only supports method
     * in XmlElementEncryptionConfig which exist here and delegate to it.
     */
    private XmlElementEncryptionConfig config = new XmlElementEncryptionConfig();

    public NonSoapEncryptElementAssertion() {
        super(TargetMessageType.RESPONSE, true);
    }

    private final static String baseName = "(Non-SOAP) Encrypt XML Element";

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    @Override
    public Object clone() {
        final NonSoapEncryptElementAssertion clone = (NonSoapEncryptElementAssertion) super.clone();
        clone.config = (XmlElementEncryptionConfig) config.clone();
        return clone;
    }

    @Override
    public String getDefaultXpathExpressionString() {
        return "//ElementsToEncrypt";
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
    protected VariablesUsed doGetVariablesUsed() {
        return super.doGetVariablesUsed().withVariables(config().getVariablesUsed());
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, baseName);
        meta.put(META_PROP_VERB, "encrypt");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately encrypt one or more elements of the message.  " +
                                                "This does not require a SOAP Envelope and does not accumulate WS-Security decoration requirements.  " +
                                                "Instead, this assertion changes the target message immediately.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapEncryptElementAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "(Non-SOAP) XML Element Encryption Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, NonSoapEncryptElementValidator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(AssertionMetadata.POLICY_ADVICE_CLASSNAME, "com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAdvice");

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public boolean isEncryptContentsOnly() {
        return config.isEncryptContentsOnly();
    }

    public void setEncryptContentsOnly(boolean encryptContentsOnly) {
        config.setEncryptContentsOnly(encryptContentsOnly);
    }

    public String getXencAlgorithm() {
        return config.getXencAlgorithm();
    }

    public void setXencAlgorithm(String xencAlgorithm) {
        config.setXencAlgorithm(xencAlgorithm);
    }

    public String getRecipientCertificateBase64() {
        return config.getRecipientCertificateBase64();
    }

    public void setRecipientCertificateBase64(String recipientCertificateBase64) {
        config.setRecipientCertificateBase64(recipientCertificateBase64);
    }

    public String getRecipientCertContextVariableName() {
        return config.getRecipientCertContextVariableName();
    }

    public void setRecipientCertContextVariableName(String contextVariableName) {
        config.setRecipientCertContextVariableName(contextVariableName);
    }

    public boolean isIncludeEncryptedDataTypeAttribute() {
        return config.isIncludeEncryptedDataTypeAttribute();
    }

    public void setIncludeEncryptedDataTypeAttribute(boolean includeEncryptedDataTypeAttribute) {
        config.setIncludeEncryptedDataTypeAttribute(includeEncryptedDataTypeAttribute);
    }

    public String getEncryptedDataTypeAttribute() {
        return config.getEncryptedDataTypeAttribute();
    }

    public void setEncryptedDataTypeAttribute(String encryptedDataTypeAttribute) {
        config.setEncryptedDataTypeAttribute(encryptedDataTypeAttribute);
    }

    public String getEncryptedKeyRecipientAttribute() {
        return config.getEncryptedKeyRecipientAttribute();
    }

    public void setEncryptedKeyRecipientAttribute(String encryptedKeyRecipientAttribute) {
        config.setEncryptedKeyRecipientAttribute(encryptedKeyRecipientAttribute);
    }

    public boolean isUseOaep() {
        return config().isUseOaep();
    }

    public void setUseOaep(boolean useOaep) {
        config.setUseOaep(useOaep);
    }

    public XmlElementEncryptionConfig config() {
        return config;
    }

    public void config(XmlElementEncryptionConfig config) {
        this.config = config;
    }
}
