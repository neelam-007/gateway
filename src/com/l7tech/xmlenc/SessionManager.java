package com.l7tech.xmlenc;

import com.l7tech.logging.LogManager;

import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;

/**
 * User: flascell
 * Date: Aug 25, 2003
 * Time: 4:52:28 PM
 *
 * holds sessions for xml encryption usage
 */
public class SessionManager {

    public static final long SESSION_LIFESPAN = 60*60*1000;
    public static final long MAX_SESSION_USES = 5000;

    public static SessionManager getInstance() { return SingletonHolder.singleton; }

    /**
     * looks up the session store for a session with the id mentioned
     * @param id session id
     * @return the session object (never null)
     * @throws SessionNotFoundException if session is invalid or does not exist
     */
    public synchronized Session getSession(long id) throws SessionNotFoundException {
        Session res =  (Session)sessions.get(new Long(id));
        if (res == null) throw new SessionNotFoundException();

        try {
            hitSession(res);
        } catch (SessionInvalidException e) {
            sessions.remove(new Long (id));
            throw new SessionNotFoundException(null, e);
        }
        return res;
    }

    public synchronized Session createNewSession() {
        Session newsession = new Session();

        newsession.setId(genUniqueId());

        byte[] randbytes = new byte[32];

        getRand().nextBytes(randbytes);
        newsession.setKeyIn(randbytes);
        randbytes = new byte[32];
        getRand().nextBytes(randbytes);
        newsession.setKeyOut(randbytes);

        sessions.put(new Long(newsession.getId()), newsession);
        return newsession;
    }

    /**
     * checks validity and increment the hit
     * @param arg
     * @throws SessionInvalidException
     */
    protected void hitSession(Session arg) throws SessionInvalidException {
        // check validity
        if ((System.currentTimeMillis() - arg.getCreationTimestamp()) > SESSION_LIFESPAN) {
            logger.warning("session time out!");
            throw new SessionInvalidException();
        }

        // increment
        if (arg.incrementRequestsUsed() > MAX_SESSION_USES) {
            logger.warning("session used out!");
            throw new SessionInvalidException();
        }
    }

    protected SecureRandom getRand() {
        if (rand == null) {
            try {
                rand = SecureRandom.getInstance("SHA1PRNG");
            } catch (NoSuchAlgorithmException e) {
                // wont happen
                logger.log(Level.SEVERE, "can't happen", e);
                throw new RuntimeException(e);
            }
        }
        return rand;
    }

    private synchronized long genUniqueId() {
        for (;;) {
            long maybeId = getRand().nextLong();
            if (sessions.get(new Long(maybeId)) == null)
                return maybeId;
        }
    }

    protected SessionManager() {
        logger = LogManager.getInstance().getSystemLogger();
    }

    private static class SingletonHolder {
        private static SessionManager singleton = new SessionManager();
    }

    private HashMap sessions = new HashMap();
    private Logger logger = null;
    private SecureRandom rand = null;
}
