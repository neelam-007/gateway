package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.util.Functions;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
@ProcessesResponse
public class ResponseWssIntegrity extends XmlSecurityAssertionBase implements ResponseWssConfig, PrivateKeyable {

    //- PUBLIC

    public ResponseWssIntegrity() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public ResponseWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }

    @Override
    public String getKeyReference() {
        return keyReference;
    }

    @Override
    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    @Override
    public String getKeyAlias() {
        return keyId;
    }

    @Override
    public void setKeyAlias(String keyid) {
        this.keyId = keyid;
    }

    @Override
    public boolean isProtectTokens() {
        return protectTokens;
    }

    @Override
    public void setProtectTokens(boolean protectTokens) {
        this.protectTokens = protectTokens;
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.SHORT_NAME, "Sign Response Element");
        meta.put(AssertionMetadata.DESCRIPTION, "Server will sign an element of the SOAP response");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(AssertionMetadata.POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, ResponseWssIntegrity>() {
            @Override
            public String call( final ResponseWssIntegrity responseWssIntegrity ) {
                StringBuilder name = new StringBuilder("Sign response element ");
                if (responseWssIntegrity.getXpathExpression() == null) {
                    name .append("[XPath expression not set]");
                } else {
                    name.append(responseWssIntegrity.getXpathExpression().getExpression());
                }
                name.append(SecurityHeaderAddressableSupport.getActorSuffix(responseWssIntegrity));
                return name.toString();
            }
        });

        return meta;
    }

    //- PRIVATE

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens = false;
    private boolean usesDefaultKeyStore = true;
    private long nonDefaultKeystoreId;
    private String keyId;
    
}
