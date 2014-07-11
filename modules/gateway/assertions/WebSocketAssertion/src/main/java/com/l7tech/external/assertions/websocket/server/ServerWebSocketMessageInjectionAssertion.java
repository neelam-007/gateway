package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.websocket.WebSocketMessageInjectionAssertion;
import com.l7tech.external.assertions.websocket.WebSocketUtils;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.IOUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: cirving
 * Date: 8/20/12
 * Time: 1:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerWebSocketMessageInjectionAssertion extends AbstractServerAssertion<WebSocketMessageInjectionAssertion> {
    protected static final Logger logger = Logger.getLogger(ServerWebSocketMessageInjectionAssertion.class.getName());

    private final String[] variablesUsed;

    public ServerWebSocketMessageInjectionAssertion(final WebSocketMessageInjectionAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    private String getVariableValue(PolicyEnforcementContext context, String variable) throws NoSuchPartException, IOException {
        String value = null;
        try {
            if ( context.getVariable(variable) instanceof String ) {
                value = (String)context.getVariable(variable);
            } else if ( context.getVariable(variable) instanceof Message) {
                value = new String(IOUtils.slurpStream(((Message) context.getVariable(variable)).getMimeKnob().getEntireMessageBodyAsInputStream()));
            }
        } catch (NoSuchVariableException e) {
            //Literal value being passed
            value = variable;
        }

        return value;
    }

    private AssertionStatus checkConfiguration(WebSocketConnectionManager manager, String clientIds, String message, String subprotocol) {
        AssertionStatus status = AssertionStatus.NONE;

        try {

            if ( message == null || subprotocol == null) {
                status = AssertionStatus.FAILED;
            }

            if ( WebSocketUtils.isEmpty(clientIds) && !assertion.isBroadcast() ) {
                status = AssertionStatus.FAILED;
            }


            if ( !manager.isStarted(String.valueOf(assertion.getServiceOid()), assertion.isInbound())) {
                status = AssertionStatus.FAILED;
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not create WebSocket Message ");
            if (assertion.isDeliveryFailure()) {
                status =  AssertionStatus.FAILED;
            }
        }

        return status;
    }

    private List<String> sendToClientList(WebSocketConnectionManager manager, WebSocketMessage webSocketMessage, String clientIds) throws WebSocketInvalidTypeException, WebSocketConnectionManagerException {
        List<String> failedDeliveryIds = new ArrayList<String>();
        String[] clients = parseClientIds(clientIds, ",");
        for ( String client : clients) {
            try {
                sendMessage(client, manager, webSocketMessage);
            } catch (WebSocketNoSuchClientException e) {
                failedDeliveryIds.add(e.getMessage());
            } catch (IOException e) {
                failedDeliveryIds.add(client);
            }
        }

        return failedDeliveryIds;
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        final String clientIds = ExpandVariables.process(assertion.getClientIds(), vars, getAudit(), true);
        //final String message = ExpandVariables.process(assertion.getMessage(), vars, getAudit(), true);
        //final String subprotocol = ExpandVariables.process(assertion.getSubprotocol(), vars, getAudit(), true);

        try {

            String message = getVariableValue(context, assertion.getMessage());
            String subprotocol = getVariableValue(context, assertion.getSubprotocol());
            WebSocketConnectionManager manager = WebSocketConnectionManager.getInstance();

            if ( AssertionStatus.FAILED.equals(checkConfiguration(manager,clientIds,message,subprotocol))) {
                return AssertionStatus.FAILED;
            }

            WebSocketMessage webSocketMessage;
            if ( assertion.isTextMessage()) {
               webSocketMessage = new WebSocketMessage(message);
            } else {
               webSocketMessage = new WebSocketMessage(message.getBytes(), 0, message.length());
            }
            webSocketMessage.setProtocol(subprotocol);

            if (assertion.isBroadcast()) {
                broadcastMessage(manager, webSocketMessage);
            } else {

                List<String> failedToDeliver = sendToClientList(manager,webSocketMessage,clientIds);

                if ( !failedToDeliver.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for ( String failedId : failedToDeliver) {
                        sb.append(failedId);
                        sb.append(" ");
                    }
                    logger.log(Level.INFO, "Could not deliver messages to the following client ids: " + sb.toString());
                    context.setVariable("websocket.delivery.failures", failedToDeliver.toArray());
                    if ( assertion.isDeliveryFailure()) {
                        return AssertionStatus.FAILED;
                    }
                }
            }

        } catch (WebSocketConnectionManagerException e) {
            logger.log(Level.WARNING, "Could not connect to WebSocket Connection Manager");
            return  AssertionStatus.FAILED;
        } catch (NoSuchVariableException e) {
            logger.log(Level.WARNING, "Could not evaluate message variable.");
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not create WebSocket Message ");
            if (assertion.isDeliveryFailure()) {
                return  AssertionStatus.FAILED;
            }
        }

        //All messages delivered, no failures needed to record.
        return AssertionStatus.NONE;
    }

    private void sendMessage(String clientId, WebSocketConnectionManager manager, WebSocketMessage webSocketMessage) throws WebSocketConnectionManagerException, WebSocketNoSuchClientException, IOException, WebSocketInvalidTypeException {
        WebSocketHandlerBase handler = null;
        if ( assertion.isInbound()) {
            handler = manager.getInboundHandler(String.valueOf(assertion.getServiceOid()));
        } else {
            handler = manager.getOutboundHandler(String.valueOf(assertion.getServiceOid()));
        }

        String [] ids = handler.resolveSocketId(clientId, webSocketMessage.getProtocol());
        if ( ids == null || ids.length == 0) {
            throw new WebSocketNoSuchClientException(clientId);
        }
        for (String id : ids) {
            handler.sendMessage(id, webSocketMessage);
        }

    }

    private String[] parseClientIds(String clientIds, String delimiter) {
        return clientIds.split(delimiter);
    }

    private void broadcastMessage(WebSocketConnectionManager manager, WebSocketMessage webSocketMessage) throws WebSocketConnectionManagerException, WebSocketInvalidTypeException, IOException {
        if ( assertion.isInbound()) {
            WebSocketInboundHandler inboundHandler = manager.getInboundHandler(String.valueOf(assertion.getServiceOid()));
            inboundHandler.broadcastMessage(webSocketMessage);
        } else {
            WebSocketOutboundHandler outboundHandler = manager.getOutboundHandler(String.valueOf(assertion.getServiceOid()));
            outboundHandler.broadcastMessage(webSocketMessage);
        }
    }
}
