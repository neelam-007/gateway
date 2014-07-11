package com.l7tech.external.assertions.websocket.console;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/5/12
 * Time: 10:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class InvalidPortException extends Exception {

    public InvalidPortException() {
        super();
    }

    public InvalidPortException(String message) {
        super(message);
    }

    public InvalidPortException(String message, Throwable cause) {
        super(message, cause);
    }
}
