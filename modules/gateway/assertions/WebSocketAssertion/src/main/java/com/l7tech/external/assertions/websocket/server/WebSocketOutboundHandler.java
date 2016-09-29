package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
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

    // MAG-1603 "sec-websocket-version" header duplicated in websocket connection request.
    // TAC-1982 - omit sec-websocket-extensions. It will be added later through request.addExtensions().
    private static final List<String> omitHeaders = Arrays.asList("get", "origin", "connection", "sec-websocket-key", "upgrade", "host", "sec-websocket-version", "sec-websocket-extensions");

    private final Map<String, SSGOutboundWebSocket> webSockets = new ConcurrentHashMap<>();
    private final Map<String, List<String>> socketReverseLookup = new ConcurrentHashMap<>();
    private String handlerId;
    private int maxIdleTime;
    private WebSocketConnectionEntity connection;


    public WebSocketOutboundHandler(MessageProcessor messageProcessor, WebSocketConnectionEntity webSocketConnectionEntity) {
        super(messageProcessor);
        this.handlerId = webSocketConnectionEntity.getId();
        this.messageProcessor = messageProcessor;
        this.connection = webSocketConnectionEntity;
        this.maxIdleTime = getMaxIdleTime(webSocketConnectionEntity.getOutboundMaxIdleTime(), WebSocketConstants.ConnectionType.Outbound);
    }

    public void registerClientId(String clientId, String webSocketId) {

        if (StringUtils.isNotEmpty(clientId)) {

            List<String> websocketIds;

            if (socketReverseLookup.containsKey(clientId)) {
                websocketIds = socketReverseLookup.get(clientId);
                websocketIds.add(webSocketId);
            } else {
                websocketIds = new ArrayList<>();
                List<String> websocketIdsPrev = socketReverseLookup.putIfAbsent(clientId, websocketIds);
                if (websocketIdsPrev != null) {
                    websocketIds = websocketIdsPrev;
                }
                websocketIds.add(webSocketId);
            }
        }
    }

    public void degisterClientId(String clientId, final String websocketId) {
        if (socketReverseLookup.containsKey(clientId)) {
            socketReverseLookup.get(clientId).remove(websocketId);
            // we won't check if the list is empty and remove it to avoid potential synchronziation issues.
        }
    }

    public void createConnection(String connectionUrl, String webSocketId, WebSocketMessage message, MockHttpServletRequest mockRequest) throws Exception {

        createConnection(connectionUrl, webSocketId, message, mockRequest, null);
    }

    private void createConnection(String connectionUrl, String webSocketId, WebSocketMessage message, MockHttpServletRequest mockRequest, AuthenticationContext authContext) throws Exception {

        URI resolvedUrl = new URI(connectionUrl);

        if (connection.isOutboundSsl()) {
            createOutboundWebSocketClient(true, webSocketId, message.getClientId(), message.getOrigin(), message.getProtocol(), resolvedUrl,
                    connection.getOutboundPolicyOID(), mockRequest, authContext);

        } else {

            createOutboundWebSocketClient(false, webSocketId, message.getClientId(), message.getOrigin(), message.getProtocol(), resolvedUrl,
                    connection.getOutboundPolicyOID(), mockRequest, authContext);
        }
    }

    private void createOutboundWebSocketClient(boolean isSsl, String webSocketId, String clientId, String origin, String protocol, URI uri, Goid serviceGoid, MockHttpServletRequest mockRequest, AuthenticationContext authContext) throws Exception {

        if (uri == null || uri.toString().trim().isEmpty()) {
            logger.log(Level.SEVERE, "The outbound url is empty!");
            throw new URISyntaxException("Could not create outbound WebSocketclient.", "The outbound URL is null.");
        }

        try {

            SSGOutboundWebSocket outboundWebSocket = new SSGOutboundWebSocket(this, webSocketId, uri.toString(), clientId, origin, protocol, serviceGoid, mockRequest, authContext);

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            addCustomerHeaders(request, mockRequest);

            WebSocketClient wsClient;

            if (isSsl) {
                wsClient = WebSocketConnectionManager.getInstance().getOutboundWebSocketClient(true, handlerId, uri.toString(), maxIdleTime, connection);
            } else {
                wsClient = WebSocketConnectionManager.getInstance().getOutboundWebSocketClient(false, handlerId, uri.toString(), maxIdleTime, null);
            }

            if (wsClient == null) {
                getInboundHandler(handlerId).closeInboundConnection(webSocketId, StatusCode.SERVER_ERROR ,"Could not get a websocketclient.  Cannot connect to outbound URL.");
                return;
            }

            addCustomerHeaders(request, mockRequest);
            request.addExtensions("permessage-deflate"); // TAC-1982 Outbound message not being compressed.
            // Note: if the backend server does not request compression, then compression is not used.

            Future<Session> fut = wsClient.connect(outboundWebSocket, uri, request);

            // fut.get() will return when websocket session object gets created.
            // Must wait for client to connect before continuing.  Otherwise will get timing issues with WebSocket handler.
            fut.get(WebSocketConstants.getClusterProperty(WebSocketConstants.CONNECT_TIMEOUT_KEY), TimeUnit.SECONDS);

            if (WebSocketOutboundHandler.this.connection != null) {
                logger.log(Level.INFO, "Created Websocket connection to: " + uri.toString());
            }
        } catch (Exception e) {

            getInboundHandler(handlerId).closeInboundConnection(webSocketId, StatusCode.SERVER_ERROR, "createOutboundWebSocketClient Exception:" + e.toString() + "Terminating WebSocketClient");
        }
    }

    // Copy all of the headers from the mockRequest except for: "GET", "Origin", "Connection", "Sec-WebSocket-Key", "Upgrade", "Host", "Sec-WebSocket-Version"
    // to the request.
    private void addCustomerHeaders(ClientUpgradeRequest request, MockHttpServletRequest mockRequest) {

        Enumeration<String> e = mockRequest.getHeaderNames();

        while (e.hasMoreElements()) {
            String requestHeaderKey = e.nextElement();
            if (!omitHeaders.contains(requestHeaderKey.toLowerCase())) {
                request.setHeader(requestHeaderKey, mockRequest.getHeader(requestHeaderKey));
            }
        }
    }


    String[] resolveSocketId(String clientId, String protocol) {
        if (clientId.startsWith(handlerId)) {
            return new String[]{clientId};
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
            if (message.getType().equals(WebSocketMessage.BINARY_TYPE)) {
                sendMessageBinary(socket, message);
            } else {
                sendMessageText(socket, message);
            }
        }
    }

    @Override
    void sendMessage(String webSocketId, WebSocketMessage message) throws WebSocketInvalidTypeException, IOException,UnsupportedOperationException {
        throw new UnsupportedOperationException("This method is not supported.");
    }

    void sendMessage(String connectionUrl, String webSocketId, WebSocketMessage message, @Nullable MockHttpServletRequest mockRequest, @Nullable AuthenticationContext authContext) throws WebSocketInvalidTypeException, IOException {
        if (!webSockets.containsKey(webSocketId)) {
            try {
                createConnection(connectionUrl, webSocketId, message, mockRequest, authContext);
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

    void closeOutboundConnection(String webSocketId, int statusCode, String message) {

        if (webSockets.get(webSocketId) != null) {
            webSockets.get(webSocketId).cleanupOnClose(true, statusCode, message);
        }
    }

    private void sendMessageText(String webSocketId, WebSocketMessage message) throws IOException, WebSocketInvalidTypeException {
        webSockets.get(webSocketId).send(message.getPayloadAsString());
    }

    private void sendMessageBinary(String webSocketId, WebSocketMessage message) throws IOException, WebSocketInvalidTypeException {
        webSockets.get(webSocketId).send(message.getPayloadAsBytes(), message.getOffset(), message.getLength());
    }

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.register(SSGOutboundWebSocket.class);
    }

    public void addToWebSocketMap(String webSocketId, SSGOutboundWebSocket ssgOutboundWebSocket) {
        webSockets.put(webSocketId, ssgOutboundWebSocket);
    }

    public void removeFromSocketMap(String webSocketId) {
        webSockets.remove(webSocketId);
    }

    public void closeInboundConnection(String websocketId, int codeStatus, String msg) {
        try {
            getInboundHandler(handlerId).closeInboundConnection(websocketId, codeStatus, msg);
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "Caught WebSocketConnectionManagerException when attempting to close the inbound connection. " + e.toString());
        }
    }

    public void sendMessageToInboundConnection(String webSocketId, WebSocketMessage webSocketMessage) {
        try {
            getInboundHandler(handlerId).sendMessage(webSocketId, webSocketMessage);
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "WebSocketConnectionManagerException:Failed to send Inbound message " + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            webSockets.get(webSocketId).cleanupOnClose(true, StatusCode.SERVER_ERROR, "Inbound handler not available yet");
        } catch (WebSocketInvalidTypeException e) {
            logger.log(Level.WARNING, "WebSocketInvalidTypeException:Failed to send Inbound message " + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            webSockets.get(webSocketId).cleanupOnClose(true, StatusCode.SERVER_ERROR, "Inbound handler not available yet");
        } catch (IOException e) {
            logger.log(Level.WARNING, "IOException:Failed to send Inbound message " + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            webSockets.get(webSocketId).cleanupOnClose(true, StatusCode.SERVER_ERROR, "Inbound handler not available yet");
        }
    }
}
