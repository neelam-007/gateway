package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPMinaClassException;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 13/03/12
 * Time: 10:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class OutboundMessageHandler extends MessageHandlerBase {
    private static final Logger logger = Logger.getLogger(InboundMessageHandler.class.getName());

    public OutboundMessageHandler(XMPPConnectionEntity entity,
                                  StashManagerFactory stashManagerFactory,
                                  MessageProcessor messageProcessor,
                                  SessionStartedCallback sessionStartedCallback,
                                  SessionTerminatedCallback sessionTerminatedCallback,
                                  XMPPClassHelper classHelper)
    throws XMPPClassHelperNotInitializedException, XMPPMinaClassException
    {
        super(entity, stashManagerFactory, messageProcessor, sessionStartedCallback, sessionTerminatedCallback, classHelper);
    }

    @Override
    protected void setContextVariables(PolicyEnforcementContext context, Object session) {
        try {
            Long sessionId = null;
            try {
                sessionId = classHelper.getSessionId(session);
            } catch(XMPPMinaClassException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                return;
            }

            context.setVariable("xmpp.outbound.sessionID", Long.toString(sessionId));

            try {
                SocketAddress localAddress = classHelper.getLocalAddress(session);
                if(localAddress != null) {
                    context.setVariable("xmpp.outbound.localAddress", ((InetSocketAddress)localAddress).getHostName());
                    context.setVariable("xmpp.outbound.localPort", ((InetSocketAddress)localAddress).getPort());
                }
            } catch(XMPPMinaClassException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }

            try {
                SocketAddress remoteAddress = classHelper.getRemoteAddress(session);
                if(remoteAddress != null) {
                    context.setVariable("xmpp.outbound.remoteAddress", ((InetSocketAddress)remoteAddress).getHostName());
                    context.setVariable("xmpp.outbound.remotePort", ((InetSocketAddress)remoteAddress).getPort());
                }
            } catch(XMPPMinaClassException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        } catch(XMPPClassHelperNotInitializedException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    protected String getDirection() {
        return "Outbound";
    }
}
