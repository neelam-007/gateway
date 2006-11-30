package com.l7tech.server.admin;

import org.apache.commons.collections.LRUMap;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.Background;

import java.security.SecureRandom;
import java.security.Principal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class keeps track of admin sessions that have already authenticated.
 * The cookie is used to look up the username that authenticated with it.  Anyone who can steal a cookie can
 * resume an admin session as that user; thus, the cookies must be sent over SSL, never written to disk by
 * either client or server, and not kept longer than necessary.
 */
public class AdminSessionManager {
    protected static final Logger logger = Logger.getLogger(AdminSessionManager.class.getName());

    private static class SessionHolder {
        private final Principal principal;
        private final String cookie;
        private long lastUsed;

        public SessionHolder(Principal principal, String cookie) {
            this.principal = principal;
            this.cookie = cookie;
            this.lastUsed = System.currentTimeMillis();
        }

        public Principal getPrincipal() {
            return principal;
        }

        public String getCookie() {
            return cookie;
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public void onUsed() {
            lastUsed = System.currentTimeMillis();
        }
    }

    // TODO expire old sessions rather than wait for them to fall out of the LRU map.
    private final LRUMap sessionMap = new LRUMap(1000);
    private final SecureRandom random = new SecureRandom();

    private static final long REAP_DELAY = 20 * 60 * 1000; // Check every 20 min for stale sessions
    private static final long REAP_STALE_AGE = 24 * 60 * 60 * 1000; // reap sessions after 24 hours of inactivity

    {
        Background.scheduleRepeated(new TimerTask() {
            public void run() {
                synchronized (AdminSessionManager.this) {
                    Collection values = sessionMap.values();
                    long now = System.currentTimeMillis();
                    for (Iterator i = values.iterator(); i.hasNext();) {
                        SessionHolder holder = (SessionHolder)i.next();
                        long age = now - holder.getLastUsed();
                        if (age > REAP_STALE_AGE) {
                            if (logger.isLoggable(Level.INFO))
                                logger.log(Level.INFO, "Removing stale admin session: " + holder.getPrincipal().getName());
                            i.remove();
                        }
                    }
                }
            }
        }, REAP_DELAY, REAP_DELAY);
    }

    /**
     * Record a successful authentication for the specified login and return a cookie that can be used
     * to resume the session from now on.
     *
     * @param authenticatedUser  the principal that was successfully authenticated.  Must not be null.
     * @return a cookie string that can be used with {@link #resumeSession} later to recover the username.  Never null or empty.
     *         Always contains at least 16 bytes of entropy.
     */
    public synchronized String createSession(Principal authenticatedUser) {
        if (authenticatedUser == null) throw new NullPointerException();

        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String cookie = HexUtils.encodeBase64(bytes, true);

        sessionMap.put(cookie, new SessionHolder(authenticatedUser, cookie));
        return cookie;
    }

    /**
     * Attempt to resume a session for a previously-authenticated user.
     *
     * @param session  the session ID that was originally returned from {@link #createSession}.  Must not be null or empty.
     * @return the login name associated with this session ID, or null if the session doesn't exist or has expired.
     */
    public synchronized Principal resumeSession(String session) {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder)sessionMap.get(session);
        if (holder == null) return null;
        holder.onUsed();
        return holder.getPrincipal();
    }

    /**
     * Attempt to destroy a session for a previously-authenticated user.  Silently takes no action if the
     * specified session does not exist.
     *
     * @param session the session ID that was originally returned from {@link #createSession}.  Must not be null or empty.
     */
    public synchronized void destroySession(String session) {
        sessionMap.remove(session);
    }
}
