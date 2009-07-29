package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.Functions;

/**
 * Immediately encrypt one or more elements of the message, which need not use WS-Security or even SOAP. 
 */
public class NonSoapEncryptElementAssertion extends NonSoapSecurityAssertionBase {
    private static final String META_INITIALIZED = NonSoapEncryptElementAssertion.class.getName() + ".metadataInitialized";

    // EncryptedKey properties

    /**
     * The recipient's certificate, if generating an EncryptedKey.
     */
    private String recipientCertificateBase64 = null;

    // Bulk encryption properties

    /**
     * If true, replace all contents of the element with a single EncryptedData element, but leave the open and close tags unencrypted.
     * If false, replace the entire element with an EncryptedData element.
     */
    private boolean encryptContentsOnly = false;

    /** Bulk encryption algorithm. */
    private String xencAlgorithm = XencUtil.AES_128_CBC;

    public NonSoapEncryptElementAssertion() {
        super(TargetMessageType.RESPONSE);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Immediate Encrypt (Non-SOAP) XML Element");
        meta.put(META_PROP_VERB, "encrypt");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately encrypt one or more elements of the message.  " +
                                                "This does not require a SOAP Envelope and does not accumulate WS-Security decoration requirements.  " +
                                                "Instead, this assertion changes the target message immediately.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1000);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapEncryptElementAssertionPropertiesDialog");
        
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, NonSoapEncryptElementAssertion>() {
            @Override
            public String call( final NonSoapEncryptElementAssertion ass ) {
                StringBuilder name = new StringBuilder("Immediately Encrypt (Non-SOAP) XML Elements ");
                if (ass.getXpathExpression() == null) {
                    name.append("[XPath expression not set]");
                } else {
                    name.append(ass.getXpathExpression().getExpression());
                }
                return AssertionUtils.decorateName(ass, name);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }

    public boolean isEncryptContentsOnly() {
        return encryptContentsOnly;
    }

    public void setEncryptContentsOnly(boolean encryptContentsOnly) {
        this.encryptContentsOnly = encryptContentsOnly;
    }

    public String getXencAlgorithm() {
        return xencAlgorithm;
    }

    public void setXencAlgorithm(String xencAlgorithm) {
        this.xencAlgorithm = xencAlgorithm;
    }

    public String getRecipientCertificateBase64() {
        return recipientCertificateBase64;
    }

    public void setRecipientCertificateBase64(String recipientCertificateBase64) {
        this.recipientCertificateBase64 = recipientCertificateBase64;
    }
}
