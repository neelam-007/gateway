package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.websocket.InvalidRangeException;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.api.extensions.ExtensionFactory;
import org.eclipse.jetty.websocket.common.extensions.compress.PerMessageDeflateExtension;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
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
    private String outboundUrl;
    private final Audit audit;

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

    public WebSocketInboundHandler(MessageProcessor messageProcessor, WebSocketConnectionEntity connectionEntity,AuditFactory auditFactory) {
        super(messageProcessor);
        this.handlerId = connectionEntity.getId();
        this.serviceGoid = connectionEntity.getInboundPolicyOID();
        this.connectionPolicyGoid = connectionEntity.getConnectionPolicyGOID();
        this.loopback = connectionEntity.isLoopback();
        this.maxIdleTime = getMaxIdleTime(connectionEntity.getInboundMaxIdleTime(), WebSocketConstants.ConnectionType.Inbound);
        this.maxConnections = getMaxConnections(connectionEntity.getInboundMaxConnections());
        this.outboundUrl = connectionEntity.getOutboundUrl();
        this.audit = auditFactory.newInstance(this, logger);
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

                ProcessMessageResults processMessageResults = processConnectionMessage(getConnectionPolicyGoid(), msg, new HttpServletRequestKnob(request), new HttpServletResponseKnob(response), null);

                if (processMessageResults == null || processMessageResults.getWebSocketMsg() == null) {
                    logger.log(Level.WARNING, "WebSocket handshake policy failed, can not complete connection");
                    sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                } else {
					if (processMessageResults.getOutboundUrl() != null ){
						request.setAttribute(WebSocketConstants.OUTBOUND_URL, processMessageResults.getOutboundUrl());
					}

                    request.setAttribute(WebSocketConstants.AUTHENTICATION_CONTEXT_REQ_ATTRIB, processMessageResults.getWebSocketMsg().getAuthCtx());
                    if (AssertionStatus.AUTH_REQUIRED.getMessage().equals(processMessageResults.getWebSocketMsg().getStatus())) {
                        if (response.containsHeader("WWW-Authenticate")) {
                            logger.log(Level.INFO, "Http Basic Authentication required for WebSocket");
                            response.addHeader("WWW-Authenticate", "Basic");
                            sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_UNAUTHORIZED, processMessageResults.getWebSocketMsg().getStatus());
                        } else {
                            logger.log(Level.WARNING, "WebSocket handshake policy failed");
                            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, processMessageResults.getWebSocketMsg().getStatus());
                        }
                    } else if (AssertionStatus.AUTH_FAILED.getMessage().equals(processMessageResults.getWebSocketMsg().getStatus())) {
                        if (request.getHeader("Authorization") != null && request.getHeader("Authorization").startsWith("Basic")) {
                            logger.log(Level.INFO, "Http Basic Authentication required for WebSocket");
                            response.addHeader("WWW-Authenticate", "Basic");
                            sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_UNAUTHORIZED, processMessageResults.getWebSocketMsg().getStatus());
                        } else {
                            logger.log(Level.INFO, "Authentication failed.");
                            sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_UNAUTHORIZED, processMessageResults.getWebSocketMsg().getStatus());
                        }
                    } else if (!AssertionStatus.NONE.getMessage().equals(processMessageResults.getWebSocketMsg().getStatus())) {
                        logger.log(Level.WARNING, "WebSocket handshake policy failed");
                        sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, processMessageResults.getWebSocketMsg().getStatus());
                    }
                }
            } else {
                if (isValidURI(getOutboundUrl())) {
                    request.setAttribute(WebSocketConstants.OUTBOUND_URL,getOutboundUrl());
                } else {
                    logger.log(Level.WARNING, "The outbound URL: {0} is not valid. WebSocket handshake failed.",getOutboundUrl());
                    sendResponseErrorAndSetHandled(request, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
                }
            }
            copyWebSocketProtocolsToHttpServletResponse(request, response);
        }
        super.handle(target, baseRequest, request, response);
    }


    private static boolean isValidURI(String uriStr) {
        try {
            String uriStrLowerCase = uriStr.toLowerCase();
            if ((!uriStrLowerCase.startsWith("ws://")) && !uriStrLowerCase.startsWith("wss://")) {
                return false;
            }

            URI uri = new URI(uriStr);
            return true;
        }
        catch (URISyntaxException e) {
            return false;
        }
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


    protected ProcessMessageResults processConnectionMessage(Goid serviceGoid,
                                                             WebSocketMessage message,
                                                             HttpServletRequestKnob requestKnob,
                                                             @Nullable HttpServletResponseKnob responseKnob,
                                                             @Nullable AuthenticationContext authContext) {

        ProcessMessageResults processMessageResults = new ProcessMessageResults();
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
                    // - DE299429-WebSocket - (Inbound) Ensure the HTTP Upgrade Request headers are copied to the request message/policy modifiable
                    copyHttpServletRequestHeadersToRequestMessage(requestKnob, request);
                }
                if (responseKnob != null) {
                    response.attachHttpResponseKnob(responseKnob);
                }
            }
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));

            AssertionStatus status = messageProcessor.processMessage(context);
            message.setAuthCtx(context.getAuthenticationContext(request));

            processMessageResults.setOutboundUrl(outboundUrl);

            // check if the outbound URL has any context variables and resolve them.
            String[] variablesUsed = Syntax.getReferencedNames(getOutboundUrl());
            Map<String, Object> varMap = context.getVariableMap(variablesUsed, audit);

            if(ExpandVariables.isVariableReferencedNotFound(getOutboundUrl(), varMap, audit)){
                logger.log(Level.WARNING, "Error, the outbound URL:{0} could not resolve all the context variables.",outboundUrl);
                return null;
            }

            String resolvedOutboundUrl = ExpandVariables.process(getOutboundUrl(), varMap, audit);

            if (isValidURI(resolvedOutboundUrl)){
                processMessageResults.setOutboundUrl(resolvedOutboundUrl);
            } else {
                logger.log(Level.WARNING, "Error, the outbound URL:{0} is not valid.",resolvedOutboundUrl);
                return null;
            }

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
            processMessageResults.setWebSocketMsg(message);
            // - DE299429-WebSocket - Set the HTTP Response code to OK_200, as Assertion processing has passed successfully.
            if (AssertionStatus.NONE.equals(status)){
                responseKnob.setStatus(HttpStatus.OK_200);
            }
            // - DE299429-WebSocket - (Inbound) Place any modified HTTP Upgrade Transport Headers back into the Response Servlet.
            copyMessageHeadersToHttpServletResponseKnob(response, responseKnob);
            return processMessageResults;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not process message {0}",e.toString());
        } finally {
            if (context != null)
                ResourceUtils.closeQuietly(context);
        }
        return null;
    }

    protected void copyHttpServletRequestHeadersToRequestMessage(HttpServletRequestKnob requestKnob, Message message){
        final String[] headers = requestKnob.getHeaderNames();
        for(String header : headers) {
            final String[] vals = requestKnob.getHeaderValues(header);
            for(String value : vals ) {
                message.getHeadersKnob().addHeader(header, value, HeadersKnob.HEADER_TYPE_HTTP);
            }
        }
        logger.log(Level.FINE, "Upgrade HTTP Request Headers: "+ Arrays.toString(message.getHeadersKnob().getHeaders().toArray()));
    }

    protected void copyMessageHeadersToHttpServletResponseKnob(Message message, @NotNull HttpServletResponseKnob httpServletResponseKnob){
        if (!WebSocketConstants.getBooleanClusterProperty(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY)) {
            final Iterator<Header> httpHeaders = message.getHeadersKnob().getHeaders().iterator();
            Collection<Pair<String, Object>> httpHeadersCollection = new ArrayList<Pair<String, Object>>();
            while (httpHeaders.hasNext()){
                final Header header = httpHeaders.next();
                httpHeadersCollection.add(new Pair<>(header.getKey(), header.getValue()));
            }
            httpServletResponseKnob.beginResponse(httpHeadersCollection,  Collections.<HttpCookie>emptyList());
        }
    }

    protected void copyWebSocketProtocolsToHttpServletResponse(@NotNull HttpServletRequest request, @NotNull HttpServletResponse httpServletResponse){
        if (WebSocketConstants.getBooleanClusterProperty(WebSocketConstants.INBOUND_COPY_UPGRADE_REQUEST_SUBPROTOCOL_HEADER_KEY)) {
            final Enumeration protocols = request.getHeaders(WebSocketConstants.SECURITY_WEBSOCKET_PROTOCOL_KEY);
            while (protocols.hasMoreElements()) {
                httpServletResponse.addHeader(WebSocketConstants.SECURITY_WEBSOCKET_PROTOCOL_KEY, (String) protocols.nextElement());
            }
        }
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

    private String getOutboundUrl() {
        return outboundUrl;
    }

    private class ProcessMessageResults {
        private WebSocketMessage webSocketMessage;
        private String outboundUrl;

        private WebSocketMessage getWebSocketMsg() {
            return webSocketMessage;
        }

        private void setWebSocketMsg(WebSocketMessage webSocketMessage) {
            this.webSocketMessage = webSocketMessage;
        }

        private String getOutboundUrl() {
            return outboundUrl;
        }

        private void setOutboundUrl(String url) {
            outboundUrl = url;
        }
    }

    protected static class WebSocketInboundHandlerBuilder {

        private MessageProcessor messageProcessor;
        private WebSocketConnectionEntity connectionEntity;
        private AuditFactory auditFactory;

       public WebSocketInboundHandlerBuilder(){
            connectionEntity = new WebSocketConnectionEntity();
        }

        public WebSocketInboundHandlerBuilder withConnectionEntity(final WebSocketConnectionEntity newConnectionEntity) {
            this.connectionEntity = newConnectionEntity;
            return this;
        }

        public WebSocketInboundHandlerBuilder withMessageProcessor(final MessageProcessor newMessageProcessor) {
            this.messageProcessor = newMessageProcessor;
            return this;
        }

        public WebSocketInboundHandlerBuilder withHandlerId(final String newHandlerId) {
            this.connectionEntity.setId(newHandlerId);
            return this;
        }

        public WebSocketInboundHandlerBuilder withConnectionPolicyGoid(final Goid newConnectionPolicyGoid) {
            this.connectionEntity.setConnectionPolicyGOID(newConnectionPolicyGoid);
            return this;
        }

        public WebSocketInboundHandlerBuilder withInboundPolicyGoid(final Goid newInboundPolicyGoid) {
            this.connectionEntity.setInboundPolicyOID(newInboundPolicyGoid);
            return this;
        }

        public WebSocketInboundHandlerBuilder withMaxIdleTime(final int newMaxIdleTime) {
            this.connectionEntity.setInboundMaxIdleTime(newMaxIdleTime);
            return this;
        }

        public WebSocketInboundHandlerBuilder withMaxConnections(final int newMaxConnections) throws InvalidRangeException {
            this.connectionEntity.setInboundMaxConnections(newMaxConnections);
            return this;
        }

        public WebSocketInboundHandlerBuilder withOutboundUrl(final String newOutboundUrl) {
            this.connectionEntity.setOutboundUrl(newOutboundUrl);
            return this;
        }

        public WebSocketInboundHandlerBuilder withAuditFactory(final AuditFactory newAuditFactory) {
            this.auditFactory = newAuditFactory;
            return this;
        }

        public WebSocketInboundHandler build() {
            return new WebSocketInboundHandler(messageProcessor, connectionEntity, auditFactory);
        }
    }
}