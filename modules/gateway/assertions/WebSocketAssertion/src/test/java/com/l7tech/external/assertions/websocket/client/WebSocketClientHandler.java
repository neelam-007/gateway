package com.l7tech.external.assertions.websocket.client;

import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 6/20/12
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketClientHandler {

    private OutboundSocket socket=null;

    public WebSocketClientHandler() {
        super();
    }

    public void createConnection(String destination, boolean isSsl,  Map<String, String> headers) throws Exception {

        if (isSsl) {
            socket = WebSocketClientConnectionManager.getInstance().getOutboundSslWebSocket(destination, 10000,headers);
            socket.getLatch().await();
        }
        else {
            socket = WebSocketClientConnectionManager.getInstance().getOutboundWebSocket(destination, 10000,headers);
            socket.getLatch().await();
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



}
