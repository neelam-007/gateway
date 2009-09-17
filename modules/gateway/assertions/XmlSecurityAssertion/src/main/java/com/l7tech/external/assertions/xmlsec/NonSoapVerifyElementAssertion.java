package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

/**
 * Immediately verify one or more signed Elements in a non-SOAP XML message.
 */
public class NonSoapVerifyElementAssertion extends NonSoapSecurityAssertionBase implements SetsVariables {
    private static final String META_INITIALIZED = NonSoapVerifyElementAssertion.class.getName() + ".metadataInitialized";
    public static final String VAR_ELEMENTS_VERIFIED = "elementsVerified";
    public static final String VAR_SIGNATURE_METHOD_URIS = "signatureMethodUris";
    public static final String VAR_DIGEST_METHOD_URIS = "digestMethodUris";
    public static final String VAR_SIGNING_CERTIFICATES = "signingCertificates";
    public static final String VAR_SIGNATURE_VALUES = "signatureValues";
    public static final String VAR_SIGNATURE_ELEMENTS = "signatureElements";

    protected String variablePrefix = "";

    public NonSoapVerifyElementAssertion() {
        super(TargetMessageType.REQUEST);
    }

    public String getVariablePrefix() {
        return variablePrefix;
    }

    public void setVariablePrefix(String variablePrefix) {
        this.variablePrefix = variablePrefix;
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(prefix(VAR_ELEMENTS_VERIFIED), false, true, prefix(VAR_ELEMENTS_VERIFIED), false, DataType.ELEMENT),
                new VariableMetadata(prefix(VAR_SIGNATURE_METHOD_URIS), false, true, prefix(VAR_SIGNATURE_METHOD_URIS), false, DataType.STRING),
                new VariableMetadata(prefix(VAR_DIGEST_METHOD_URIS), false, true, prefix(VAR_DIGEST_METHOD_URIS), false, DataType.STRING),
                new VariableMetadata(prefix(VAR_SIGNING_CERTIFICATES), false, true, prefix(VAR_SIGNING_CERTIFICATES), false, DataType.CERTIFICATE),
                new VariableMetadata(prefix(VAR_SIGNATURE_VALUES), false, true, prefix(VAR_SIGNATURE_VALUES), false, DataType.STRING),
                new VariableMetadata(prefix(VAR_SIGNATURE_ELEMENTS), false, true, prefix(VAR_SIGNATURE_ELEMENTS), false, DataType.ELEMENT),
        };
    }

    /**
     * Prepend the current variable prefix, if any, to the specified variable name.  If the current prefix is
     * null or empty this will return the input variable name unchanged.
     *
     * @param var  the variable name to prefix.  Required.
     * @return the variable name with the current prefix prepended, along with a dot; or the variable name unchanged if the prefix is currently null or empty.
     */
    public String prefix(String var) {
        String prefix = getVariablePrefix();
        return prefix == null || prefix.trim().length() < 1 ? var : prefix.trim() + "." + var;
    }

    private final static String baseName = "Immediate Verify (Non-SOAP) XML Element";

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
        meta.put(META_PROP_VERB, "verify");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately verify one or more signatures of the message.  " +
                                                "This does not require a SOAP Envelope and does not examine or produce WS-Security processor results.  " +
                                                "Instead, this assertion examines the target message immediately.  The XPath should match the Signature elements " +
                                                "which are to be verified.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1110);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapVerifyElementAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
