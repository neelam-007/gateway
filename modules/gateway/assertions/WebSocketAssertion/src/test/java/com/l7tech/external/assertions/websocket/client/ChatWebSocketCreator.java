package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import java.util.Set;

/**
 * Created by chaja24 on 5/4/2016.
 */
public class ChatWebSocketCreator implements WebSocketCreator {

    private ChatWebSocket chatWebSocket;

    public ChatWebSocketCreator(Set<Session> websocketSet) {
        this.chatWebSocket = new ChatWebSocket(websocketSet);
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
        return chatWebSocket;
    }

}
