package com.l7tech.server.admin;

import org.apache.commons.collections.LRUMap;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Background;
import com.l7tech.identity.ValidationException;

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
        private final Object sessionInfo;
        private long lastUsed;

        public SessionHolder(Principal principal, String cookie, Object sessionInfo) {
            this.principal = principal;
            this.cookie = cookie;
            this.lastUsed = System.currentTimeMillis();
            this.sessionInfo = sessionInfo;
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

        /**
         * Get the session info, which contains port number, etc.
         * @return an object of the class {@link com.l7tech.server.admin.ManagerAppletFilter.AdditionalSessionInfo}
         */
        public Object getSessionInfo() {
            return sessionInfo;
        }
    }

    public SessionValidator getSessionValidator() {
        return sessionValidator;
    }

    public void setSessionValidator(SessionValidator sessionValidator) {
        this.sessionValidator = sessionValidator;
    }

    private SessionValidator sessionValidator;
    
    // TODO expire old sessions rather than wait for them to fall out of the LRU map.
    @SuppressWarnings({"deprecation"})
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
     * @param sessionInfo the additional session info such as port number, etc.
     * @return a cookie string that can be used with {@link #getPrincipalsAndResumeSession} later to recover the username.  Never null or empty.
     *         Always contains at least 16 bytes of entropy.
     */
    public synchronized String createSession(Principal authenticatedUser, Object sessionInfo) {
        if (authenticatedUser == null) throw new NullPointerException();

        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String cookie = HexUtils.encodeBase64(bytes, true);

        sessionMap.put(cookie, new SessionHolder(authenticatedUser, cookie, sessionInfo));
        return cookie;
    }

    /**
     * Retrieve the additional session info for a previously-authenticated user.
     * @param session the session ID that was originally returned from {@link #createSession}.  Must not be null or empty.
     * @return the additional session info.  Return null if not found the given session.
     */
    public Object getSessionInfo(String session) {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder)sessionMap.get(session);
        if (holder == null) return null;
        return holder.getSessionInfo();
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

    public synchronized Set<Principal> getPrincipalsAndResumeSession(String session) throws ValidationException {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder)sessionMap.get(session);
        if (holder == null) return null;
        holder.onUsed();
        Principal p = holder.getPrincipal();
        if(p == null){
            return null;
        }
        Set<Principal> returnSet = new HashSet<Principal>();
        //Add the auth user to the returned set
        returnSet.add(p);
        //this set does not include the user principal
        Set<Principal> principals = sessionValidator.validate(p);
        returnSet.addAll(principals);
        return returnSet;
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

    /**
     * Attempt to destroy a session for a previously-authenticated user.  Takes no action if the
     * specified session does not exist.
     *
     * @param principal the principal that was originally passed to {@link #createSession}.
     */
    @SuppressWarnings({"unchecked"})
    public synchronized void destroySession(Principal principal) {
        boolean destroyed = false;
        for (Iterator<SessionHolder> iter = sessionMap.values().iterator(); iter.hasNext();) {
            SessionHolder holder = iter.next();
            // Object equality, not login name! (one user can have many sessions)
            if (holder.getPrincipal() == principal) {
                iter.remove();
                destroyed = true;
                break;
            }
        }

        if (!destroyed) {
            logger.warning("Admin session not found for principal '"+principal.getName()+"'.");    
        }
    }
}
