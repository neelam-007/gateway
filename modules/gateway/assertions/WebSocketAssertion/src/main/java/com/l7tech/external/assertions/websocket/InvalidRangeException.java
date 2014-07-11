package com.l7tech.external.assertions.websocket;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/4/12
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class InvalidRangeException extends Exception {
    public InvalidRangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRangeException() {
        super();
    }

    public InvalidRangeException(String message) {
        super(message);
    }
}
