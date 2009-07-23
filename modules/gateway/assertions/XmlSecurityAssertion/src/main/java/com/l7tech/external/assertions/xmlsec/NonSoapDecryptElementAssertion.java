package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import java.util.logging.Logger;

/**
 * Immediately decrypt one or more elements of the message, which need not use WS-Security or even SOAP. 
 */
public class NonSoapDecryptElementAssertion extends NonSoapSecurityAssertionBase implements SetsVariables {
    protected static final Logger logger = Logger.getLogger(NonSoapEncryptElementAssertion.class.getName());

    private static final String META_INITIALIZED = NonSoapDecryptElementAssertion.class.getName() + ".metadataInitialized";
    
    public static final String VAR_ELEMENTS_DECRYPTED = "elementsDecrypted";
    public static final String VAR_ENCRYPTION_METHOD_URIS = "encryptionMethodUris";

    public NonSoapDecryptElementAssertion() {
        super(TargetMessageType.REQUEST);
    }

    @Override
    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
                new VariableMetadata(VAR_ELEMENTS_DECRYPTED, false, true, VAR_ELEMENTS_DECRYPTED, false),
                new VariableMetadata(VAR_ENCRYPTION_METHOD_URIS, false, true, VAR_ENCRYPTION_METHOD_URIS, false, DataType.STRING),
        };
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Immediate Decrypt (Non-SOAP) XML Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately decrypt one or more elements of the message.  " +
                                                "This does not require a SOAP Envelope and does not examine WS-Security processor results.  " +
                                                "Instead, this assertion changes the target message immediately.  The XPath should match the EncryptedData elements " +
                                                "which are to be decrypted.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1010);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapDecryptElementAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, NonSoapDecryptElementAssertion>() {
            @Override
            public String call( final NonSoapDecryptElementAssertion ass ) {
                StringBuilder name = new StringBuilder("Immediately Decrypt (Non-SOAP) XML Element ");
                if (ass.getXpathExpression() == null) {
                    name .append("[XPath expression not set]");
                } else {
                    name.append(ass.getXpathExpression().getExpression());
                }
                return AssertionUtils.decorateName(ass, name);
            }
        });

        meta.put(META_INITIALIZED, Boolean.TRUE);
        return meta;
    }
}
