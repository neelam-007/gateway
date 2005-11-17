package com.l7tech.server.admin;

import org.apache.commons.collections.LRUMap;
import com.l7tech.common.util.HexUtils;

import java.security.SecureRandom;

public class AdminSessionManager {
    private final LRUMap sessionMap = new LRUMap(1000);
    private final SecureRandom random = new SecureRandom();

    public synchronized String login(String login) {
        if (login == null) throw new NullPointerException();

        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String cookie = HexUtils.encodeBase64(bytes, true);

        sessionMap.put(cookie, login);
        return cookie;
    }

    public synchronized String getLogin(String session) {
        if (session == null) throw new NullPointerException();
        return (String)sessionMap.get(session);
    }

}
