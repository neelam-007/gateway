package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.xml.xpath.XpathExpression;
import com.l7tech.common.security.xml.KeyReference;
import com.l7tech.policy.assertion.PrivateKeyable;

/**
 * Enforces XML security on the message elements or the entire message.
 * <p/>
 * <code>ElementSecurity</code> list.
 * <p/>
 *
 * @author flascell<br/>
 * @version Aug 27, 2003<br/>
 */
public class ResponseWssIntegrity extends XmlSecurityAssertionBase implements ResponseWssConfig, PrivateKeyable {
    private String keyReference = KeyReference.BST.getName();
    public ResponseWssIntegrity() {
        setXpathExpression(XpathExpression.soapBodyXpathValue());
    }

    public ResponseWssIntegrity(XpathExpression xpath) {
        setXpathExpression(xpath);
    }

    public String getKeyReference() {
        return keyReference;
    }

    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    private boolean usesDefaultKeyStore = true;
    private long nonDefaultKeystoreId;
    private String keyId;

    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    public void setUsesDefaultKeystore(boolean usesDefault) {
        this.usesDefaultKeyStore = usesDefault;
    }

    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    public void setNonDefaultKeystoreId(long nonDefaultId) {
        this.nonDefaultKeystoreId = nonDefaultId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyid) {
        this.keyId = keyid;
    }
}
