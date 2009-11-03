package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.validator.XpathBasedAssertionValidator;

/**
 * Immediately sign one or more Elements in a non-SOAP XML message.
 */
public class NonSoapSignElementAssertion extends NonSoapSecurityAssertionBase implements PrivateKeyable {
    private static final String META_INITIALIZED = NonSoapSignElementAssertion.class.getName() + ".metadataInitialized";

    private final PrivateKeyableSupport privateKeyableSupport = new PrivateKeyableSupport();

    public NonSoapSignElementAssertion() {
        super(TargetMessageType.RESPONSE);
    }

    private final static String baseName = "Immediate Sign (Non-SOAP) XML Element";

    @Override
    public String getDefaultXpathExpressionString() {
        return "//ElementsToSign";
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

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, XpathBasedAssertionValidator.class.getName());
        meta.put(AssertionMetadata.FEATURE_SET_NAME, "(fromClass)");
        
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
    public long getNonDefaultKeystoreId() {
        return privateKeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        privateKeyableSupport.setNonDefaultKeystoreId(nonDefaultId);
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
