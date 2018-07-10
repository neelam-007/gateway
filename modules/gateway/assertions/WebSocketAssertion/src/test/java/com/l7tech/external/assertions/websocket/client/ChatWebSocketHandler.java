package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatWebSocketHandler extends WebSocketHandler {

    public Set<Session> getWebSockets() {
        return webSockets;
    }

    //private final Set<ChatWebSocket> webSockets = new CopyOnWriteArraySet<ChatWebSocket>();
    private final Set<Session> webSockets = new CopyOnWriteArraySet<Session>();

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {

        ChatWebSocketCreator chatWebSocketCreator = new ChatWebSocketCreator(getWebSockets());
        webSocketServletFactory.setCreator(chatWebSocketCreator);

        // enable websocket compression.
        webSocketServletFactory.getExtensionFactory().register("permessage-deflate",PerMessageDeflateExtension.class);
    }


}
