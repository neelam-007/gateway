package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.validator.ElementSelectingXpathBasedAssertionValidator;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

/**
 * Immediately decrypt one or more elements of the message, which need not use WS-Security or even SOAP. 
 */
public class NonSoapDecryptElementAssertion extends NonSoapSecurityAssertionBase implements SetsVariables, HasVariablePrefix {
    private static final String META_INITIALIZED = NonSoapDecryptElementAssertion.class.getName() + ".metadataInitialized";
    
    public static final String VAR_ELEMENTS_DECRYPTED = "elementsDecrypted";
    public static final String VAR_ENCRYPTION_METHOD_URIS = "encryptionMethodUris";
    public static final String VAR_RECIPIENT_CERTIFICATES = "recipientCertificates";
    public static final String VAR_CONTENT_ONLY = "contentsOnly";

    protected String variablePrefix = "";
    private boolean reportContentsOnly = false;

    public NonSoapDecryptElementAssertion() {
        super(TargetMessageType.REQUEST, true);
        setXpathExpression(createDefaultXpathExpression(false, null));
    }

    @Override
    public String getVariablePrefix() {
        return variablePrefix;
    }

    @Override
    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    /**
     * @return true if elements where only the content (not the open and close tag) were encrypted should be included in the result table.
     *         The contentsOnly multivalued variable is set only if this is true.
     */
    public boolean isReportContentsOnly() {
        return reportContentsOnly;
    }

    /**
     * @param reportContentsOnly true if elements where only the contents were encrypted (not the open and close tag) should be included
     *                           in the output table.  If this is true, the "contentsOnly" column will be included in the output table
     *                           (by setting the "prefix.contentsOnly" multivalued variable where each value is true if the corresponding element
     *                            only had its contents encrypted).
     */
    public void setReportContentsOnly(boolean reportContentsOnly) {
        this.reportContentsOnly = reportContentsOnly;
    }

    @Override
    protected VariablesSet doGetVariablesSet() {
        VariablesSet variablesSet = super.doGetVariablesSet().withVariables(
            new VariableMetadata(prefix(VAR_ELEMENTS_DECRYPTED), false, true, prefix(VAR_ELEMENTS_DECRYPTED), false, DataType.ELEMENT),
            new VariableMetadata(prefix(VAR_ENCRYPTION_METHOD_URIS), false, true, prefix(VAR_ENCRYPTION_METHOD_URIS), false, DataType.STRING),
            new VariableMetadata(prefix(VAR_RECIPIENT_CERTIFICATES), false, true, prefix(VAR_RECIPIENT_CERTIFICATES), false, DataType.CERTIFICATE)
        );

        if (isReportContentsOnly()) {
            variablesSet = variablesSet.withVariables(new VariableMetadata(prefix(VAR_CONTENT_ONLY), false, true, prefix(VAR_CONTENT_ONLY), false, DataType.BOOLEAN));
        }

        return variablesSet;
    }

    /**
     * Prepend the current variable prefix, if any, to the specified variable name.  If the current prefix is
     * null or empty this will return the input variable name unchanged.
     *
     * @param var  the variable name to prefix.  Required.
     * @return the variable name with the current prefix prepended, along with a dot; or the variable name unchanged if the prefix is currently null or empty.
     */
    @Override
    public String prefix(String var) {
        String prefix = getVariablePrefix();
        return prefix == null || prefix.trim().length() < 1 ? var : prefix.trim() + "." + var;
    }
    @Override
    public String[] suffixes(){
        return new String[]{VAR_ELEMENTS_DECRYPTED,VAR_ENCRYPTION_METHOD_URIS,VAR_RECIPIENT_CERTIFICATES};
    }

    private final static String baseName = "(Non-SOAP) Decrypt XML Element";

    @Override
    public String getDefaultXpathExpressionString() {
        return "//xenc:EncryptedData";
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
        meta.put(META_PROP_VERB, "decrypt");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately decrypt one or more elements of the message.  " +
                                                "This does not require a SOAP Envelope and does not examine or produce WS-Security processor results.  " +
                                                "Instead, this assertion changes the target message immediately.  The XPath should match the EncryptedData elements " +
                                                "which are to be decrypted.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1010);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapDecryptElementAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "(Non-SOAP) XML Element Decryption Properties");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, ElementSelectingXpathBasedAssertionValidator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");

        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
