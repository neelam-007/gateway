package com.l7tech.server.secureconversation;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server-side manager that manages the SecureConversation sessions.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationContextManager implements WssProcessor.SecurityContextFinder {

    public static SecureConversationContextManager getInstance() {
        return SingletonHolder.singleton;
    }

    /**
     * Retrieve a session previously recorded.
     */
    public synchronized SecureConversationSession getSession(String identifier) {
        return (SecureConversationSession)sessions.get(identifier);
    }

    /**
     * For use by the token service. Records and remembers a session for the duration specified.
     */
    public synchronized void saveSession(SecureConversationSession newSession) throws DuplicateSessionException {
        // Two sessions with same id is not allowed. ever (even if one is expired)
        SecureConversationSession alreadyExistingOne = getSession(newSession.getIdentifier());
        if (alreadyExistingOne != null) {
            throw new DuplicateSessionException("Session already exists with id " + newSession.getIdentifier());
        }
        sessions.put(newSession.getIdentifier(), newSession);
    }

    /**
     * For use by the WssProcessor on the ssg.
     */
    public WssProcessor.SecurityContext getSecurityContext(String securityContextIdentifier) {
        return getSession(securityContextIdentifier);
    }

    public void loadFakeSession() {
        SecureConversationSession fakesession = new SecureConversationSession();
        fakesession.setCreation(System.currentTimeMillis());
        fakesession.setExpiration(System.currentTimeMillis() + (1000*60*60));
        fakesession.setIdentifier("http://www.l7tech.com/uuid/sessionid/123");
        fakesession.setSharedSecret(new SecretKey(){
            public byte[] getEncoded() {
                return new byte[] {5,2,4,5,
                                   8,7,9,6,
                                   32,4,1,55,
                                   8,7,77,7};
            }
            public String getAlgorithm() {
                return "blah";
            }
            public String getFormat() {
                return "blah";
            }
        });
        try {
            IdentityProvider internalProvider = IdentityProviderFactory.getProvider(-2);
            User user = internalProvider.getUserManager().findByLogin("mike");
            fakesession.setUsedBy(user);
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Cannot set fake session", e);
        }
        try {
            saveSession(fakesession);
        } catch (DuplicateSessionException e) {
            logger.log(Level.SEVERE, "could not save session", e);
        }
        logger.fine("\n\n\n\nfake session loaded\n\n\n\n");
    }

    private SecureConversationContextManager() {
        // maybe in the future we use some distributed cache?
    }

    /**
     * keys: identifier (string)
     * values: SecureConversationSession objects
     */
    private HashMap sessions = new HashMap();

    private static class SingletonHolder {
        private static SecureConversationContextManager singleton = new SecureConversationContextManager();
    }

    private final Logger logger = Logger.getLogger(SecureConversationContextManager.class.getName());
}
