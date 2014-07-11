package com.l7tech.external.assertions.websocket.server;

import org.springframework.context.ApplicationEvent;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/8/12
 * Time: 9:45 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketConnectionChangeEvent extends ApplicationEvent {

    private String connectionId;

    private WebSocketConnectionChangeEvent(Object source) {
        super(source);
    }

    public WebSocketConnectionChangeEvent(Object source, String connectionId) {
        this(source);
        this.connectionId = connectionId;
    }

    public String getId() {
        return connectionId;
    }
}
