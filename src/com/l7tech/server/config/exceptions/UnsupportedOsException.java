package com.l7tech.server.config.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 1:58:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class UnsupportedOsException extends RuntimeException {
    public UnsupportedOsException() {
        super();
    }

    public UnsupportedOsException(String message) {
        super(message);
    }

    public UnsupportedOsException(String message, Throwable cause) {
        super(message, cause);
    }
}
