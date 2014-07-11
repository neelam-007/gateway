package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.AuthenticationContext;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 5/31/12
 * Time: 9:19 AM
 * To change this template use File | Settings | File Templates.
 */
public class WebSocketOutboundHandler extends WebSocketHandlerBase {
    protected static final Logger logger = Logger.getLogger(WebSocketOutboundHandler.class.getName());

    private final Map<String, SSGOutboundWebSocket> webSockets = new ConcurrentHashMap<String, SSGOutboundWebSocket>();
    private final Map<String, List<String>> socketReverseLookup = new ConcurrentHashMap<String, List<String>>();
    private String handlerId;
    private URI uri;
    private int maxIdleTime;
    private WebSocketConnectionEntity connection;


    public WebSocketOutboundHandler(MessageProcessor messageProcessor, WebSocketConnectionEntity connection) {
        super(messageProcessor);
        this.handlerId = connection.getId();
        this.messageProcessor = messageProcessor;
        try {
            this.uri = new URI(connection.getOutboundUrl());
        } catch (URISyntaxException e) {
           logger.log(Level.WARNING, "Malformed URI format");
        }
        this.connection = connection;
        this.maxIdleTime = getMaxIdleTime(connection.getOutboundMaxIdleTime(), 'O');
    }

    private void createConnection(String webSocketId, WebSocketMessage message, MockHttpServletRequest mockRequest, AuthenticationContext authContext) throws Exception {
        if (connection.isOutboundSsl()) {
            new SSGOutboundWebSocket(webSocketId, message.getClientId(), uri, WebSocketConnectionManager.getInstance().getOutBoundSslWebSocket(message.getOrigin(),
                    message.getProtocol(),maxIdleTime, handlerId), connection.getOutboundPolicyOID(), mockRequest, authContext);
        } else {
            new SSGOutboundWebSocket(webSocketId, message.getClientId(), uri, WebSocketConnectionManager.getInstance().getOutBoundWebSocket(message.getOrigin(),
                    message.getProtocol(),maxIdleTime), connection.getOutboundPolicyOID(), mockRequest, authContext);
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

    void broadcastMessage(WebSocketMessage message) throws IOException, WebSocketInvalidTypeException {
        Set<String> keys = webSockets.keySet();
        for (String socket : keys) {
            if ( message.getType().equals(WebSocketMessage.BINARY_TYPE)) {
                sendMessageBinary(socket, message);
            } else {
                sendMessageText(socket, message);
            }
        }
    }

    @Override
    void sendMessage(String webSocketId, WebSocketMessage message) throws WebSocketInvalidTypeException, IOException {
        sendMessage(webSocketId, message, null, null);
    }

    void sendMessage(String webSocketId, WebSocketMessage message, @Nullable MockHttpServletRequest mockRequest, @Nullable AuthenticationContext authContext) throws WebSocketInvalidTypeException, IOException {
        if (!webSockets.containsKey(webSocketId)) {
            try {
                createConnection(webSocketId, message, mockRequest, authContext);
            } catch (Exception e) {
                throw new IOException("Unable to send outbound message", e);
            }
        }
        if (message.getType().equals(WebSocketMessage.BINARY_TYPE)) {
            sendMessageBinary(webSocketId, message);
        } else {
            sendMessageText(webSocketId, message);
        }
    }

    void closeOutboundConnection(String webSocketId, String message) {
        webSockets.get(webSocketId).failAndDisconnect(message);
    }

    private void sendMessageText(String webSocketId, WebSocketMessage message) throws IOException, WebSocketInvalidTypeException {
        webSockets.get(webSocketId).send(message.getPayloadAsString());
    }

    private void sendMessageBinary(String webSocketId, WebSocketMessage message ) throws IOException, WebSocketInvalidTypeException {
        webSockets.get(webSocketId).send(message.getPayloadAsBytes(), message.getOffset(), message.getLength());
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest httpServletRequest, String s) {
        return null; //Meant to do nothing
    }

    private class SSGOutboundWebSocket implements WebSocket.OnTextMessage, WebSocket.OnBinaryMessage {

        private String webSocketId;
        private String clientId;
        // Updated to use 8.0 GOIDs
        private Goid serviceGoid;
        private Connection connection;
        private String origin;
        private String protocol;
        private AuthenticationContext authContext;
        private MockHttpServletRequest mockRequest;


        public SSGOutboundWebSocket(String webSocketId, String clientId, URI uri, WebSocketClient client, Goid serviceGoid, MockHttpServletRequest mockRequest, AuthenticationContext authContext) throws Exception {
            this.serviceGoid = serviceGoid;
            this.webSocketId = webSocketId;
            this.clientId = clientId;
            this.origin = client.getOrigin();
            this.authContext = authContext;
            this.mockRequest = mockRequest;
            this.protocol = client.getProtocol();
            addCustomerHeaders(client, mockRequest);
            registerClientId(clientId);
            connection = client.open(uri, this, WebSocketConstants.getClusterProperty(WebSocketConstants.CONNECT_TIMEOUT_KEY), TimeUnit.SECONDS);
        }

        private void registerClientId(String clientId) {
            List<String> ids = null;
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

        @Override
        public void onOpen(Connection connection) {
            webSockets.put(webSocketId, this);
        }

        private void addCustomerHeaders(WebSocketClient client, MockHttpServletRequest request) {
            List<String> headers = Arrays.asList("GET", "Origin", "Connection", "Sec-WebSocket-Key", "Upgrade");
            Enumeration<String> e = request.getHeaderNames();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                if (!headers.contains(key)) {
                     client.addHeader(key, request.getHeader(key));
                }
            }
        }

        private void processOnMessage(WebSocketMessage message) throws WebSocketConnectionManagerException, IOException, WebSocketInvalidTypeException {
            message.setOrigin(origin);
            message.setProtocol(connection.getProtocol());

            if ( (serviceGoid.getHi() != 0) && (serviceGoid.getLow() != 0) ){
                message = processMessage(serviceGoid, message, new HttpServletRequestKnob(mockRequest), null, authContext);
            }
            if (message.getStatus().equals(AssertionStatus.NONE.getMessage())) {
                sendInboundMessage(message);
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
            cleanupOnClose();
        }

        private void disconnectionAndNotify (String msg) {
            failAndDisconnect(msg);
            //Notify Inbound
            try {
                getInboundHandler(handlerId).closeInboundConnection(webSocketId,msg);
            } catch (WebSocketConnectionManagerException e) {
                logger.log(Level.WARNING,"Failed to notify Inbound connection of outbound closure");
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
            webSockets.remove(webSocketId);
            degisterClientId(clientId, webSocketId);
        }

        private void failAndDisconnect(String msg) {
            logger.log(Level.WARNING, msg);
            this.connection.close(1, msg);
            cleanupOnClose();
        }

        private void sendInboundMessage(WebSocketMessage message) {
            try {
                //Send to Inbound
                getInboundHandler(handlerId).sendMessage(webSocketId, message);
            } catch (WebSocketConnectionManagerException e) {
                logger.log(Level.WARNING,"Failed to send Inbound message");
                disconnectionAndNotify("Inbound handler not available yet");
            } catch (WebSocketInvalidTypeException e) {
                logger.log(Level.WARNING,"Failed to send Inbound message");
                disconnectionAndNotify("Inbound handler not available yet");
            } catch (IOException e) {
                logger.log(Level.WARNING,"Failed to send Inbound message");
                disconnectionAndNotify("Inbound handler not available yet");
            }
        }
    }
}
