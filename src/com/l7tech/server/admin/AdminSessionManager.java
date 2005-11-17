package com.l7tech.server.admin;

import org.apache.commons.collections.LRUMap;
import com.l7tech.common.util.HexUtils;

import java.security.SecureRandom;

/**
 * This class keeps track of admin sessions that have already authenticated.
 * The cookie is used to look up the username that authenticated with it.  Anyone who can steal a cookie can
 * resume an admin session as that user; thus, the cookies must be sent over SSL, never written to disk by
 * either client or server, and not kept longer than necessary.
 */
public class AdminSessionManager {
    // TODO expire old sessions rather than wait for them to fall out of the LRU map.
    private final LRUMap sessionMap = new LRUMap(1000);
    private final SecureRandom random = new SecureRandom();

    /**
     * Record a successful authentication for the specified login and return a cookie that can be used
     * to resume the session from now on.
     *
     * @param login  the login to register.  Must not be null.
     * @return a cookie string that can be used with {@link #getLogin} later to recover the username.  Never null or empty.
     *         Always contains at least 16 bytes of entropy.
     */
    public synchronized String login(String login) {
        if (login == null) throw new NullPointerException();

        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String cookie = HexUtils.encodeBase64(bytes, true);

        sessionMap.put(cookie, login);
        return cookie;
    }

    /**
     * Attempt to resume a session for a previously-authenticated user.
     *
     * @param session  the session ID that was originally returned from {@link #login}.  Must not be null or empty.
     * @return the login name associated with this session ID, or null if the session doesn't exist or has expired.
     */
    public synchronized String getLogin(String session) {
        if (session == null) throw new NullPointerException();
        return (String)sessionMap.get(session);
    }

}
