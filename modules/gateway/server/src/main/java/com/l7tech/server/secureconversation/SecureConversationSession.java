package com.l7tech.server.secureconversation;

import com.l7tech.security.token.SessionSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.SecurityContext;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.identity.User;
import org.w3c.dom.Element;

/**
 * A secure conversation session used between a client and the ssg.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 */
public class SecureConversationSession implements SecurityContext, DecorationRequirements.SecureConversationSession {

    public SecureConversationSession( final String secConvNamespaceUsed,
                                      final String identifier,
                                      final byte[] sharedSecret,
                                      final long creation,
                                      final long expiration,
                                      final User usedBy ) {
        this( secConvNamespaceUsed, identifier, null, null, sharedSecret, creation, expiration, usedBy, null );
    }

    public SecureConversationSession( final String secConvNamespaceUsed,
                                      final String identifier,
                                      final byte[] clientEntropy,
                                      final byte[] serverEntropy,
                                      final byte[] sharedSecret,
                                      final long creation,
                                      final long expiration,
                                      final User usedBy,
                                      final Element token ) {
        this.secConvNamespaceUsed = secConvNamespaceUsed;
        this.identifier = identifier;
        this.clientEntropy = clientEntropy;
        this.serverEntropy = serverEntropy;
        this.sharedSecret = sharedSecret;
        this.creation = creation;
        this.expiration = expiration;
        this.usedBy = usedBy;
        this.token = token;
        this.identitySecurityToken = new SessionSecurityToken( SecurityTokenType.WSSC_CONTEXT, usedBy.getProviderId(), usedBy.getId(), usedBy.getLogin() );
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public byte[] getSharedSecret() {
        return sharedSecret;
    }

    public boolean hasEntropy() {
        return clientEntropy != null && serverEntropy != null;
    }

    public byte[] getClientEntropy() {
        return clientEntropy;
    }

    public byte[] getServerEntropy() {
        return serverEntropy;
    }

    /**
     * Get the key size in bits.
     *
     * @return The size of the session key.
     */
    public int getKeySize() {
        return sharedSecret.length * 8;
    }

    @Override
    public SecurityToken getSecurityToken() {
        return null;
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
     * Get the namespace used.
     *
     * @return The namespace (may be null)
     */
    public String getSecConvNamespaceUsed() {
        return secConvNamespaceUsed;
    }

    @Override
    public String getId() {
        return getIdentifier();
    }

    @Override
    public byte[] getSecretKey() {
        return getSharedSecret();
    }

    @Override
    public String getSCNamespace() {
        return getSecConvNamespaceUsed();
    }

    @Override
    public Element getElement() {
        return token;
    }

    public SessionSecurityToken getCredentialSecurityToken() {
        return identitySecurityToken;
    }

    private final String identifier;
    private final byte[] sharedSecret;
    private final byte[] clientEntropy;
    private final byte[] serverEntropy;
    private final long expiration;
    private final long creation;
    private final User usedBy;
    private final String secConvNamespaceUsed;
    private final Element token;
    private final SessionSecurityToken identitySecurityToken;
}