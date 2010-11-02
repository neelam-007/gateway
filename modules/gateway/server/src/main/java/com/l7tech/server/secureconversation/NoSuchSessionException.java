package com.l7tech.server.secureconversation;

/**
 * @auther: ghuang
 */
public class NoSuchSessionException extends Exception {
    public NoSuchSessionException(String message) {
        super(message);
    }
}
