package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.util.ExceptionUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chaja24 on 4/4/2016.
 * Created for TAC-1893: Support WebSocket Compression.
 */

public class SSGInboundWebSocket extends WebSocketAdapter {

    private final String webSocketId;
    private final String origin;
    private final String clientId;
    private final AuthenticationContext authContext;
    private final MockHttpServletRequest mockHttpServletRequest;
    private final WebSocketInboundHandler webSocketInboundHandler;
    private final String protocol;
    private final String resolvedOutboundConnectionUrl;
    private static final String contextVariablePattern = "(\\$\\{.*?\\})";
    private boolean isLoopback = false;

    protected static final Logger logger = Logger.getLogger(SSGInboundWebSocket.class.getName());

    public SSGInboundWebSocket(WebSocketInboundHandler webSocketInboundHandler, HttpServletRequest httpServletRequest, @NotNull WebSocketMetadata webSocketMetadata) throws URISyntaxException {

        this.webSocketInboundHandler = webSocketInboundHandler;
        this.webSocketId = webSocketMetadata.getId();
        this.origin = httpServletRequest.getRemoteAddr();
        this.mockHttpServletRequest = new MockHttpServletRequest(httpServletRequest);
        this.authContext = webSocketMetadata.getAuthenticationContext();
        this.protocol = httpServletRequest.getRemoteAddr();
        this.clientId = generateClientId(webSocketMetadata, this.protocol);
        this.resolvedOutboundConnectionUrl = resolveOutboundUrl(this.webSocketInboundHandler.getUnresolvedOutboundUrl(), webSocketMetadata.getContextVariables());
        webSocketInboundHandler.registerClientId(clientId, webSocketId);
        this.isLoopback = webSocketInboundHandler.isLoopback();
    }

    private String resolveOutboundUrl(String originalOutboundUrl, HashMap contextVariables) throws URISyntaxException {

        if (isOutboundUrlNeedResolution(originalOutboundUrl)) {
            if ((contextVariables == null) || contextVariables.isEmpty()) {
                throw new URISyntaxException("No context variables found.", "Cannot resolve outbound URL:" + originalOutboundUrl.toString());
            }

            StringBuilder resolvedUri = new StringBuilder(originalOutboundUrl);

            Matcher matcher = Pattern.compile(contextVariablePattern).matcher(resolvedUri);

            while (matcher.find()) {

                String contextVarToFind = matcher.group().replaceAll("[${}]", "");
                Object resolvedContextVariableObj = contextVariables.get(contextVarToFind);
                if (resolvedContextVariableObj != null) {
                    resolvedUri.replace(matcher.start(), matcher.end(), resolvedContextVariableObj.toString());
                }
            }
            if (isOutboundUrlNeedResolution(resolvedUri.toString())) {
                throw new URISyntaxException("Not all context variables have been resolved:" + resolvedUri.toString(), "Cannot resolve outbound URL.");
            }
            return resolvedUri.toString();
        }
        return originalOutboundUrl;

    }

    private boolean isOutboundUrlNeedResolution(String uri) {

        Matcher matcher = Pattern.compile(contextVariablePattern).matcher(uri);
        return (matcher.find());
    }


    private String generateClientId(WebSocketMetadata metadata, String protocol) {
        if (authContext != null && authContext.isAuthenticated()) {
            return authContext.getLastCredentials().getLogin() + ":" + protocol;
        } else if (metadata.hasAccessToken()) {
            //create id based on token
            return metadata.getAccessToken() + ":" + protocol;
        }

        return "";
    }


    // TAC-1376 - Web socket passive client connection support.
    private void connectToServer(String webSocketId) throws Exception {

        WebSocketMessage message = new WebSocketMessage("");
        message.setOrigin(origin);
        message.setProtocol(protocol);
        message.setId(webSocketId);
        message.setClientId(clientId);

        if (!webSocketInboundHandler.getWebSocketIdWebSocketMap().containsKey(webSocketId)) {
            throw new IOException("Message could not be delivered, connection no longer available");
        }

        WebSocketOutboundHandler obh = webSocketInboundHandler.getOutboundHandler(webSocketInboundHandler.getHandlerId());

        obh.createConnection(resolvedOutboundConnectionUrl, webSocketId, message, mockHttpServletRequest);
    }

