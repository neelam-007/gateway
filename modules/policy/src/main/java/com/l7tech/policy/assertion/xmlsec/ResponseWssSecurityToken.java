package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.ProcessesResponse;

/**
 * Creates a Security Token element and adds it to the SOAP security header in the response.
 *
 * @author alex
 */
@ProcessesResponse
@RequiresSOAP
public class ResponseWssSecurityToken extends Assertion implements ResponseWssConfig {
    public static final SecurityTokenType[] SUPPORTED_TOKEN_TYPES = new SecurityTokenType[] { SecurityTokenType.WSS_USERNAME };

    private String keyReference = KeyReference.BST.getName();
    private SecurityTokenType tokenType = SecurityTokenType.WSS_USERNAME;
    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();
    private boolean includePassword;

    public SecurityTokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(SecurityTokenType tokenType) {
        this.tokenType = tokenType;
    }

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public String getKeyReference() {
        return keyReference;
    }

    public void setKeyReference(String keyReference) {
        this.keyReference = keyReference;
    }

    public boolean isIncludePassword() {
        return includePassword;
    }

    public void setIncludePassword(boolean includePassword) {
        this.includePassword = includePassword;
    }

    public void copyFrom(ResponseWssSecurityToken other) {
        this.keyReference = other.keyReference;
        this.tokenType = other.tokenType;
        this.recipientContext = other.recipientContext;
        this.includePassword = other.includePassword;
    }
}
