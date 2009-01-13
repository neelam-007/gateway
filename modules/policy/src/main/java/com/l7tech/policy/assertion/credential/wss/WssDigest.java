package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;

/**
 * WSS Digest authentication.
 */
public class WssDigest extends WssCredentialSourceAssertion implements SecurityHeaderAddressable {
    private boolean requireNonce = false;
    private boolean requireTimestamp = false;
    private String requiredPassword = null;

    public XmlSecurityRecipientContext getRecipientContext() {
        return recipientContext;
    }

    public void setRecipientContext(XmlSecurityRecipientContext recipientContext) {
        this.recipientContext = recipientContext;
    }

    public boolean isRequireNonce() {
        return requireNonce;
    }

    public void setRequireNonce(boolean requireNonce) {
        this.requireNonce = requireNonce;
    }

    public boolean isRequireTimestamp() {
        return requireTimestamp;
    }

    public void setRequireTimestamp(boolean requireTimestamp) {
        this.requireTimestamp = requireTimestamp;
    }

    public String getRequiredPassword() {
        return requiredPassword;
    }

    public void setRequiredPassword(String requiredPassword) {
        this.requiredPassword = requiredPassword;
    }

    private XmlSecurityRecipientContext recipientContext = XmlSecurityRecipientContext.getLocalRecipient();

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.DESCRIPTION, "Require UsernameToken with password digest");
        meta.put(AssertionMetadata.SHORT_NAME, "WSS Digest");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });

        return meta;
    }
}
