package com.l7tech.external.assertions.websocket.server;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/31/12
 * Time: 9:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketConnectionManagerException extends Exception {
    public WebSocketConnectionManagerException(String message) {
        super(message);
    }

    public WebSocketConnectionManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketConnectionManagerException(Throwable cause) {
        super(cause);
    }

    public WebSocketConnectionManagerException() {
        super();
    }
}
