package com.l7tech.external.assertions.websocket.client;

import com.l7tech.external.assertions.websocket.WebSocketConstants;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/20/12
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketClientHandler extends WebSocketHandler {

    private ClientOutboundSocket socket;

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String s) {
        return null;
    }

    public void createConnection(URI uri, boolean ssl, Map<String, String> headers) throws Exception {
        if (ssl) {
            socket = new ClientOutboundSocket(uri, WebSocketClientConnectionManager.getInstance().getOutBoundSslWebSocket("", "", 60000), headers);
        } else {
            socket = new ClientOutboundSocket(uri, WebSocketClientConnectionManager.getInstance().getOutBoundWebSocket("", "", 60000), headers);
        }

    }

    public void sendMessage(String message) throws IOException {
        socket.send(message);
    }

    public void sendMessage(byte[] bytes, int i, int i1) throws IOException {
        socket.send(bytes, i, i1);
    }

    public String getMessage() {
        return socket.getMessage();
    }

    public byte[] getBinMsg() {
        return socket.getBinaryMsg();
    }

    public boolean hasMessage() {
        return socket.hasMessage();
    }

    private class ClientOutboundSocket implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

        private WebSocket.Connection connection;
        private String message;
        private byte[] binaryMsg;
        private boolean messageReceived;


        public ClientOutboundSocket(URI uri, WebSocketClient client, Map<String, String> headers) throws Exception {
            addCustomHeaders(client, headers);
            connection = client.open(uri, this, WebSocketConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS);
        }

        private void addCustomHeaders(WebSocketClient client, Map<String, String> headers) {
            if (headers != null) {
                Set<String> headerKeys = headers.keySet();
                for (String h : headerKeys) {
                    client.addHeader(h, headers.get(h));
                }
            }
        }

        @Override
        public void onOpen(WebSocket.Connection connection) {
            //this.connection = connection;
        }

        @Override
        public void onMessage(String data) {
            message = data;
            messageReceived = true;
        }

        @Override
        public void onMessage(byte[] bytes, int i, int i1) {
            setBinaryMsg(bytes, i, i1);
            messageReceived = true;
        }

        public void send(String message) throws IOException {
            connection.sendMessage(message);
            messageReceived = false;
        }

        public void send(byte[] message, int i, int i1) throws IOException {
            connection.sendMessage(message, i, i1);
            messageReceived = false;
        }

        @Override
        public void onClose(int closeCode, String message) {
            if (connection != null) connection.close();
            this.message = null;
            binaryMsg = null;
            messageReceived = true;
        }

        public String getMessage() {
            return message;
        }

        public byte[] getBinaryMsg() {
            return binaryMsg;
        }

        public boolean hasMessage() {
            return messageReceived;
        }

        private void setBinaryMsg(byte[] b, int i, int i1) {
            binaryMsg = new byte[i1];
            for (int j = 0; j < i1; j++) {
                binaryMsg[j] = b[j + i];
            }
        }
    }
}
