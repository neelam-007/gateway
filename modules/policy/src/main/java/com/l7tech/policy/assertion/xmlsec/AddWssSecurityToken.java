package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.policy.assertion.*;
import static com.l7tech.policy.assertion.AssertionMetadata.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;

/**
 * Creates a Security Token element and adds it to the SOAP security header in the target message.
 *
 * @author alex
 */
@RequiresSOAP
public class AddWssSecurityToken extends MessageTargetableAssertion implements WssDecorationConfig, PrivateKeyable {
    public static final SecurityTokenType[] SUPPORTED_TOKEN_TYPES = new SecurityTokenType[] { SecurityTokenType.WSS_USERNAME };

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens;
    private SecurityTokenType tokenType = SecurityTokenType.WSS_USERNAME;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean includePassword;
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();

    public AddWssSecurityToken() {
        super(TargetMessageType.RESPONSE);
    }

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    @Override
    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        if (recipientContext == null) recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
        this.recipientContext = recipientContext;
    }

    @Override
    public String getKeyReference() {
        return keyReference;
    }

    @Override
    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    public boolean isIncludePassword() {
        return includePassword;
    }

    public void setIncludePassword(boolean includePassword) {
        this.includePassword = includePassword;
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
    public String getKeyAlias() {
        return privatekeyableSupport.getKeyAlias();
    }

    @Override
    public void setKeyAlias(String keyAlias) {
        privatekeyableSupport.setKeyAlias(keyAlias);
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return privatekeyableSupport.getNonDefaultKeystoreId();
    }

    @Override
    public void setNonDefaultKeystoreId(long nonDefaultKeystoreId) {
        privatekeyableSupport.setNonDefaultKeystoreId(nonDefaultKeystoreId);
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return privatekeyableSupport.isUsesDefaultKeyStore();
    }

    @Override
    public void setUsesDefaultKeyStore(boolean usesDefaultKeyStore) {
        privatekeyableSupport.setUsesDefaultKeyStore(usesDefaultKeyStore);
    }

    public void copyFrom(AddWssSecurityToken other) {
        this.keyReference = other.keyReference;
        this.tokenType = other.tokenType;
        this.recipientContext = other.recipientContext;
        this.includePassword = other.includePassword;
        this.protectTokens = other.protectTokens;
        this.privatekeyableSupport.copyFrom( other.privatekeyableSupport );
    }

    final static String baseName = "Add Signed Security Token";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AddWssSecurityToken>(){
        @Override
        public String getAssertionName( final AddWssSecurityToken assertion, final boolean decorate) {
            return (decorate) ? AssertionUtils.decorateName(assertion, baseName) : baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();

        meta.put(PALETTE_NODE_NAME, baseName);
        meta.put(DESCRIPTION, "Add a signed security token to the message.");
        meta.put(PALETTE_FOLDERS, new String[]{"xmlSecurity"});
        meta.put(PALETTE_NODE_SORT_PRIORITY, 60000);
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.AddWssSecurityTokenPropertiesAction");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_NAME, "Signed Security Token Properties");
        meta.put(AssertionMetadata.PROPERTIES_ACTION_ICON, "com/l7tech/console/resources/About16.gif");
        meta.put(AssertionMetadata.POLICY_VALIDATOR_CLASSNAME, "com.l7tech.policy.validator.WssDecorationAssertionValidator");

        return meta;
    }
}
