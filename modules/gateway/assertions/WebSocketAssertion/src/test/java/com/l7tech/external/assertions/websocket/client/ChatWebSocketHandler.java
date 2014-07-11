package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatWebSocketHandler extends WebSocketHandler {

    private final Set<ChatWebSocket> webSockets = new CopyOnWriteArraySet<ChatWebSocket>();

    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new ChatWebSocket();
    }

    private class ChatWebSocket implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

        private Connection connection;

        public void onOpen(Connection connection) {

            this.connection = connection;
            this.connection.setMaxBinaryMessageSize(1024*1024);
            this.connection.setMaxTextMessageSize(1024*1024);
            this.connection.setMaxIdleTime(480000);
            webSockets.add(this);
        }

        public void onMessage(String data) {
            try {
                for (ChatWebSocket webSocket : webSockets) {
                    webSocket.connection.sendMessage(data);
                }
            } catch (IOException e) {
                this.connection.close();
            }

        }

        public void onClose(int closeCode, String message) {
            webSockets.remove(this);
        }

        @Override
        public void onMessage(byte[] arg0, int arg1, int arg2) {
            try {

                for (ChatWebSocket webSocket : webSockets){
                    webSocket.connection.sendMessage(arg0,arg1,arg2);
                }
            } catch (IOException e) {
                this.connection.close();
            }

        }
    }
}
