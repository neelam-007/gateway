package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.wsp.Java5EnumTypeMapping;
import com.l7tech.policy.wsp.SimpleTypeMappingFinder;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.security.xml.WsSecurityVersion;

import java.util.Arrays;

import static com.l7tech.policy.assertion.AssertionMetadata.WSP_SUBTYPE_FINDER;

/**
 * An assertion that can modify the pending decoration requirements for a message before it is decorated.
 */
@RequiresSOAP(wss=true)
public class WssConfigurationAssertion extends MessageTargetableAssertion implements WssDecorationConfig, PrivateKeyable {
    private WsSecurityVersion wssVersion;
    private String keyReference;
    private boolean useDerivedKeys = false;
    private boolean addTimestamp = true;
    private boolean signTimestamp = true;
    private boolean protectTokens = true;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();
    private String digestAlgorithmName;
    private String encryptionKeyReference;
    private String encryptionAlgorithmUri;
    private String keyWrappingAlgorithmUri;

    public WssConfigurationAssertion() {
        // Point at response by default, like our historical decoration requirements assertions.
        // Per SetsVaraibles contract, changes to decoration requirements count as modification of the target message.
        super(TargetMessageType.RESPONSE, true);
    }

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.SHORT_NAME, "Configure WS-Security Decoration");
        meta.put(AssertionMetadata.DESCRIPTION, "Configure WS-Security decoration requirements");
        meta.put(AssertionMetadata.PROPERTIES_EDITOR_CLASSNAME, "com.l7tech.console.panels.WssConfigurationAssertionPropertiesDialog");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(WSP_SUBTYPE_FINDER, new SimpleTypeMappingFinder(Arrays.<TypeMapping>asList(
            new Java5EnumTypeMapping(WsSecurityVersion.class, "wssVersion")
        )));
        return meta;
    }

    public WsSecurityVersion getWssVersion() {
        return wssVersion;
    }

    public void setWssVersion(WsSecurityVersion wssVersion) {
        this.wssVersion = wssVersion;
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
    public boolean isProtectTokens() {
        return protectTokens;
    }

    @Override
    public void setProtectTokens(boolean protectTokens) {
        this.protectTokens = protectTokens;
    }

    @Override
    public String getDigestAlgorithmName() {
        return digestAlgorithmName;
    }

    @Override
    public void setDigestAlgorithmName(String digestAlgorithmName) {
        this.digestAlgorithmName = digestAlgorithmName;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privatekeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefault) {
        privatekeyableSupport.setUsesDefaultKeyStore(usesDefault);
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultId) {
        privatekeyableSupport.setNonDefaultKeystoreId(nonDefaultId);
    }

    @Override
    public String getKeyAlias() {
        return privatekeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyid) {
        privatekeyableSupport.setKeyAlias(keyid);
    }

    public String getEncryptionKeyReference() {
        return encryptionKeyReference;
    }

    public void setEncryptionKeyReference(String encryptionKeyReference) {
        this.encryptionKeyReference = encryptionKeyReference;
    }

    public String getEncryptionAlgorithmUri() {
        return encryptionAlgorithmUri;
    }

    public void setEncryptionAlgorithmUri(String encryptionAlgorithmUri) {
        this.encryptionAlgorithmUri = encryptionAlgorithmUri;
    }

    public String getKeyWrappingAlgorithmUri() {
        return keyWrappingAlgorithmUri;
    }

    public void setKeyWrappingAlgorithmUri(String keyWrappingAlgorithmUri) {
        this.keyWrappingAlgorithmUri = keyWrappingAlgorithmUri;
    }

    public boolean isUseDerivedKeys() {
        return useDerivedKeys;
    }

    public void setUseDerivedKeys(boolean useDerivedKeys) {
        this.useDerivedKeys = useDerivedKeys;
    }

    public boolean isAddTimestamp() {
        return addTimestamp;
    }

    public void setAddTimestamp(boolean addTimestamp) {
        this.addTimestamp = addTimestamp;
    }

    public boolean isSignTimestamp() {
        return signTimestamp;
    }

    public void setSignTimestamp(boolean signTimestamp) {
        this.signTimestamp = signTimestamp;
    }
}
