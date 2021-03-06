package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConstants;
import com.l7tech.server.message.AuthenticationContext;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
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

        // retreive the resolved outbound URL if it exists from the httpServletRequest.
        Object outboundUrlObj = httpServletRequest.getAttribute(
                WebSocketConstants.OUTBOUND_URL);

        String outboundUrl = null;

		if (outboundUrlObj != null) {
	        if (outboundUrlObj instanceof String) {
	            outboundUrl = (String) outboundUrlObj;
	        }
		}
        Collection connectionPolicyHeaders = null;
        final Object connectionPolicyHeadersObj = httpServletRequest.getAttribute(
                WebSocketConstants.REQUEST_HEADERS_FROM_CONN_POLICY_ATTR);

        if (connectionPolicyHeadersObj instanceof Collection) {
            connectionPolicyHeaders = (Collection) connectionPolicyHeadersObj;
        }
        return new WebSocketMetadata(gen_webSocketid, authenticationContext, httpServletRequest, outboundUrl, connectionPolicyHeaders);
    }

    private String generateWebSocketId(String protocol) {
        //default generate an id;
        return webSocketInboundHandler.getHandlerId() + ":" + String.valueOf(webSocketId.getAndIncrement()) + ":" + protocol;
    }
}
