package com.l7tech.server.secureconversation;

/**
 * @auther: ghuang
 */
public class SessionExpiredException extends Exception {
    public SessionExpiredException(String message) {
        super(message);
    }
}
