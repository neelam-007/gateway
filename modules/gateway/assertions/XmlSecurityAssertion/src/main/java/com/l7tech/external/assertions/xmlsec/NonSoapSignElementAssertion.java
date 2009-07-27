package com.l7tech.external.assertions.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.util.Functions;

import java.util.logging.Logger;

/**
 * Immediately sign one or more Elements in a non-SOAP XML message.
 */
public class NonSoapSignElementAssertion extends NonSoapSecurityAssertionBase implements PrivateKeyable {
    private static final String META_INITIALIZED = NonSoapSignElementAssertion.class.getName() + ".metadataInitialized";

    private final PrivateKeyableSupport privateKeyableSupport = new PrivateKeyableSupport();

    public NonSoapSignElementAssertion() {
        super(TargetMessageType.RESPONSE);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        if (Boolean.TRUE.equals(meta.get(META_INITIALIZED)))
            return meta;

        meta.put(AssertionMetadata.SHORT_NAME, "Immediate Sign (Non-SOAP) XML Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Immediately sign one or more elements of the message.  " +
                                                "This does not require a SOAP Envelope and does not accumulate WS-Security decoration requirements.  " +
                                                "Instead, this assertion changes the target message immediately.");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(AssertionMetadata.PALETTE_NODE_SORT_PRIORITY, -1100);
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.external.assertions.xmlsec.console.NonSoapSignElementAssertionPropertiesDialog");

        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, NonSoapSignElementAssertion>() {
            @Override
            public String call( final NonSoapSignElementAssertion ass ) {
                StringBuilder name = new StringBuilder("Immediately Sign (Non-SOAP) XML Element ");
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
