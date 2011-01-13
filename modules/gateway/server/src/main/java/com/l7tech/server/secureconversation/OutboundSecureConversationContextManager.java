package com.l7tech.server.secureconversation;

import com.l7tech.identity.User;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.util.Config;

import java.util.Set;

/**
 * The outbound session map uses the concatenation of User ID and Service URL (rather than Session Identifier) as a lookup key.
 *
 * @author ghuang
 */
public class OutboundSecureConversationContextManager extends SecureConversationContextManager {

    public OutboundSecureConversationContextManager(Config config) {
        super(config, false);
    }

    /**
     * Lookup an outbound secure conversation session by a session identifier.  Since the cache is mapped by user id and server url, to lookup  a
     * session by a session identifier, go thru the list of sessions and find the one, whose session identifier is the same as the given session identifier. 
     *
     * @param sessionIdentifier: the identifier of a session to be retrieved
     * @return an outbound secure conversation session object, which matches the given session identifier.
     */
    @Override
    public SecureConversationSession getSession(String sessionIdentifier) {
        if (sessionIdentifier == null || sessionIdentifier.trim().isEmpty()) return null;

        // Cleanup if necessary
        checkExpiredSessions();

        // Get session
        SecureConversationSession output = null;
        String identifier = null; // This is not a session identifier.  It is a lookup identifier and obtained by matching the given session identifier.
        synchronized (sessions) {
            Set idSet = sessions.keySet();
            for (Object id: idSet) {
                SecureConversationSession session = (SecureConversationSession) sessions.get(id);
                if (sessionIdentifier.equals(session.getIdentifier())) {
                    output = session;
                    identifier = (String) id;
                    break;
                }
            }
        }

        // Check if expired
        if ( output != null && output.getExpiration() <= System.currentTimeMillis() ) {
            output = null;

            synchronized (sessions) {
                sessions.remove(identifier);
            }
        }

        return output;
    }

    /**
     * Lookup an outbound secure conversation session by user id and service url.
     *
     * @param userId: the identifier of an authenticated user
     * @param serviceUrl: the URL of the service with which a client established a secure conversation.
     * @return an outbound secure conversation session object, which matches the given userId and serviceUrl.
     */
    public SecureConversationSession getSession(String userId, String serviceUrl) throws SessionLookupException {
        if (userId == null || userId.trim().isEmpty()) {
            throw new SessionLookupException("The user identifier used for session lookup is not specified.");
        } else if (serviceUrl == null || serviceUrl.trim().isEmpty()) {
            throw new SessionLookupException("The service URL for session lookup is not specified.");
        }

        // Cleanup if necessary
        checkExpiredSessions();

        // Get session
        SecureConversationSession output;
        String identifier = userId + serviceUrl;
        synchronized( sessions ) {
            output = (SecureConversationSession) sessions.get(identifier);
        }

        // Check if expired
        if ( output != null && output.getExpiration() <= System.currentTimeMillis() ) {
            output = null;

            synchronized( sessions ) {
                sessions.remove(identifier);
            }
        }
        return output;
    }

    /**
     * Cancel an outbound secure conversation session, whose session identifier is the same as the given id, sessionIdentifier.
     *
     * @param sessionIdentifier: the session identifier, with which an outbound session associated will be removed from the cache.
     * @return true if the cancellation is successful.
     */
    @Override
    public boolean cancelSession(String sessionIdentifier) {
        // Get the lookup identifier, which is <user id + service url> and not same as a session identifier.
        String identifier = null; // The identifier is obtained by matching the given session identifier.
        synchronized (sessions) {
            Set idSet = sessions.keySet();
            for (Object id: idSet) {
                SecureConversationSession session = (SecureConversationSession) sessions.get(id);
                if (sessionIdentifier.equals(session.getIdentifier())) {
                    identifier = (String) id;
                    break;
                }
            }

            return identifier != null && sessions.remove(identifier) != null;
        }
    }

