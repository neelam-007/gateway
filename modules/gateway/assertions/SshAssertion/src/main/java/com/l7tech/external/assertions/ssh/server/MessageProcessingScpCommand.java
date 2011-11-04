package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.log.HybridDiagnosticContext;
import static com.l7tech.external.assertions.ssh.server.MessageProcessingSshUtil.buildPolicyExecutionContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TruncatingInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.command.ScpCommand;
import org.apache.sshd.server.session.ServerSession;

import java.io.*;
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
        throw new IOException("Unsupported mode");
    }

    @Override
    protected void writeFile(String header, SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Writing file {0}", path);
        }
        if (!header.startsWith("C")) {
            throw new IOException("Expected a C message but got '" + header + "'");
        }

        final long length;
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
            final InputStream fileInput = length <= 0L ?
                    new EmptyInputStream() :
                    new TruncatingInputStream(in, length);
            ack();

            sendFileToMessageProcessor( connector, absolutePath, name, fileInput );
            ack();
            readAck(false);
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
        }
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
        throw new IOException("Unsupported mode");
    }

    @Override
    protected void writeExit(IOException e, int exitValue, String exitMessage) {
        try {
            exitValue = ERROR;
            exitMessage = e.getMessage();

            // intercept any messages that may reveal which files are present on the Gateway
            if (!StringUtils.isEmpty(exitMessage) &&
                    (exitMessage.contains("java.lang.IllegalStateException: basedir") && exitMessage.contains("does not exist"))) {
                exitMessage = "Unsupported mode";
            }

            out.write(exitValue);
            out.write(exitMessage.getBytes());
            out.write( (int) '\n' );
            out.flush();
        } catch (IOException e2) {
            // Ignore
        }
        log.info("Error in scp command", e);
    }

    private boolean sendFileToMessageProcessor( final SsgConnector connector,
                                                final String path,
                                                final String file,
                                                final InputStream inputStream ) throws IOException {
        boolean success = false;

        final PolicyEnforcementContext context =
                buildPolicyExecutionContext( connector, session, stashManagerFactory, inputStream, file, path );

        try {
            String faultXml = null;
            AssertionStatus status = AssertionStatus.UNDEFINED;
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
        }

        return success;
    }
}
