package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.server.xmlstreamcodec.XMPPMinaClassException;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 13/03/12
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class MessageHandlerBase {
    private static final Logger logger = Logger.getLogger(MessageHandlerBase.class.getName());

    private final XMPPConnectionEntity config;
    private final StashManagerFactory stashManagerFactory;
    private final MessageProcessor messageProcessor;

    private final SessionStartedCallback sessionStartedCallback;
    private final SessionTerminatedCallback sessionTerminatedCallback;

    protected final XMPPClassHelper classHelper;

    private final Object ioHandler;

    public MessageHandlerBase(XMPPConnectionEntity entity,
                              StashManagerFactory stashManagerFactory,
                              MessageProcessor messageProcessor,
                              SessionStartedCallback sessionStartedCallback,
                              SessionTerminatedCallback sessionTerminatedCallback,
                              XMPPClassHelper classHelper)
    throws XMPPClassHelperNotInitializedException, XMPPMinaClassException
    {
        this.config = entity;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessor = messageProcessor;
        this.sessionStartedCallback = sessionStartedCallback;
        this.sessionTerminatedCallback = sessionTerminatedCallback;
        this.classHelper = classHelper;

        ioHandler = classHelper.createXmppIoHandlerAdapter(this);
    }

    public Object getIoHandler() {
        return ioHandler;
    }

    public void messageReceived(final Object session, Object message) throws Exception {
        byte[] bytesIn;
        if(message instanceof byte[]) {
            bytesIn = (byte[])message;
        } else if(message instanceof String) {
            bytesIn = ((String)message).getBytes();
        } else {
            return;
        }

        logger.log(Level.FINE, "XMPP received message: " + new String(bytesIn));

        processMessage(session, config.getMessageReceivedServiceOid(), bytesIn);
    }
    
    private void processMessage(final Object session, Goid serviceGoid, byte[] message) {
        PolicyEnforcementContext context = null;
        try {
            Message request = new Message();
            Message response = new Message();
            context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            String ctypeStr = config.getContentType();

            ContentTypeHeader ctype = ctypeStr == null ? ContentTypeHeader.OCTET_STREAM_DEFAULT : ContentTypeHeader.create(ctypeStr);
            request.initialize(stashManagerFactory.createStashManager(), ctype, new ByteArrayInputStream(message));

            InetSocketAddress localAddress = (InetSocketAddress)classHelper.getLocalAddress(session);
            if(localAddress == null) {
                localAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5222);
            }
            InetSocketAddress remoteAddress = (InetSocketAddress)classHelper.getRemoteAddress(session);
            if(remoteAddress == null) {
                remoteAddress = new InetSocketAddress(InetAddress.getLocalHost(), 5222);
            }

            request.attachKnob(TcpKnob.class, new SocketTcpKnob(localAddress, remoteAddress, session));

            setContextVariables(context, session);

            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));

            synchronized(session) {
                AssertionStatus status = messageProcessor.processMessage(context);

                if (status != AssertionStatus.NONE) {
                    // Send fault
                    logger.log(Level.WARNING, "XMPP " + getDirection() + " policy failed with assertion status: {0}", status);
                    // TODO customize response to send upon error?
                    //return null;
                } else if (response.getKnob(MimeKnob.class) != null && response.isInitialized()) {
                    byte[] responseBytes = IOUtils.slurpStream(response.getMimeKnob().getEntireMessageBodyAsInputStream());
                    classHelper.ioSessionWrite(session, responseBytes);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "I/O error handling raw TCP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (Exception e) {
            // TODO send response of some kind?  customize response to send upon error?
            logger.log(Level.SEVERE, "Unexpected error handling raw TCP request: " + ExceptionUtils.getMessage(e), e);
        } finally {
            if (context != null)
                ResourceUtils.closeQuietly(context);
        }
    }

    protected abstract void setContextVariables(PolicyEnforcementContext context, Object session);

    public void sessionOpened(Object session) throws Exception {
        logger.log(Level.FINE, "Opening XMPP connection.");
        sessionStartedCallback.sessionStarted(session);
    }

    public void sessionClosed(Object session) throws Exception {
        logger.log(Level.FINE, "Closing XMPP connection.");
        sessionTerminatedCallback.sessionTerminated(session);

        if(config.getSessionTerminatedServiceOid() != null) {
            processMessage(session, config.getSessionTerminatedServiceOid(), new byte[0]);
        }
    }
    
    protected abstract String getDirection();
    
    public void exceptionCaught(Object session, Throwable cause) throws Exception {
        logger.log(Level.WARNING, "Exception caught in message handler.", cause);
    }
}