    /**
     * Cancel a secure conversation session matched by User ID and Service URL
     *
     * @param userId: the user identifier
     * @param serviceUrl: the URL of the service with which  a client established a secure conversation
     * @return true if the cancellation is successful.  Otherwise, return false if userId or serviceUrl is not specified.
     */
    public boolean cancelSession(String userId, String serviceUrl) {
        if (userId == null || userId.trim().isEmpty() || serviceUrl == null || serviceUrl.trim().isEmpty()) return false;

        // Get the lookup identifier
        String identifier = userId + serviceUrl;
        synchronized (sessions) {
            return sessions.remove(identifier) != null;
        }
    }

    /**
     * Creates a new session and saves it.
     *
     * @param sessionOwner The user for the session (required)
     * @param serviceUrl The URL of the service that creates a SCT (must not be null)
     * @param credentials The credentials used to authenticate
     * @param namespace The WS-SecureConversation namespace in use (may be null)
     * @param sessionDuration: its unit is milliseconds.  It must be greater than 0.
     * @param requestClientEntropy The request client entropy (may be null)
     *  @param requestServerEntropy The request server entropy (may be null)
     * @return the newly created session
     */
    public SecureConversationSession createContextForUser(final User sessionOwner,
                                                          final String serviceUrl,
                                                          final LoginCredentials credentials,
                                                          final String namespace,
                                                          final String sessionIdentifier,
                                                          final long sessionDuration,
                                                          final byte[] requestClientEntropy,
                                                          final byte[] requestServerEntropy,
                                                          final byte[] requestSharedSecret) throws SessionCreationException {
        final byte[] sharedSecret;
        final byte[] clientEntropy;
        final byte[] serverEntropy;

        if (requestSharedSecret != null) {
            if (requestSharedSecret.length >= MIN_SHARED_SECRET_BYTES && requestSharedSecret.length <= MAX_SHARED_SECRET_BYTES) {
                sharedSecret = requestSharedSecret;
                clientEntropy = null;
                serverEntropy = null;
            } else {
                throw new SessionCreationException("Unable to create a session: the shared secret length is not in the valid range from " +
                    MIN_SHARED_SECRET_BYTES + " bytes to " + MAX_SHARED_SECRET_BYTES + "bytes.");
            }
        } else if (requestClientEntropy != null) {
            if (requestClientEntropy.length >= MIN_CLIENT_ENTROPY_BYTES && requestClientEntropy.length <= MAX_CLIENT_ENTROPY_BYTES) {
                if (requestServerEntropy != null) {
                    if (requestServerEntropy.length >= MIN_SERVER_ENTROPY_BYTES && requestServerEntropy.length <= MAX_SERVER_ENTROPY_BYTES) {
                        sharedSecret = null;
                        clientEntropy = requestClientEntropy;
                        serverEntropy = requestServerEntropy;
                    } else {
                        throw new SessionCreationException("Unable to create a session: the server entropy length is not in the valid range from " +
                            MIN_SERVER_ENTROPY_BYTES + " bytes to " + MAX_SERVER_ENTROPY_BYTES + "bytes.");
                    }
                } else {
                    sharedSecret = requestClientEntropy;
                    clientEntropy = null;
                    serverEntropy = null;
                }
            } else {
                throw new SessionCreationException("Unable to create a session: the client entropy length is not in the valid range from " +
                    MIN_CLIENT_ENTROPY_BYTES + " bytes to " + MAX_CLIENT_ENTROPY_BYTES + "bytes.");
            }
        } else {
            throw new SessionCreationException("Unable to create a session: there are no shared secret and client entropy to create a session key");
        }

        final long time = System.currentTimeMillis();
        final SecureConversationSession session = new SecureConversationSession(
            namespace,
            sessionIdentifier,
            clientEntropy,
            serverEntropy,
            sharedSecret,
            time,
            time + sessionDuration,
            sessionOwner,
            credentials
        );

        String identifier = sessionOwner.getId() + serviceUrl; // This identifier is not a session identifier.
        saveSession(identifier, session);

        return session;
    }
}