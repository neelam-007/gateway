package com.l7tech.server;

import com.l7tech.common.security.xml.Session;
import com.l7tech.common.security.xml.SessionInvalidException;
import com.l7tech.common.security.xml.SessionNotFoundException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Holds sessions for xml encryption usage.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 25, 2003
 */
public class SessionManager {

    public static final long SESSION_LIFESPAN = 60*60*1000;
    public static final long MAX_SESSION_USES = 5000;

    public static SessionManager getInstance() { return SingletonHolder.singleton; }

    /**
     * looks up the session store for a session with the id mentioned
     * @param id session id
     * @return the session object (never null)
     * @throws com.l7tech.common.security.xml.SessionNotFoundException if session is invalid or does not exist
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
        newsession.setKeyReq(randbytes);
        randbytes = new byte[32];
        getRand().nextBytes(randbytes);
        newsession.setKeyRes(randbytes);

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

    public SecureRandom getRand() {
        if (rand == null)
            rand = new SecureRandom();
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
    }

    private static class SingletonHolder {
        private static SessionManager singleton = new SessionManager();
    }

    private HashMap sessions = new HashMap();
    private final Logger logger = Logger.getLogger(getClass().getName());
    private SecureRandom rand = null;
}
