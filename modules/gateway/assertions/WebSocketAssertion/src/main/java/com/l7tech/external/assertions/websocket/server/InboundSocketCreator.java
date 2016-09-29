package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.server.message.AuthenticationContext;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created by chaja24 on 4/4/2016.
 * Created for TAC-1893: Support WebSocket Compression.
 * The InboundSocketCreator will create the inbound websocket.  This mechnanism is required to pass the HttpServletRequest and Metadata
 * to the SSGInboundWebSocket.
 */
public class InboundSocketCreator implements WebSocketCreator {

    protected static final Logger logger = Logger.getLogger(InboundSocketCreator.class.getName());
    private static final AtomicLong webSocketId = new AtomicLong();
    private final WebSocketInboundHandler webSocketInboundHandler;


    public InboundSocketCreator(WebSocketInboundHandler webSocketInboundHandler) {
        this.webSocketInboundHandler = webSocketInboundHandler;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {

        SSGInboundWebSocket ssgInboundWebSocket = null;

        try {

            ssgInboundWebSocket = webSocketInboundHandler.createWebSocket(
                    servletUpgradeRequest.getHttpServletRequest(),
                    getMetaData(servletUpgradeRequest.getHttpServletRequest()));

        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception caught creating inbound WebSocket:" + e.toString());
        }

        return ssgInboundWebSocket;
    }


    private WebSocketMetadata getMetaData(HttpServletRequest httpServletRequest) {

        String gen_webSocketid = generateWebSocketId(httpServletRequest.getProtocol());

        AuthenticationContext authenticationContext = null;
        Object authenticationContextObj = httpServletRequest.getAttribute(
                WebSocketConstants.AUTHENTICATION_CONTEXT_REQ_ATTRIB);

        if (authenticationContextObj instanceof AuthenticationContext) {
            authenticationContext = (AuthenticationContext) authenticationContextObj;
        }

        // must pass in context variables to the websocket to help resolve dynamic url before connection to backend server.
        HashMap contextVariables = null;
        Object contextVariablesObj = httpServletRequest.getAttribute(
                WebSocketConstants.REQUEST_CONTEXT_VARIABLES);

        if (contextVariablesObj instanceof HashMap) {
            contextVariables = (HashMap) contextVariablesObj;
        }

        return new WebSocketMetadata(gen_webSocketid, authenticationContext, httpServletRequest, contextVariables);
    }

    private String generateWebSocketId(String protocol) {
        //default generate an id;
        return webSocketInboundHandler.getHandlerId() + ":" + String.valueOf(webSocketId.getAndIncrement()) + ":" + protocol;
    }
}