    private void processOnMessage(WebSocketMessage message) throws WebSocketConnectionManagerException, WebSocketInvalidTypeException, IOException {

        message.setOrigin(origin);
        message.setProtocol(protocol);
        message.setId(webSocketId);
        message.setClientId(clientId);

        WebSocketMessage processedMessage;

        if ((webSocketInboundHandler.getServiceGoid().getHi() != 0) && (webSocketInboundHandler.getServiceGoid().getLow() != 0)) {
            processedMessage = webSocketInboundHandler.processMessage(
                    webSocketInboundHandler.getServiceGoid(),
                    message,
                    new HttpServletRequestKnob(mockHttpServletRequest),
                    null,
                    authContext);
        } else {
            processedMessage = message;
        }

        if (processedMessage == null) {
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Unable to process message");
        } else {
            if (AssertionStatus.NONE.getMessage().equals(processedMessage.getStatus())) {
                if (!webSocketInboundHandler.isLoopback()) {
                    sendOutboundMessage(processedMessage);
                } else {
                    // situation when there is no outbound url (loopback mode).
                    webSocketInboundHandler.sendMessage(webSocketId, processedMessage);
                }
            } else {
                disconnectAndNotify(StatusCode.SERVER_ERROR, "Unable to process message");
            }
        }
    }


    public void send(String message) throws IOException {

        if (getSession().isOpen()) {
            getRemote().sendString(message);
        } else {
            logger.log(Level.WARNING, "Message could not be sent.  The session not open.");
        }
    }

    public void send(byte[] message, int offset, int length) throws IOException {

        ByteBuffer byteBuffer = ByteBuffer.wrap(message);

        if (getSession().isOpen()) {
            getRemote().sendBytes(byteBuffer);
        } else {
            logger.log(Level.WARNING, "Message could not be sent.  The session not open.");
        }
    }


    private void disconnectAndNotify(int statusCode, String msg) {

        cleanupOnClose(statusCode, msg);

        //Notify Outbound
        webSocketInboundHandler.notifyCloseOutboundConnection(webSocketId, statusCode, msg);
    }


    public void cleanupOnClose(int closeStatus, String msg) {

        logger.log(Level.INFO, "Terminating SSGInboundWebSocket for URL:" + resolvedOutboundConnectionUrl + " websocket id=" + webSocketId + ". " + msg);

        if (isConnected()) {
            getSession().close(closeStatus, msg);
        }

        webSocketInboundHandler.getWebSocketIdWebSocketMap().remove(webSocketId);
        webSocketInboundHandler.deregisterClientId(clientId, webSocketId);
    }


    private void sendOutboundMessage(WebSocketMessage message) throws WebSocketConnectionManagerException {
        try {
            //Send to outbound
            webSocketInboundHandler.getOutboundHandler(webSocketInboundHandler.getHandlerId()).sendMessage(resolvedOutboundConnectionUrl, webSocketId, message, mockHttpServletRequest, authContext);
        } catch (WebSocketInvalidTypeException e) {
            logger.log(Level.WARNING, "Failed to send Outbound message." + ExceptionUtils.getMessageWithCause(e) + "'." + ExceptionUtils.getDebugException(e));
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Outbound handler not available yet");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to send Outbound message." + ExceptionUtils.getMessageWithCause(e) + "'." + ExceptionUtils.getDebugException(e));
            disconnectAndNotify(StatusCode.SERVER_ERROR, "sendOutboundMessage IOException:" + e.toString());
        }
    }


    @Override
    public void onWebSocketConnect(Session session) {

        super.onWebSocketConnect(session);

        getSession().getPolicy().setMaxTextMessageSize(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY));
        getSession().getPolicy().setMaxBinaryMessageSize(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY));
        getSession().getPolicy().setMaxTextMessageBufferSize(WebSocketConstants.getClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY));
        getSession().getPolicy().setMaxBinaryMessageBufferSize(WebSocketConstants.getClusterProperty(WebSocketConstants.BUFFER_SIZE_KEY));
        getSession().setIdleTimeout(webSocketInboundHandler.getMaxIdleTime());

        webSocketInboundHandler.getWebSocketIdWebSocketMap().put(webSocketId, this);

        try {
            if (!isLoopback) { //  DE245097:The WebSocket's loopback mode is broken
                connectToServer(webSocketId); // connect to the server on open.  TAC-1376.
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create connection to the server for websocket id=" + webSocketId + ". Reason:" + e.toString());
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Closing SSGInboundWebSocket. Reason:" + e.toString());
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
    public void onWebSocketBinary(byte[] bytes, int offset, int len) {

        super.onWebSocketBinary(bytes, offset, len);

        try {
            WebSocketMessage msg = new WebSocketMessage(bytes, offset, len);
            processOnMessage(msg);
        } catch (Exception e) {
            disconnectAndNotify(StatusCode.SERVER_ERROR, "Unable to create WebSocketMessage");
        }
    }


    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        super.onWebSocketClose(statusCode, reason);
        disconnectAndNotify(statusCode, "Closing SSGInboundWebSocket. Reason:" + reason);

    }


    @Override
    public void onWebSocketError(Throwable cause) {

        super.onWebSocketError(cause);
        logger.log(Level.WARNING, "Inbound WebSocket OnWebSocketError():" + cause.toString() + ". Closing inbound websocket.");
    }
}