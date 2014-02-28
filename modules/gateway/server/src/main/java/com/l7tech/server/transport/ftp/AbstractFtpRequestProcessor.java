package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.HasServiceId;
import com.l7tech.message.HasServiceIdImpl;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
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
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ResourceUtils;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedFtpReply;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.ftp.FtpCommand.STOU;

/**
 * Helper for custom FTP Commands, FtpRequestKnob composition, and MessageProcessor preparation.
 *
 * This class' core functionality was extracted from {@link SsgFtplet} (formerly named MessageProcessingFtplet).
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public abstract class AbstractFtpRequestProcessor implements FtpRequestProcessor {
    private final Logger logger;

    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ExecutorService transferTaskExecutor;
    private final ContentTypeHeader overriddenContentType;
    private final Goid hardwiredServiceGoid;
    private final Goid connectorGoid;
    private final long maxRequestSize;

    public AbstractFtpRequestProcessor(final MessageProcessor messageProcessor,
                                       final SoapFaultManager soapFaultManager,
                                       final StashManagerFactory stashManagerFactory,
                                       final EventChannel messageProcessingEventChannel,
                                       final ExecutorService transferTaskExecutor,
                                       final ContentTypeHeader overriddenContentType,
                                       final Goid hardwiredServiceGoid,
                                       final Goid connectorGoid,
                                       final long maxRequestSize,
                                       final Logger logger) {
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.transferTaskExecutor = transferTaskExecutor;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceGoid = hardwiredServiceGoid;
        this.connectorGoid = connectorGoid;
        this.maxRequestSize = maxRequestSize;
        this.logger = logger;
    }

    protected void addDiagnosticContextInfo(FtpIoSession session) {
        String clientIp = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, clientIp);
    }

    protected void removeDiagnosticContextInfo() {
        HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
        HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
    }

    protected boolean validateConnectionFactoryState(final FtpIoSession ftpSession) {
        boolean valid = true;

        DataConnectionFactory connFactory = ftpSession.getDataConnection();

        if (connFactory instanceof IODataConnectionFactory) {
            InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();

            if (address == null) {
                valid = false;
            }
        }

        return valid;
    }

    protected void resetStateAndCloseConnection(FtpIoSession ftpSession) {
        // reset state variables
        ftpSession.resetState();

        // and abort any data connection
        ftpSession.getDataConnection().closeDataConnection();
    }

    protected void handleCreateRequestMessageFailure(FtpIoSession ftpSession, FtpCommand command, IOException e) {
        logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                ExceptionUtils.getDebugException(e));
        reply550ProcessingError(ftpSession, command);
    }

    protected void handleMessageProcessingFailure(FtpIoSession ftpSession, FtpCommand command, AssertionStatus status) {
        logger.log(Level.WARNING,
                "FTP " + command + " message processing failed: " + status.getMessage());
        reply550ProcessingError(ftpSession, command);
    }

    protected void handleOpenConnectionFailure(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                             FtpRequest ftpRequest, Exception e) {
        logger.log(Level.WARNING, "Failed to open a data connection: " + ExceptionUtils.getMessage(e),
                ExceptionUtils.getDebugException(e));
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    protected void handleDataTransferFailure(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                           FtpRequest ftpRequest, Exception e) {
        logger.log(Level.WARNING, "Error during data transfer: " + e.getMessage(),
                ExceptionUtils.getDebugException(e));
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    protected void reply150FileStatusOk(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                      FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_150_FILE_STATUS_OKAY,
                ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    protected void reply226ClosingConnection(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                           FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    protected void reply501SyntaxError(FtpIoSession ftpSession, FtpServerContext ftpServerContext, FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                ftpRequest.getCommand(), null));
    }

    protected void reply503BadSequence(FtpIoSession ftpSession) {
        ftpSession.write(new DefaultFtpReply(
                FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                "PORT or PASV must be issued first"));
    }

    protected void reply550ProcessingError(FtpIoSession ftpSession, FtpCommand command) {
        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                "Error processing FTP(S) " + command + " request."));
    }

    protected AssertionStatus submitRequestMessage(PolicyEnforcementContext context) {
        String faultXml = null;
        AssertionStatus status = AssertionStatus.UNDEFINED;

        try {
            status = messageProcessor.processMessage(context);

            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
            }

            if (status != AssertionStatus.NONE) {
                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
            }
        } catch (PolicyVersionException pve) {
            logger.log(Level.INFO, "Request referred to an outdated version of policy.");
            faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Exception while processing FTP message: " +
                    ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
            faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
        }

        if (faultXml != null)
            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

        return status;
    }

    protected Message createRequestMessage(final FtpIoSession ftpSession, final FtpRequest ftpRequest,
                                           final FtpCommand command, final InputStream inputStream)
            throws FtpException, IOException {
        return createRequestMessage(ftpSession, ftpRequest, command, STOU == command, inputStream);
    }

    protected Message createRequestMessage(final FtpIoSession ftpSession, final FtpRequest ftpRequest,
                                           final FtpCommand command, boolean unique, final InputStream inputStream)
            throws FtpException, IOException {
        User user = ftpSession.getUser();
        String argument = ftpRequest.getArgument();
        String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();

        boolean secure = isSessionSecure(ftpSession);

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        ContentTypeHeader cType =
                overriddenContentType != null
                        ? overriddenContentType
                        : getDefaultMessageContentType();

        // Create request message
        Message request = new Message();

        request.initialize(stashManagerFactory.createStashManager(), cType, inputStream, maxSize);

        InetSocketAddress serverAddress = (InetSocketAddress) ftpSession.getLocalAddress();

        request.attachFtpRequestKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpSession.getRemoteAddress()).getAddress(),
                command,
                argument,
                path,
                secure,
                unique,
                user));

        // ensure resolution to a hardwired service, if specified
        if (null != hardwiredServiceGoid) {
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        return request;
    }

    protected abstract ContentTypeHeader getDefaultMessageContentType();

    protected PolicyEnforcementContext createPolicyEnforcementContext(Message request) {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message(), true);
    }

    /**
     * Check if the session is secure.
     *
     * <p>To be secure both the control and data connections must be secured.</p>
     *
     * <p>NOTE: This will NOT WORK for explicit FTP, which is currently fine
     * since that is not enabled.</p>
     * @param ftpSession ftp session to check whether control connection is secure
     * @return whether the connection is secure
     */
    private boolean isSessionSecure(FtpIoSession ftpSession) {
        boolean dataSecure = ftpSession.getDataConnection().isSecure();
        boolean controlSecure = ftpSession.getListener().isImplicitSsl();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Security levels, control secure ''{0}'', data secure ''{1}''.",
                    new Object[] {controlSecure, dataSecure});

        return dataSecure && controlSecure;
    }

    /*
     * Create an FtpKnob for the given info.
     */
    protected FtpRequestKnob buildFtpKnob(final InetAddress serverAddress, final int port,
                                        final InetAddress clientAddress, final FtpCommand command,
                                        final String argument, final String path, final boolean secure,
                                        final boolean unique, final User user) {
        return new FtpRequestKnob() {
            @Override
            public int getLocalPort() {
                return getLocalListenerPort();
            }

            @Override
            public int getLocalListenerPort() {
                return port;
            }

            @Override
            public String getRemoteAddress() {
                return clientAddress.getHostAddress();
            }
            @Override
            public String getRemoteHost() {
                return clientAddress.getHostAddress();
            }

            @Override
            public int getRemotePort() {
                return 0;
            }

            @Override
            public String getLocalAddress() {
                return serverAddress.getHostAddress();
            }

            @Override
            public String getLocalHost() {
                return serverAddress.getHostAddress();
            }

            @Override
            public String getCommand() {
                return command.toString();
            }

            @Override
            public String getArgument() {
                return argument;
            }

            @Override
            public String getPath() {
                return path;
            }

            @Override
            public String getRequestUri() {
                return path;
            }

            @Override
            public String getRequestUrl() {
                StringBuilder urlBuilder = new StringBuilder();

                urlBuilder.append(secure ? "ftps" : "ftp");
                urlBuilder.append("://");
                urlBuilder.append(InetAddressUtil.getHostForUrl(serverAddress.getHostAddress()));
                urlBuilder.append(":");
                urlBuilder.append(port);
                urlBuilder.append(path);
                if (!path.endsWith("/"))
                    urlBuilder.append("/");
                urlBuilder.append(argument);

                return urlBuilder.toString();
            }

            @Override
            public boolean isSecure() {
                return secure;
            }

            @Override
            public boolean isUnique() {
                return unique;
            }

            @Override
            public PasswordAuthentication getCredentials() {
                PasswordAuthentication passwordAuthentication = null;

                if (user.getPassword() != null) {
                    passwordAuthentication =
                            new PasswordAuthentication(user.getName(), user.getPassword().toCharArray());
                }

                return passwordAuthentication;
            }
        };
    }

    /*
     * Begin transferring data from the client and return an InputStream for the data.
     */
    protected InputStream transferDataFromClient(final FtpSession ftpSession, final DataConnection dataConnection,
                                               final String argument) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);

        transferTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                PipedOutputStream pos = null;

                try {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", argument);

                    //noinspection IOResourceOpenedButNotSafelyClosed
                    pos = new PipedOutputStream(pis);
                    startedSignal.countDown();
                    dataConnection.transferFromClient(ftpSession, pos);

                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", argument);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Data transfer error for '" +
                            argument + "'.", ExceptionUtils.getDebugException(e));
                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
            }
        });

        try {
            startedSignal.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for data.", ie);
        }

        return pis;
    }

    /**
     * Read the data from InputStream and transfer to client.
     */
    protected void transferDataToClient(final FtpSession ftpSession,
                                      final DataConnection dataConnection,
                                      final InputStream is) throws IOException {
        try {
            dataConnection.transferToClient(ftpSession, is);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    /**
     * Shut down the transfer task thread pool executor.
     */
    @Override
    public void dispose() {
        if (transferTaskExecutor != null) {
            transferTaskExecutor.shutdown();

            try {
                transferTaskExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.log(Level.FINE, "Thread pool shutdown interrupted: " + e.getMessage());
            }
        }
    }
}