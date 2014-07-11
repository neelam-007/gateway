package com.l7tech.external.assertions.websocket.server;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/1/12
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketInvalidTypeException extends Exception {
    public WebSocketInvalidTypeException() {
        super();
    }

    public WebSocketInvalidTypeException(String message) {
        super(message);
    }

    public WebSocketInvalidTypeException(String message, Throwable cause) {
        super(message, cause);
    }

}
