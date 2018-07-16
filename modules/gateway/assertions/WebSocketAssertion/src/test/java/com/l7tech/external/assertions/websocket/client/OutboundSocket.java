package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Created by chaja24 on 5/9/2016.
 */

@WebSocket
public class OutboundSocket {
    private Session session;
    private String message;
    private byte[] binaryMsg;
    private boolean messageReceived;
    private URI uri;
    private WebSocketClient client;
    private Map<String, String> headers;

    CountDownLatch latch= new CountDownLatch(1);

    @OnWebSocketConnect
    public void onConnect(Session session) {

        this.session = session;
        latch.countDown();
    }

    @OnWebSocketMessage
    public void onMessage(String data) {
        message = data;
        messageReceived = true;
    }


    @OnWebSocketMessage
    public void onMessage(byte[] bytes, int arg1, int len) {
        setBinaryMsg(bytes, arg1, len);
        messageReceived = true;
    }

    public void send(String message) throws IOException {
        session.getRemote().sendString(message);
        messageReceived = false;
    }

    public void send(byte[] message, int i, int i1) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(message);
        session.getRemote().sendBytesByFuture(b);
        messageReceived = false;
    }

    @OnWebSocketClose
    public void onClose(int closeCode, String message) {
        if (session != null) session.close();
        this.message = null;
        binaryMsg = null;
        messageReceived = true;
        latch.countDown();
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

    public void terminateSession() {
        session.close();
    }

    @OnWebSocketError
    public void onWebSocketError(Throwable cause) {
        latch.countDown();
    }

    public CountDownLatch getLatch() {

        return latch;

    }
}
