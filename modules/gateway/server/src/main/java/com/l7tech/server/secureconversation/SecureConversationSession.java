package com.l7tech.server.secureconversation;

import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * A secure conversation session used between a client and the ssg.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationSession implements SecurityContext {
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public byte[] getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public long getCreation() {
        return creation;
    }

    public void setCreation(long creation) {
        this.creation = creation;
    }

    public User getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(User usedBy) {
        this.usedBy = usedBy;
    }

    /**
     * The <code>LoginCredentials</code> are the credentials that the <code>User</code>
     * {@link com.l7tech.server.secureconversation.SecureConversationSession#getUsedBy()}
     * authenticated with.
     *
     * @return the <code>LoginCredentials</code> that thee
     */
    public LoginCredentials getCredentials() {
        return credentials;
    }

    /**
     * Set the credentials that the <code>User</code> authenticated with.
     *
     * @param credentials
     */
    public void setCredentials(LoginCredentials credentials) {
        this.credentials = credentials;
    }

    public String getSecConvNamespaceUsed() {
        return secConvNamespaceUsed;
    }

    public void setSecConvNamespaceUsed(String secConvNamespaceUsed) {
        this.secConvNamespaceUsed = secConvNamespaceUsed;
    }

    private String identifier;
    private byte[] sharedSecret;
    private long expiration;
    private long creation;
    private User usedBy;
    private LoginCredentials credentials;
    private String secConvNamespaceUsed;
}
