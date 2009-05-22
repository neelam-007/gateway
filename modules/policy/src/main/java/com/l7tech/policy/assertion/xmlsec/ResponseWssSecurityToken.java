package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.PrivateKeyableSupport;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_NAME;
import static com.l7tech.policy.assertion.AssertionMetadata.DESCRIPTION;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_NODE_ICON;
import static com.l7tech.policy.assertion.AssertionMetadata.PALETTE_FOLDERS;
import static com.l7tech.policy.assertion.AssertionMetadata.POLICY_NODE_NAME_FACTORY;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;
import com.l7tech.util.Functions;

/**
 * Creates a Security Token element and adds it to the SOAP security header in the response.
 *
 * @author alex
 */
@ProcessesResponse
@RequiresSOAP
public class ResponseWssSecurityToken extends Assertion implements ResponseWssConfig, PrivateKeyable {
    public static final SecurityTokenType[] SUPPORTED_TOKEN_TYPES = new SecurityTokenType[] { SecurityTokenType.WSS_USERNAME };

    private String keyReference = KeyReference.BST.getName();
    private boolean protectTokens;
    private SecurityTokenType tokenType = SecurityTokenType.WSS_USERNAME;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean includePassword;
    private PrivateKeyableSupport privatekeyableSupport = new PrivateKeyableSupport();

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

    public void copyFrom(ResponseWssSecurityToken other) {
        this.keyReference = other.keyReference;
        this.tokenType = other.tokenType;
        this.recipientContext = other.recipientContext;
        this.includePassword = other.includePassword;
        this.protectTokens = other.protectTokens;
        this.privatekeyableSupport.copyFrom( other.privatekeyableSupport );
    }

   @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = super.defaultMeta();
        meta.put(PALETTE_NODE_NAME, "Add Signed Security Token to Response");
        meta.put(DESCRIPTION, "Include signed security token in response");
        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/xmlencryption.gif");
        meta.put(PALETTE_FOLDERS, new String[] { "xmlSecurity" });
        meta.put(POLICY_NODE_NAME_FACTORY, new Functions.Unary<String, ResponseWssSecurityToken>() {
            @Override
            public String call( final ResponseWssSecurityToken assertion ) {
                return "Add Signed " + assertion.getTokenType().getName() + " to Response";
            }
        });
        meta.put(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.ResponseWssSecurityTokenPropertiesAction");

        return meta;
    }
}
