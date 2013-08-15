package com.l7tech.server.secureconversation;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Persistent entity for WS-SecureConversation sessions.
 */
@Entity
@Proxy(lazy=false)
@Table(name="wssc_session")
public class StoredSecureConversationSession extends GoidEntityImp {

    //- PUBLIC

    /**
     * Constructor for inbound sessions.
     *
     * @param identifier The unique identifier for the session.
     * @param created The created date for the session.
     * @param expires The expiry date for the session.
     * @param namespace The WS-SecureConversation namespace for the session.
     * @param encryptedKey The encrypted session key.
     * @param providerId The provider identifier for the session user.
     * @param userId The user identifier for the session user.
     * @param userLogin The user login for the session user.
     */
    public StoredSecureConversationSession( final String identifier,
                                            final long created,
                                            final long expires,
                                            final String namespace,
                                            final String encryptedKey,
                                            final Goid providerId,
                                            final String userId,
                                            final String userLogin ) {
        this.sessionKey = generateSessionKey( identifier );
        this.inbound = true;
        this.identifier = identifier;
        this.created = created;
        this.expires = expires;
        this.namespace = namespace;
        this.encryptedKey = encryptedKey;
        this.providerId = providerId;
        this.userId = userId;
        this.userLogin = userLogin;
    }

    /**
     * Constructor for outbound sessions.
     *
     * @param identifier The unique identifier for the session.
     * @param created The created date for the session.
     * @param expires The expiry date for the session.
     * @param namespace The WS-SecureConversation namespace for the session.
     * @param token The token for the session (may be null)
     * @param encryptedKey The encrypted session key.
     * @param providerId The provider identifier for the session user.
     * @param userId The user identifier for the session user.
     * @param userLogin The user login for the session user.
     */
    public StoredSecureConversationSession( final String serviceUrl,
                                            final Goid providerId,
                                            final String userId,
                                            final String userLogin,
                                            final String identifier,
                                            final long created,
                                            final long expires,
                                            final String namespace,
                                            final String token,
                                            final String encryptedKey ) {
        this.sessionKey = generateSessionKey( providerId, userId, serviceUrl );
        this.inbound = false;
        this.identifier = identifier;
        this.serviceUrl = serviceUrl;
        this.created = created;
        this.expires = expires;
        this.namespace = namespace;
        this.token = token;
        this.encryptedKey = encryptedKey;
        this.providerId = providerId;
        this.userId = userId;
        this.userLogin = userLogin;
    }

    @Column(name="session_key_hash", nullable=false, unique=true, updatable=false, length=128)
    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey( final String sessionKey ) {
        this.sessionKey = sessionKey;
    }

    @Column(name="inbound", updatable=false)
    public boolean isInbound() {
        return inbound;
    }

    public void setInbound( final boolean inbound ) {
        this.inbound = inbound;
    }

    @Column(name="identifier", nullable=false, updatable=false, length=4096)
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier( final String identifier ) {
        this.identifier = identifier;
    }

    @Column(name="created", updatable=false)
    public long getCreated() {
        return created;
    }

    public void setCreated( final long created ) {
        this.created = created;
    }

    @Column(name="expires", updatable=false)
    public long getExpires() {
        return expires;
    }

    public void setExpires( final long expires ) {
        this.expires = expires;
    }

    @Column(name="namespace", updatable=false, length=4096)
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace( final String namespace ) {
        this.namespace = namespace;
    }

    @Column(name="token", updatable=false, length=131072)
    public String getToken() {
        return token;
    }

    public void setToken( final String token ) {
        this.token = token;
    }

    @Column(name="service_url", updatable=false, length=4096)
    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl( final String serviceUrl ) {
        this.serviceUrl = serviceUrl;
    }

    @Column(name="encrypted_key", updatable=false)
    public String getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey( final String encryptedKey ) {
        this.encryptedKey = encryptedKey;
    }

    @Column(name="provider_goid", updatable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getProviderId() {
        return providerId;
    }

    public void setProviderId( final Goid providerId ) {
        this.providerId = providerId;
    }

    @Column(name="user_id", updatable=false, length=255)
    public String getUserId() {
        return userId;
    }

    public void setUserId( final String userId ) {
        this.userId = userId;
    }

    @Column(name="user_login", updatable=false, length=255)
    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin( final String userLogin ) {
        this.userLogin = userLogin;
    }

    //- PROTECTED

    protected StoredSecureConversationSession() {
    }

    //- PACKAGE

    /**
     * Generate a session key for an inbound session.
     *
     * @param identifier The inbound sessions unique identifier.
     * @return The session key.
     */
    static String generateSessionKey( final String identifier ) {
        return HexUtils.encodeBase64( HexUtils.getSha512Digest( identifier.getBytes( Charsets.UTF8 ) ), true );
    }

    /**
     * Generate a session key for an inbound session.
     *
     * @param providerId The provider identifier for the session user.
     * @param userId The user identifier for the session user.
     * @param serviceUrl The URL of the service for the session.
     * @return The session key.
     */
    static String generateSessionKey( final Goid providerId, final String userId, final String serviceUrl ) {
        return HexUtils.encodeBase64( HexUtils.getSha512Digest( new byte[][]{
                Goid.toString( providerId ).getBytes(Charsets.UTF8),
                userId.getBytes( Charsets.UTF8 ),
                serviceUrl.getBytes( Charsets.UTF8 )
        } ), true );
    }

    //- PRIVATE

    private String sessionKey;
    private boolean inbound;
    private String identifier;
    private long created;
    private long expires;
    private String namespace;
    private String token;
    private String serviceUrl;
    private String encryptedKey;
    private Goid providerId;
    private String userId;
    private String userLogin;
}
