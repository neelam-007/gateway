package com.l7tech.external.assertions.websocket.server;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 8/24/12
 * Time: 9:38 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketNoSuchClientException extends Exception {

    public WebSocketNoSuchClientException(String message) {
        super(message);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public WebSocketNoSuchClientException(String message, Throwable cause) {
        super(message, cause);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public WebSocketNoSuchClientException(Throwable cause) {
        super(cause);    //To change body of overridden methods use File | Settings | File Templates.
    }
}
