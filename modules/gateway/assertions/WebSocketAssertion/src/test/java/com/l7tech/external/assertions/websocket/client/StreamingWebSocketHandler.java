package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/27/12
 * Time: 3:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class StreamingWebSocketHandler extends WebSocketHandler {

    private final Set<StreamingWebSocket> webSockets = new CopyOnWriteArraySet<StreamingWebSocket>();

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new StreamingWebSocket();
    }

    private class StreamingWebSocket implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

        private Connection connection;

        public void onOpen(Connection connection) {

            this.connection = connection;
            this.connection.setMaxBinaryMessageSize(1024 * 8 * 1024);
            this.connection.setMaxTextMessageSize(1024 * 1024);
            this.connection.setMaxIdleTime(480000);
            webSockets.add(this);
        }

        public void onMessage(String data) {
            try {
                for (StreamingWebSocket webSocket : webSockets) {
                    if (webSocket != this) {
                        webSocket.connection.sendMessage(data);
                    }
                }
            } catch (IOException e) {
                this.connection.close();
            }

        }

        public void onClose(int closeCode, String message) {
            webSockets.remove(this);
        }

        @Override
        public void onMessage(byte[] bytes, int i, int i1) {
            try {
                for (StreamingWebSocket webSocket : webSockets) {
                    if (webSocket != this) {
                        webSocket.connection.sendMessage(bytes, i, i1);
                    }
                }
            } catch (IOException e) {
                this.connection.close();
            }
        }
    }
}
