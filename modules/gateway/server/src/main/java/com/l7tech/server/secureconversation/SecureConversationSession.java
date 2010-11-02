package com.l7tech.server.secureconversation;

import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;

/**
 * A secure conversation session used between a client and the ssg.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 */
public class SecureConversationSession implements SecurityContext {

    public SecureConversationSession( final String secConvNamespaceUsed,
                                      final String identifier,
                                      final byte[] sharedSecret,
                                      final long creation,
                                      final long expiration,
                                      final User usedBy,
                                      final LoginCredentials credentials ) {
        this.secConvNamespaceUsed = secConvNamespaceUsed;
        this.identifier = identifier;
        this.sharedSecret = sharedSecret;
        this.creation = creation;
        this.expiration = expiration;
        this.usedBy = usedBy;
        this.credentials = credentials;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public byte[] getSharedSecret() {
        return sharedSecret;
    }

    public byte[] getClientEntropy() {
        return clientEntropy;
    }

    public void setClientEntropy(byte[] clientEntropy) {
        this.clientEntropy = clientEntropy;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    @Override
    public SecurityToken getSecurityToken() {
        return credentials!=null ? credentials.getSecurityToken() : null;
    }

    public long getExpiration() {
        return expiration;
    }

    public long getCreation() {
        return creation;
    }

    public User getUsedBy() {
        return usedBy;
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
     * Get the namespace used.
     *
     * @return The namespace (may be null)
     */
    public String getSecConvNamespaceUsed() {
        return secConvNamespaceUsed;
    }

    private final String identifier;
    private final byte[] sharedSecret;
    private byte[] clientEntropy;
    private int keySize;
    private final long expiration;
    private final long creation;
    private final User usedBy;
    private final LoginCredentials credentials;
    private final String secConvNamespaceUsed;
}