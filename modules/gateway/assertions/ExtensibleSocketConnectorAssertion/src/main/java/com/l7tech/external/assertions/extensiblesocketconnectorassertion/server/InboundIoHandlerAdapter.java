package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.IOUtils;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 01/12/11
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class InboundIoHandlerAdapter extends IoHandlerAdapter {
    private static final Logger logger = Logger.getLogger(InboundIoHandlerAdapter.class.getName());

    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private Goid serviceGoid;

    public InboundIoHandlerAdapter(StashManagerFactory stashManagerFactory, MessageProcessor messageProcessor, Goid serviceGoid) {
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessor = messageProcessor;
        this.serviceGoid = serviceGoid;
    }

    @Override
    public void messageReceived(IoSession session, Object buf) throws Exception {
        PolicyEnforcementContext context = null;

        Message request = new Message();
        Message response = new Message();
        context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        ContentTypeHeader ctype = ContentTypeHeader.OCTET_STREAM_DEFAULT;
        request.initialize(stashManagerFactory.createStashManager(), ctype, new ByteArrayInputStream((byte[]) buf));

        //retrive any ssl information
        AttributeKey sslKey = getKeyFromSession(session, "org.apache.mina.filter.ssl.SslFilter.session");
        if (sslKey != null) {
            SSLSession sslSession = (SSLSession) session.getAttribute(sslKey);

            try {
                Certificate[] clientCerts = sslSession.getPeerCertificates();
                request.attachKnob(TcpKnob.class, new SslSocketTcpKnob((InetSocketAddress) session.getLocalAddress(), (InetSocketAddress) session.getRemoteAddress(), (clientCerts != null && clientCerts.length > 0) ? (X509Certificate) clientCerts[0] : null));
            } catch (SSLPeerUnverifiedException e) {
                request.attachKnob(TcpKnob.class, new SslSocketTcpKnob((InetSocketAddress) session.getLocalAddress(), (InetSocketAddress) session.getRemoteAddress(), null));
            }
        } else {
            //no ssl information retrieve basic session information
            request.attachKnob(TcpKnob.class, new SocketTcpKnob((InetSocketAddress) session.getLocalAddress(), (InetSocketAddress) session.getRemoteAddress()));
        }

        request.attachKnob(HasServiceId.class, new HasServiceIdImpl(serviceGoid));
        AssertionStatus status = messageProcessor.processMessage(context);

        if (status != AssertionStatus.NONE) {
            logger.log(Level.WARNING, "Raw TCP policy failed with assertion status: {0}", status);
        } else if (response.getKnob(MimeKnob.class) != null && response.isInitialized()) {
            byte[] outData = IOUtils.slurpStream(response.getMimeKnob().getEntireMessageBodyAsInputStream());
            session.write(outData);
        }
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        logger.log(Level.WARNING, "Error communicating with client: " + cause);
    }

    /**
     * Retrieve an attribute key from the session by keyName
     *
     * @param session
     * @param keyName
     * @return The attribute key
     */
    private AttributeKey getKeyFromSession(IoSession session, String keyName) {

        Object temp = null;
        AttributeKey key = null;
        boolean keyFound = false;
        Iterator<Object> keySet = session.getAttributeKeys().iterator();

        while (keySet.hasNext() && !keyFound) {
            temp = keySet.next();

            if (temp instanceof AttributeKey) {

                key = (AttributeKey) temp;
                if (key.toString().contains(keyName)) {
                    keyFound = true;
                }
            }
        }

        if (!keyFound) {
            return null;
        } else {
            return key;
        }
    }
}
