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
 * Date: 08/03/12
 * Time: 4:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class InboundMessageHandler extends MessageHandlerBase {
    private static final Logger logger = Logger.getLogger(InboundMessageHandler.class.getName());

    public InboundMessageHandler(XMPPConnectionEntity entity,
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

            context.setVariable("xmpp.inbound.sessionID", Long.toString(sessionId));

            try {
                SocketAddress localAddress = classHelper.getLocalAddress(session);
                if(localAddress != null) {
                    context.setVariable("xmpp.inbound.localAddress", ((InetSocketAddress)localAddress).getHostName());
                    context.setVariable("xmpp.inbound.localPort", ((InetSocketAddress)localAddress).getPort());
                }
            } catch(XMPPMinaClassException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }

            try {
                SocketAddress remoteAddress = classHelper.getRemoteAddress(session);
                if(remoteAddress != null) {
                    context.setVariable("xmpp.inbound.remoteAddress", ((InetSocketAddress)remoteAddress).getHostName());
                    context.setVariable("xmpp.inbound.remotePort", ((InetSocketAddress)remoteAddress).getPort());
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
        return "Inbound";
    }

    @Override
    public void exceptionCaught(Object session, Throwable cause) throws Exception {
        logger.log(Level.WARNING, cause.getMessage(), cause);
    }
}
