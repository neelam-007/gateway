package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasServiceOidImpl;
import com.l7tech.message.Message;
import com.l7tech.message.SshKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.command.ScpCommand;
import org.apache.sshd.server.session.ServerSession;

import java.io.*;
import java.net.PasswordAuthentication;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing for SCP support (heavily borrowed from org.apache.sshd.server.command.ScpCommand).
 */
public class MessageProcessingScpCommand extends ScpCommand implements SessionAware {
    protected static final Logger logger = Logger.getLogger(MessageProcessingScpCommand.class.getName());

    private ServerSession session;
    private SsgConnector connector;
    private MessageProcessor messageProcessor;
    private EventChannel messageProcessingEventChannel;
    private SoapFaultManager soapFaultManager;
    private StashManagerFactory stashManagerFactory;

    public MessageProcessingScpCommand(String[] args, SsgConnector c, MessageProcessor mp, StashManagerFactory smf,
                                       SoapFaultManager sfm, EventChannel mpec) {
        super(args);
        connector = c;
        messageProcessor = mp;
        messageProcessingEventChannel = mpec;
        soapFaultManager = sfm;
        stashManagerFactory = smf;
    }

    @Override
    public void setSession(ServerSession session) {
        this.session = session;
    }

    @Override
    protected void writeDir(String header, SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Recursively writing dir {0} unsupported", path);
        }
        throw new IOException("Recursive directory write unsupported.");
    }

    @Override
    protected void writeFile(String header, SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Writing file {0}", path);
        }
        if (!header.startsWith("C")) {
            throw new IOException("Expected a C message but got '" + header + "'");
        }

        long length = 0;
        try {
            length = Long.parseLong(header.substring(6, header.indexOf(' ', 6)));
        } catch (NumberFormatException nfe) {
            throw new CausedIOException("Error parsing header.", nfe);
        }

        String name = header.substring(header.indexOf(' ', 6) + 1);

        // if required, remove filename from the absolute path (e.g. jscape scp client appends the file name to the path)
        String absolutePath = path.getAbsolutePath();
        if (!StringUtils.isEmpty(absolutePath)) {
            int index = absolutePath.lastIndexOf("/" + name);
            if (index >= 0) {
                absolutePath = absolutePath.substring(0, index);
            }
        }

        HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, Long.toString( connector.getOid() ) );
        HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.CLIENT_IP, MessageProcessingSshUtil.getRemoteAddress(session) );
        try {
            pipeInputStreamToGatewayRequestMessage(connector, absolutePath, name, in, length);
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
        }

        readAck(false);
    }

    @Override
    protected void readFile(SshFile path) throws IOException {
        throw new IOException("Copy from server currently unsupported.");
    }

    @Override
    protected void readDir(SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Recursively reading directory {0} unsupported", path);
        }
        throw new IOException("Recursive directory read unsupported.");
    }

    private boolean pipeInputStreamToGatewayRequestMessage( final SsgConnector connector,
                                                            final String path,
                                                            final String file,
                                                            final InputStream inputStream,
                                                            final long length ) throws IOException {
        boolean success = false;
        Message request = new Message();

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, true);
        final long requestSizeLimit = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, Message.getMaxBytes());
        final InputStream pis = getDataInputStream(inputStream, path, length);

        String ctypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        ContentTypeHeader ctype = ctypeStr == null ? ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.create(ctypeStr);

        request.initialize(stashManagerFactory.createStashManager(), ctype, pis, requestSizeLimit);

        // attach ssh knob
        String userName = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_USERNAME);
        String userPublicKey = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_PUBLIC_KEY);
        if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPublicKey)) {
            request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                    session.getIoSession().getRemoteAddress(), file, path, new SshKnob.PublicKeyAuthentication(userName, userPublicKey)));
        } else {
            userName = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_USERNAME);
            String userPassword = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_PASSWORD);
            if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPassword)) {
                request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file, path, new PasswordAuthentication(userName, userPassword.toCharArray())));
            } else {
                request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file, path, null, null));
            }
        }

        long hardwiredServiceOid = connector.getLongProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, -1L );
        if (hardwiredServiceOid != -1L ) {
            request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
        }

            AssertionStatus status = AssertionStatus.UNDEFINED;
            String faultXml = null;
            try {
                try {
                    status = messageProcessor.processMessage(context);

                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);

                } catch ( PolicyVersionException pve ) {
                    logger.log( Level.INFO, "Request referred to an outdated version of policy" );
                    faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
                } catch ( Throwable t ) {
                    logger.log( Level.WARNING, "Exception while processing SCP message. " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                    faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                }

                if ( status != AssertionStatus.NONE ) {
                    faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                } else {
                    success = true;
                }

                if (faultXml != null) {
                    messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                }
            } finally {
                ResourceUtils.closeQuietly(context);
                ResourceUtils.closeQuietly(pis);
            }
        return success;
    }

    /*
     * Convert OutputStream to InputStream.
     */
    private InputStream getDataInputStream(final InputStream inputStream,
                                           final String fullPath, final long length) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                PipedOutputStream pos = null;
                try {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", fullPath);

                    pos = new PipedOutputStream(pis);
                    startedSignal.countDown();
                    transferFromClient(inputStream, pos, length);

                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", fullPath);
                }
                catch (IOException ioe) {
                    logger.log(Level.WARNING, "Data transfer error for '" + fullPath + "'.", ExceptionUtils.getDebugException(ioe));
                }
                finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
            }
        }, "ScpServer-DataTransferThread-" + System.currentTimeMillis());

        thread.setDaemon(true);
        thread.start();

        try {
            startedSignal.await();
        }
        catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for data.", ie);
        }

        return pis;
    }

    private void transferFromClient(InputStream inputStream, OutputStream outputStream, long length) throws IOException {
        try {
            ack();

            byte[] buffer = new byte[8192];
            while (length > 0L ) {
                int len = (int) Math.min(length, (long) buffer.length );
                len = inputStream.read(buffer, 0, len);
                if (len <= 0) {
                    throw new IOException("End of stream reached");
                }
                outputStream.write(buffer, 0, len);
                length -= (long) len;
            }
        } finally {
            outputStream.close();
        }

        ack();
    }
}
