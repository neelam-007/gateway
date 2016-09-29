package com.l7tech.external.assertions.websocket.server;

import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.AuthenticationContext;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/31/12
 * Time: 9:19 AM
 * To change this template use File | Settings | File Templates.
 */


public class SSGOutboundWebSocket extends WebSocketAdapter {

    private final String webSocketId;
    private final String clientId;
    // Updated to use 8.0 GOIDs
    private final Goid serviceGoid;
    private final AuthenticationContext authContext;
    private final MockHttpServletRequest mockRequest;
    private final String origin;
    private final String protocol;
    private final String outboundUri;
    private final WebSocketOutboundHandler webSocketOutboundHandler;

    protected static final Logger logger = Logger.getLogger(SSGOutboundWebSocket.class.getName());

    public SSGOutboundWebSocket(WebSocketOutboundHandler webSocketOutboundHandler, String webSocketId, String outboundUri, String clientId, String origin, String protocol, Goid serviceGoid, MockHttpServletRequest mockRequest, AuthenticationContext authContext) throws Exception {

        this.webSocketOutboundHandler = webSocketOutboundHandler;
        this.serviceGoid = serviceGoid;
        this.webSocketId = webSocketId;
        this.clientId = clientId;
        this.origin = origin;
        this.protocol = protocol;
        this.authContext = authContext;
        this.mockRequest = mockRequest;
        this.outboundUri = outboundUri;

        webSocketOutboundHandler.registerClientId(clientId, webSocketId);
    }


    private void processOnMessage(WebSocketMessage message) throws WebSocketConnectionManagerException, IOException, WebSocketInvalidTypeException {

        message.setOrigin(origin);
        message.setProtocol(protocol);

        if ((serviceGoid.getHi() != 0) && (serviceGoid.getLow() != 0)) {
            message = webSocketOutboundHandler.processMessage(serviceGoid, message, new HttpServletRequestKnob(mockRequest), null, authContext);
        }
        if (message.getStatus().equals(AssertionStatus.NONE.getMessage())) {
            sendInboundMessage(message);
        } else {
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Unable to process message");
        }
    }

    public void send(String message) throws IOException {

        if (getSession().isOpen()) {
            getRemote().sendString(message);
        } else {
            logger.log(Level.WARNING, "Session not open to send text message.");
            cleanupOnClose(true, StatusCode.SERVER_ERROR, message);
        }
    }

    public void send(byte[] message, int offset, int length) throws IOException {

        ByteBuffer byteBuffer = ByteBuffer.wrap(message);

        if (getSession().isOpen()) {
            getRemote().sendBytes(byteBuffer);
        } else {
            logger.log(Level.WARNING, "Session not open to send binary message.");
            cleanupOnClose(true, StatusCode.SERVER_ERROR, "Session not open to send binary message.");
        }
    }

    private void disconnectAndNotify(int statusCode, String msg) {

        cleanupOnClose(true, statusCode, msg);

        //Notify Inbound
        webSocketOutboundHandler.closeInboundConnection(webSocketId, statusCode, msg);

    }

    public void cleanupOnClose(boolean bCloseSession, int statusCode, String msg) {

        if (bCloseSession) {
            if (isConnected()) {
                logger.log(Level.INFO, "Terminating SSGOutboundSocket for " + outboundUri + " websocket id=" + webSocketId + ". " +msg);
                getSession().close(statusCode, msg);
            }
        }

        webSocketOutboundHandler.removeFromSocketMap(webSocketId);
        webSocketOutboundHandler.degisterClientId(clientId, webSocketId);
    }


    private void sendInboundMessage(WebSocketMessage message) {
        //Send to Inbound
        webSocketOutboundHandler.sendMessageToInboundConnection(webSocketId, message);
    }

    @Override
    public void onWebSocketConnect(Session session) {

        super.onWebSocketConnect(session);
        webSocketOutboundHandler.addToWebSocketMap(webSocketId, this);
    }

    @Override
    public void onWebSocketBinary(byte[] bytes, int i, int i1) {

        super.onWebSocketBinary(bytes, i, i1);

        try {
            WebSocketMessage msg = new WebSocketMessage(bytes, i, i1);
            processOnMessage(msg);
        } catch (Exception e) {
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Unable to create WebSocketMessage");
        }
    }

    @Override
    public void onWebSocketText(String s) {

        super.onWebSocketText(s);
        try {
            WebSocketMessage msg = new WebSocketMessage(s);
            processOnMessage(msg);
        } catch (Exception e) {
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Unable to create WebSocketMessage");
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        super.onWebSocketClose(statusCode, reason);
        cleanupOnClose(false, statusCode, "Code:" + String.valueOf(statusCode) + ". Reason:" + reason);
    }

    @Override
    public void onWebSocketError(Throwable cause) {

        super.onWebSocketError(cause);
        logger.log(Level.WARNING, "Outbound WebSocket onWebSocketError()" + cause.toString() + ". Closing outbound websocket.");
        disconnectAndNotify(StatusCode.SERVER_ERROR, "onWebSocketError Exception:" + cause.toString());
    }
}