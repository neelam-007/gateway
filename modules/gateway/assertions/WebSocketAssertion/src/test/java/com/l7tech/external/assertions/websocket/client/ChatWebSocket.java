package com.l7tech.external.assertions.websocket.client;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Created by chaja24 on 5/4/2016.
 */
public class ChatWebSocket implements WebSocketListener {

    private Session session;
    Set<Session> sessionSet;

    public ChatWebSocket(Set<Session> sessionSet) {
        this.sessionSet = sessionSet;
    }

    public void onWebSocketConnect(Session session) {

        this.session = session;
        this.session.getPolicy().setMaxBinaryMessageSize(1024*1024);
        this.session.getPolicy().setMaxTextMessageSize(1024*1024);
        this.session.getPolicy().setIdleTimeout(480000);
        sessionSet.add(this.session);

    }

    public void onWebSocketText(String message) {
        try {

            for (Session aSession: sessionSet) {
                aSession.getRemote().sendString(message);
            }
        } catch (IOException e) {
            this.session.close();
        }
    }


    public void onWebSocketBinary(byte[] arg0, int arg1, int len) {
        try {

            for (Session aSession: sessionSet) {
                aSession.getRemote().sendBytes(ByteBuffer.wrap(arg0));
            }
        } catch (IOException e) {
            this.session.close();
        }
    }


    public void onWebSocketClose(int statusCode, String reason)
    {
        sessionSet.remove(this.session);
        this.session = null;
    }


    public void onWebSocketError(Throwable cause)
    {
        System.out.println("onWebSocketError");
    }

}
