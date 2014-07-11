package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.AuthenticationContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.WebSocket;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/29/12
 * Time: 4:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketInboundHandler extends WebSocketHandlerBase {
    protected static final Logger logger = Logger.getLogger(WebSocketInboundHandler.class.getName());

    private final AtomicInteger count = new AtomicInteger();
    private static AtomicLong socketId = new AtomicLong();
    private final Map<String, SSGInboundWebSocket> webSockets = new ConcurrentHashMap<String, SSGInboundWebSocket>();
    private final Map<HttpServletRequest, WebSocketMetadata> socketMetadataMap = new ConcurrentHashMap<HttpServletRequest, WebSocketMetadata>();
    private final Map<String, List<String>> socketReverseLookup = new ConcurrentHashMap<String, List<String>>();

    private String handlerId;
    // Updated for 8.0 GOID Support
    private Goid serviceGoid;
    private boolean loopback;
    private int maxIdleTime;
    private int maxConnections;

    public WebSocketInboundHandler(MessageProcessor messageProcessor, WebSocketConnectionEntity connection) {
        super(messageProcessor);
        this.handlerId = connection.getId();
        this.serviceGoid = connection.getInboundPolicyOID();
        this.loopback = connection.isLoopback();
        this.maxIdleTime = getMaxIdleTime(connection.getInboundMaxIdleTime(), 'I');
        this.maxConnections = getMaxConnections(connection.getInboundMaxConnections());
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WebSocketMessage msg;
        String gen_socketid = generateSocketId(request.getHeader("Sec-WebSocket-Protocol"));
        if ( (serviceGoid.getHi() != 0) && (serviceGoid.getLow() != 0) ) {
            try {
                msg = new WebSocketMessage("");
                msg.setId(gen_socketid);
                msg = processMessage(serviceGoid, msg, new HttpServletRequestKnob(request), new HttpServletResponseKnob(response), null);
                if ( msg != null) {
                    if ( msg.getStatus().equals(AssertionStatus.AUTH_REQUIRED.getMessage())) {
                        if ( response.containsHeader("WWW-Authenticate")) {
                            logger.log(Level.INFO, "Http Basic Authentication required for WebSocket");
                            response.addHeader("WWW-Authenticate", "Basic");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, msg.getStatus());
                        } else {
                            logger.log(Level.WARNING, "WebSocket handshake policy failed");
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg.getStatus());
                        }
                    } else if ( msg.getStatus().equals(AssertionStatus.AUTH_FAILED.getMessage())) {
                        if ( request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Basic")) {
                            logger.log(Level.INFO, "Http Basic Authentication required for WebSocket");
                            response.addHeader("WWW-Authenticate", "Basic");
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, msg.getStatus());
                        }
                    } else if (!msg.getStatus().equals(AssertionStatus.NONE.getMessage())) {
                        logger.log(Level.WARNING, "WebSocket handshake policy failed");
                        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg.getStatus());
                    } else { //Policy returned NONE
                        WebSocketMetadata metadata = new WebSocketMetadata(gen_socketid, msg.getAuthCtx(), request);
                        socketMetadataMap.put(request, metadata);
                        super.handle(target, baseRequest, request, response);
                    }
                } else {
                    logger.log(Level.WARNING, "WebSocket handshake policy failed, can not complete connection");
                }
            } catch (Exception e) {
               logger.log(Level.WARNING, "WebSocket handshake failed", e);
               response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            super.handle(target, baseRequest, request, response);
        }
    }

    private String generateSocketId( String protocol ) {
        //default generate an id;
        return handlerId + ":" + String.valueOf(socketId.getAndIncrement()) + ":" + protocol;
    }

    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        if (count.get() < maxConnections) {
           return new SSGInboundWebSocket(request, protocol);
        }
        return null;
    }

    void broadcastMessage(WebSocketMessage message) throws IOException, WebSocketInvalidTypeException {
        Set<String> keys = webSockets.keySet();
        for (String socket : keys) {
            sendMessage(socket, message);
        }
    }

    String[] resolveSocketId(String clientId, String protocol) {
        if ( clientId.startsWith(handlerId) ) {
            return new String[] { clientId };
        }

        String fullClientId = clientId + ":" + protocol;
        if (socketReverseLookup.containsKey(fullClientId)) {
            return socketReverseLookup.get(fullClientId).toArray(new String[socketReverseLookup.get(fullClientId).size()]);
        }

        return null;
    }

    void sendMessage(String webSocketId, WebSocketMessage message) throws WebSocketInvalidTypeException, IOException {
        if (message.getType().equals(WebSocketMessage.BINARY_TYPE)) {
            sendMessage(webSocketId, message.getPayloadAsBytes(), message.getOffset(), message.getLength());
        } else {
            sendMessage(webSocketId, message.getPayloadAsString());
        }
    }

    void closeInboundConnection(String webSocketId, String message) {
        webSockets.get(webSocketId).failAndDisconnect(message);
    }

    private void sendMessage(String webSocketId, String message) throws IOException {
        if (!webSockets.containsKey(webSocketId)) {
            throw new IOException("Message could not be delivered, connection no longer available");
        }
        webSockets.get(webSocketId).send(message);
    }

    private void sendMessage(String webSocketId, byte[] message, int i, int i1) throws IOException {
        if (!webSockets.containsKey(webSocketId)) {
            throw new IOException("Message could not be delivered, connection no longer available");
        }
        webSockets.get(webSocketId).send(message, i, i1);
    }

    private class SSGInboundWebSocket implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

        private String webSocketId;
        private Connection connection;
        private String protocol;
        private String origin;
        private String clientId;
        private AuthenticationContext authContext;
        private MockHttpServletRequest mockRequest;

        private SSGInboundWebSocket(HttpServletRequest request, String protocol ) {
            WebSocketMetadata metadata = socketMetadataMap.remove(request);
            this.webSocketId = metadata.getId();
            this.origin = request.getRemoteAddr();
            this.protocol = protocol;
            this.mockRequest = new MockHttpServletRequest(request);
            this.authContext = metadata.getAuthenticationContext();
            this.clientId = generateClientId(metadata);
            registerClientId(clientId);
            logger.log(Level.INFO, "Created Websocket -> handler:" + handlerId + ",socket id:" + webSocketId + ",subprotocol:" + protocol + ",origin:" + origin);
        }

        private void registerClientId(String clientId) {
            List<String> ids;
            if ( clientId != null && !"".equals(clientId)) {
                if ( socketReverseLookup.containsKey(clientId)) {
                    ids = socketReverseLookup.get(clientId);
                    ids.add(webSocketId);
                } else  {
                    ids = new ArrayList<String>();
                    ids.add(webSocketId);

                }
                socketReverseLookup.put(clientId, ids);
            }

        }

        private String generateClientId( WebSocketMetadata metadata) {
            if ( authContext != null && authContext.isAuthenticated()) {
                return  authContext.getLastCredentials().getLogin()+ ":" + protocol;
            } else if (metadata.hasAccessToken()) {
                //create id based on token
                return metadata.getAccessToken()+":"+protocol;
            }

            return "";
        }


        @Override
        public void onOpen(Connection connection) {
            this.connection = connection;
            this.connection.setMaxBinaryMessageSize(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_BINARY_MSG_SIZE_KEY));
            this.connection.setMaxTextMessageSize(WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_TEXT_MSG_SIZE_KEY)-1);
            this.connection.setMaxIdleTime(maxIdleTime);
            count.incrementAndGet();
            webSockets.put(webSocketId, this);
        }

        private void processOnMessage(WebSocketMessage message) throws WebSocketConnectionManagerException, IOException, WebSocketInvalidTypeException {
            message.setOrigin(origin);
            message.setProtocol(protocol);
            message.setId(webSocketId);
            message.setClientId(clientId);

            if ( (serviceGoid.getHi() != 0) && (serviceGoid.getLow() != 0) ) {
                message = processMessage(serviceGoid, message, new HttpServletRequestKnob(mockRequest), null, authContext);
            }
            if (message.getStatus().equals(AssertionStatus.NONE.getMessage())) {
                if (!loopback) {
                    sendOutboundMessage(message);
                } else {
                    sendMessage(webSocketId, message);
                }
            } else {
                disconnectionAndNotify("Unable to process message");
            }
        }

        @Override
        public void onMessage(String data) {
            try {
                WebSocketMessage msg = new WebSocketMessage(data);
                processOnMessage(msg);
            } catch (Exception e) {
                disconnectionAndNotify("Unable to create WebSocketMessage");
            }
        }

        @Override
        public void onMessage(byte[] bytes, int i, int i1) {
            try {
                WebSocketMessage msg = new WebSocketMessage(bytes, i, i1);
                processOnMessage(msg);
            } catch (Exception e) {
                disconnectionAndNotify("Unable to create WebSocketMessage");
            }

        }

        public void send(String message) throws IOException {
            connection.sendMessage(message);
        }

        public void send(byte[] message, int i, int i1) throws IOException {
            connection.sendMessage(message, i, i1);
        }

        @Override
        public void onClose(int closeCode, String message) {
            logger.log(Level.INFO, message);
            cleanupOnClose();
        }

        private void disconnectionAndNotify(String msg) {
            failAndDisconnect(msg);
            //Notify Outbound
            try {
                getOutboundHandler(handlerId).closeOutboundConnection(webSocketId, msg);
            } catch (WebSocketConnectionManagerException e) {
                logger.log(Level.WARNING,"Failed to notify Outbound connection of outbound closure");
            }
        }

        private void degisterClientId(String clientId, String websocketId) {
            if ( socketReverseLookup.containsKey(clientId)) {
                socketReverseLookup.get(clientId).remove(websocketId);
                if ( socketReverseLookup.get(clientId).isEmpty()) {
                    socketReverseLookup.remove(clientId);
                }
            }
        }

        private void cleanupOnClose() {
            count.decrementAndGet();
            webSockets.remove(webSocketId);
            degisterClientId(clientId, webSocketId);
        }

        private void failAndDisconnect(String msg) {
            logger.log(Level.WARNING, msg);
            this.connection.close(1, msg);
            cleanupOnClose();
        }

        private void sendOutboundMessage(WebSocketMessage message) throws WebSocketConnectionManagerException {
            try {
                //Send to outbound
                getOutboundHandler(handlerId).sendMessage(webSocketId, message, mockRequest, authContext);
            } catch (WebSocketInvalidTypeException e) {
                logger.log(Level.WARNING,"Failed to send Outbound message");
                disconnectionAndNotify("Outbound handler not available yet");
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to send Outbound message");
                disconnectionAndNotify("Outbound handler not available yet");
            }
        }

    }
}
