package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ResourceUtils;
import org.eclipse.jetty.websocket.WebSocketHandler;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: cirving
 * Date: 5/31/12
 * Time: 1:34 PM
 */
public abstract class WebSocketHandlerBase extends WebSocketHandler {
    protected static final Logger logger = Logger.getLogger(WebSocketHandlerBase.class.getName());

    protected MessageProcessor messageProcessor;

    protected WebSocketHandlerBase(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    protected WebSocketInboundHandler getInboundHandler(String id) throws WebSocketConnectionManagerException {
        try {
            return WebSocketConnectionManager.getInstance().getInboundHandler(id);
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.INFO, "Inbound handler not available yet");
            throw new WebSocketConnectionManagerException("Inbound handler not available yet");
        }
    }

    protected WebSocketOutboundHandler getOutboundHandler(String handlerId) throws WebSocketConnectionManagerException {
        try {
            return WebSocketConnectionManager.getInstance().getOutboundHandler(handlerId);
        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "Outbound handler not available yet");
            throw new WebSocketConnectionManagerException("Outbound handler not available yet");
        }
    }

    protected WebSocketMessage processMessage(Goid serviceGoid, WebSocketMessage message, HttpServletRequestKnob requestKnob, @Nullable HttpServletResponseKnob responseKnob, @Nullable AuthenticationContext authContext) {

        PolicyEnforcementContext context = null;
        try {
            Message request = new Message();
            Message response = new Message();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            request.initialize(message.getMessageAsDocument());
            if (authContext != null ) {
                request.attachHttpRequestKnob(requestKnob);
                context.getAuthenticationContext(request).addCredentials(authContext.getLastCredentials());
            } else {
                if ( requestKnob != null) {
                    request.attachHttpRequestKnob(requestKnob);
                }
                if ( responseKnob != null) {
                    response.attachHttpResponseKnob(responseKnob);
                }
            }
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));

            AssertionStatus status = messageProcessor.processMessage(context);
            message.setAuthCtx(context.getAuthenticationContext(request));

            //Check for challenges and process them
            if (response.isHttpResponse() && response.getHttpResponseKnob() instanceof HttpServletResponseKnob ) {
                HttpServletResponseKnob knob = (HttpServletResponseKnob)response.getHttpResponseKnob();
                if ( knob != null && knob.hasChallenge()) {
                    knob.beginChallenge();
                }
            }
            if ( status.equals(AssertionStatus.NONE) && response.isInitialized()) {
                message.setPayload(XmlUtil.parse(response.getMimeKnob().getEntireMessageBodyAsInputStream()));
            }
            message.setStatus(status.getMessage());
            return message;

        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not pass websocket message to policy");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not pass websocket message to policy");
        } finally {
            if (context != null)
                ResourceUtils.closeQuietly(context);
        }
        return null;

    }

    abstract String[] resolveSocketId(String clientId, String protocol);

    abstract void sendMessage(String webSocketId, WebSocketMessage message) throws WebSocketInvalidTypeException, IOException;

    protected static int getMaxIdleTime(int connectionValue, char direction) {
        if (direction == 'I') {
            return WebSocketUtils.returnLowerValue(connectionValue, WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_INBOUND_IDLE_TIME_MS_KEY));
        }
        if (direction == 'O') {
            return WebSocketUtils.returnLowerValue(connectionValue, WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_OUTBOUND_IDLE_TIME_MS_KEY));
        }
        return -1;
    }

    protected static int getMaxConnections(int connectionValue) {
        return WebSocketUtils.returnLowerValue(connectionValue, WebSocketConstants.getClusterProperty(WebSocketConstants.MAX_INBOUND_CONNECTIONS_KEY));
    }

}
