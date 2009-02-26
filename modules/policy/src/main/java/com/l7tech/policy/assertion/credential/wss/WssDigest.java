package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

/**
 * WSS Digest authentication.
 */
@ProcessesRequest
public class WssDigest extends WssCredentialSourceAssertion {
    private boolean requireNonce = false;
    private boolean requireTimestamp = false;
    private String requiredPassword = null;

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

    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(AssertionMetadata.DESCRIPTION, "Require UsernameToken with password digest");
        meta.put(AssertionMetadata.SHORT_NAME, "WSS Digest");
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[] { "accessControl" });

        return meta;
    }
}
