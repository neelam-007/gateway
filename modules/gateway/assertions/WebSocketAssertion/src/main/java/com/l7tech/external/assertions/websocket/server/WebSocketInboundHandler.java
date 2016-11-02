package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    // Updated for 8.0 GOID Support
    private Goid serviceGoid;
    private Goid connectionPolicyGoid;
    private boolean loopback;
    private int maxIdleTime;
    private int maxConnections;
    private final Map<String, SSGInboundWebSocket> webSocketIdWebSocketMap = new ConcurrentHashMap<>();
    private final Map<String, List<String>> socketReverseLookup = new ConcurrentHashMap<>();
    private String handlerId;
    private String unresolvedOutboundUrl;

    public Goid getConnectionPolicyGoid() {
        return connectionPolicyGoid;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public Map<String, SSGInboundWebSocket> getWebSocketIdWebSocketMap() {
        return webSocketIdWebSocketMap;
    }

    public Map<String, List<String>> getSocketReverseLookup() {
        return socketReverseLookup;
    }

    public Goid getServiceGoid() {
        return serviceGoid;
    }

    public String getHandlerId() {
        return handlerId;
    }

    public static Logger getLogger() {
        return logger;
    }

    public boolean isLoopback() {
        return loopback;
    }

    public WebSocketInboundHandler(MessageProcessor messageProcessor, WebSocketConnectionEntity connectionEntity) {
        super(messageProcessor);
        this.handlerId = connectionEntity.getId();
        this.serviceGoid = connectionEntity.getInboundPolicyOID();
        this.connectionPolicyGoid = connectionEntity.getConnectionPolicyGOID();
        this.loopback = connectionEntity.isLoopback();
        this.maxIdleTime = getMaxIdleTime(connectionEntity.getInboundMaxIdleTime(), WebSocketConstants.ConnectionType.Inbound);
        this.maxConnections = getMaxConnections(connectionEntity.getInboundMaxConnections());
        this.unresolvedOutboundUrl = connectionEntity.getOutboundUrl();
    }

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {

        webSocketServletFactory.setCreator(new InboundSocketCreator(this));  // handle the inbound requests.
        ExtensionFactory extFact = webSocketServletFactory.getExtensionFactory();
        extFact.register("permessage-deflate", PerMessageDeflateExtension.class);

    }

    private void sendResponseErrorAndSetHandled(HttpServletRequest request,
                                                HttpServletResponse response,
                                                int httpServletResponse,
                                                String messageStatus) throws IOException {
        Request base_request = (request instanceof Request) ? (Request) request : HttpConnection.getCurrentConnection().getHttpChannel().getRequest();
        base_request.setHandled(true);
        response.sendError(httpServletResponse, messageStatus);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (getWebSocketFactory().isUpgradeRequest(request, response)) {

            if ((getConnectionPolicyGoid() != null) && (getConnectionPolicyGoid().getHi() != 0) && (getConnectionPolicyGoid().getLow() != 0)) {
                WebSocketMessage msg = null;
                try {
                    msg = new WebSocketMessage("");
                } catch (Exception e) {
                    logger.log(Level.WARNING, "WebSocket handshake failed", e);
                    sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                }

                ProcessResults processResults = processConnectionMessage(getConnectionPolicyGoid(), msg, new HttpServletRequestKnob(request), new HttpServletResponseKnob(response), null);

                if (processResults.webSocketMessage == null) {
                    logger.log(Level.WARNING, "WebSocket handshake policy failed, can not complete connection");
                    sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                } else {
                    request.setAttribute(WebSocketConstants.REQUEST_CONTEXT_VARIABLES, processResults.getContextVariable());
                    request.setAttribute(WebSocketConstants.AUTHENTICATION_CONTEXT_REQ_ATTRIB, processResults.webSocketMessage.getAuthCtx());
                    if (AssertionStatus.AUTH_REQUIRED.getMessage().equals(processResults.getMsg().getStatus())) {
                        if (response.containsHeader("WWW-Authenticate")) {
                            logger.log(Level.INFO, "Http Basic Authentication required for WebSocket");
                            response.addHeader("WWW-Authenticate", "Basic");
                            sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_UNAUTHORIZED, processResults.getMsg().getStatus());
                        } else {
                            logger.log(Level.WARNING, "WebSocket handshake policy failed");
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, processResults.getMsg().getStatus());
                        }
                    } else if (AssertionStatus.AUTH_FAILED.getMessage().equals(processResults.getMsg().getStatus())) {
                        if (request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Basic")) {
                            logger.log(Level.INFO, "Http Basic Authentication required for WebSocket");
                            response.addHeader("WWW-Authenticate", "Basic");
                            sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_UNAUTHORIZED, processResults.getMsg().getStatus());
                        } else {
                            logger.log(Level.INFO, "Authentication failed.");
                            sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_UNAUTHORIZED, processResults.getMsg().getStatus());
                        }
                    } else if (!AssertionStatus.NONE.getMessage().equals(processResults.getMsg().getStatus())) {
                        logger.log(Level.WARNING, "WebSocket handshake policy failed");
                        sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, processResults.getMsg().getStatus());
                    }
                }
            }
        }
        super.handle(target, baseRequest, request, response);
    }


    protected SSGInboundWebSocket createWebSocket(HttpServletRequest httpServletRequest, WebSocketMetadata metaData) throws WebSocketCreationException, URISyntaxException {

        // - DE245426-WebSocket - (Inbound) Maximum Connections - Client can generate one more connection than the configured Maximum Connections number.
        if ((webSocketIdWebSocketMap.size() + 1) > getMaxConnections()) {
            logger.log(Level.WARNING, "Will not create inbound websocket because the maximum number of connections will be exceeded.");
            throw new WebSocketCreationException("Will not create inbound websocket because the maximum number of connections will be exceeded.");
        }
        return new SSGInboundWebSocket(this, httpServletRequest, metaData);
    }


    void broadcastMessage(WebSocketMessage message) throws IOException, WebSocketInvalidTypeException {

        Set<String> keys = webSocketIdWebSocketMap.keySet();
        for (String socket : keys) {
            sendMessage(socket, message);
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

    void sendMessage(String webSocketId, WebSocketMessage message) throws WebSocketInvalidTypeException, IOException {
        if (message.getType().equals(WebSocketMessage.BINARY_TYPE)) {
            sendMessage(webSocketId, message.getPayloadAsBytes(), message.getOffset(), message.getLength());
        } else {
            sendMessage(webSocketId, message.getPayloadAsString());
        }
    }

    void closeInboundConnection(String webSocketId, int closeStatus, String message) {

        if (webSocketIdWebSocketMap.get(webSocketId) != null) {
            webSocketIdWebSocketMap.get(webSocketId).cleanupOnClose(closeStatus, message);
        }
    }

    private void sendMessage(String webSocketId, String message) throws IOException {
        if (!webSocketIdWebSocketMap.containsKey(webSocketId)) {
            throw new IOException("Message could not be delivered, connection no longer available");
        }
        webSocketIdWebSocketMap.get(webSocketId).send(message);
    }

    private void sendMessage(String webSocketId, byte[] message, int i, int i1) throws IOException {
        if (!webSocketIdWebSocketMap.containsKey(webSocketId)) {
            throw new IOException("Message could not be delivered, connection no longer available");
        }
        webSocketIdWebSocketMap.get(webSocketId).send(message, i, i1);
    }


    protected ProcessResults processConnectionMessage(Goid serviceGoid,
                                                      WebSocketMessage message,
                                                      HttpServletRequestKnob requestKnob,
                                                      @Nullable HttpServletResponseKnob responseKnob,
                                                      @Nullable AuthenticationContext authContext) {

        ProcessResults messageContextVariable = new ProcessResults();
        PolicyEnforcementContext context = null;
        try {
            Message request = new Message();
            Message response = new Message();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            request.initialize(message.getMessageAsDocument());
            if (authContext != null) {

                request.attachHttpRequestKnob(requestKnob);
                context.getAuthenticationContext(request).addCredentials(authContext.getLastCredentials());
            } else {
                if (requestKnob != null) {
                    request.attachHttpRequestKnob(requestKnob);
                }
                if (responseKnob != null) {
                    response.attachHttpResponseKnob(responseKnob);
                }
            }
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));

            AssertionStatus status = messageProcessor.processMessage(context);
            message.setAuthCtx(context.getAuthenticationContext(request));

            messageContextVariable.contextVariable = new HashMap<>(context.getAllVariables());

            //Check for challenges and process them
            if (response.isHttpResponse() && response.getHttpResponseKnob() instanceof HttpServletResponseKnob) {
                HttpServletResponseKnob knob = (HttpServletResponseKnob) response.getHttpResponseKnob();
                if (knob != null && knob.hasChallenge()) {
                    knob.beginChallenge();
                }
            }

            if (AssertionStatus.NONE.equals(status) && response.isInitialized()) {
                message.setPayload(XmlUtil.parse(response.getMimeKnob().getEntireMessageBodyAsInputStream()));
            }

            message.setStatus(status.getMessage());
            messageContextVariable.webSocketMessage = message;

            return messageContextVariable;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not pass websocket message to policy");
        } finally {
            if (context != null)
                ResourceUtils.closeQuietly(context);
        }
        return null;
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

    public void deregisterClientId(String clientId, String websocketId) {

        if (getSocketReverseLookup().containsKey(clientId)) {
            getSocketReverseLookup().get(clientId).remove(websocketId);
            // we won't check if the list is empty and remove it to avoid potential synchronziation issues.
        }
    }


    public void notifyCloseOutboundConnection(String webSocketId, int statusCode, String msg) {
        try {
            if (!isLoopback()) { //DE245097:The WebSocket's loopback mode is broken
                getOutboundHandler(getHandlerId()).closeOutboundConnection(webSocketId, statusCode, msg);
            }
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "Could not close the outbound connection.");
        }
    }

    private class ProcessResults {
        private WebSocketMessage webSocketMessage;
        private HashMap contextVariable;

        private WebSocketMessage getMsg() {
            return webSocketMessage;
        }

        private HashMap getContextVariable() {
            return contextVariable;
        }
    }

    public String getUnresolvedOutboundUrl() {
        return unresolvedOutboundUrl;
    }
}
