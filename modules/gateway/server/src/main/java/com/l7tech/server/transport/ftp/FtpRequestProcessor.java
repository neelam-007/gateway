package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.message.*;
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
import com.l7tech.util.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;
import org.apache.ftpserver.impl.IODataConnectionFactory;
import org.apache.ftpserver.impl.LocalizedFtpReply;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.ftp.FtpCommand.*;

/**
 * Helper for custom FTP Commands, FtpRequestKnob composition, and MessageProcessor preparation.
 *
 * Most of this class' functionality was extracted from {@link SsgFtplet} (formerly named MessageProcessingFtplet).
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpRequestProcessor {
    private static final Logger logger = Logger.getLogger(FtpRequestProcessor.class.getName());

    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ExecutorService transferTaskExecutor;
    private final ContentTypeHeader overriddenContentType;
    private final Goid hardwiredServiceGoid;
    private final Goid connectorGoid;
    private final long maxRequestSize;

    public FtpRequestProcessor(final MessageProcessor messageProcessor,
                               final SoapFaultManager soapFaultManager,
                               final StashManagerFactory stashManagerFactory,
                               final EventChannel messageProcessingEventChannel,
                               final ExecutorService transferTaskExecutor,
                               final ContentTypeHeader overriddenContentType,
                               final Goid hardwiredServiceGoid,
                               final Goid connectorGoid,
                               final long maxRequestSize) {
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.transferTaskExecutor = transferTaskExecutor;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceGoid = hardwiredServiceGoid;
        this.connectorGoid = connectorGoid;
        this.maxRequestSize = maxRequestSize;
    }

    public void process(FtpIoSession session, FtpServerContext context,
                        FtpRequest request, FtpCommand command) throws FtpException {
        try {
            String clientIp = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, clientIp);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Processing ''{0}'' with argument ''{1}''.",
                        new Object[] {command, request.getArgument()});
            }

            // validate a command argument is present if one is required
            if (command.isArgumentRequired() &&
                    (null == request.getArgument() || request.getArgument().trim().isEmpty())) {
                reply501SyntaxError(session, context, request);
                return;
            }

            switch (command) {
                case APPE:
                case STOR:
                case STOU:
                    processUpload(session, context, request, command);
                    break;

                case RETR:
                    processDownload(session, context, request, command);
                    break;

                case LIST:
                case MLSD:
                case NLST:
                    processListCommand(session, context, request, command);
                    break;

                case CDUP:
                case CWD:
                    processDirectoryNavigation(session, request, command);
                    break;

                case DELE:
                case NOOP:
                case MKD:
                case RMD:
                case SIZE:
                case MDTM:
                case MLST:
                case PWD:
                    processSimpleCommand(session, request, command);
                    break;

                default:
                    // should never happen
                    throw new FtpException("Routing of \"" + command + "\" command not supported.");
            }
        } finally {
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
        }
    }

    /**
     * Process a simple command that requires no data connection.
     */
    private void processUpload(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                               FtpRequest ftpRequest, FtpCommand command) throws FtpException {
        // check connection factory state
        if (!validateConnectionFactoryState(ftpSession)) {
            reply503BadSequence(ftpSession);
            return;
        }

        // check Data Type is Binary
        if (!ftpSession.getDataType().equals(DataType.BINARY)) {
            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Type '" + ftpSession.getDataType().toString() + "' not supported for this action."));
            return;
        }

        reply150FileStatusOk(ftpSession, ftpServerContext, ftpRequest);

        try {
            // open a data connection
            DataConnection dataConnection;

            try {
                dataConnection = ftpSession.getDataConnection().openConnection();
            } catch (Exception e) {
                ftpSession.resetState();
                handleOpenConnectionFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // upload from client
            InputStream requestInputStream;

            try {
                requestInputStream = transferDataFromClient(ftpSession.getFtpletSession(),
                        dataConnection, ftpRequest.getArgument());
            } catch (RejectedExecutionException e) {
                logger.log(Level.WARNING, "Failed to transfer data from client: " + e.getMessage());
                ftpSession.write(new DefaultFtpReply(
                        FtpReply.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION,
                        "Max connections exceeded."));
                return;
            } catch (Exception e) {
                handleDataTransferFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // create request message
            Message request;

            try {
                request = createRequestMessage(ftpSession, ftpRequest, command, requestInputStream);
            } catch (IOException e) {
                handleCreateRequestMessageFailure(ftpSession, command, e);
                return;
            }

            // create PEC
            final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

            try {
                // process request message
                AssertionStatus status = submitRequestMessage(context);

                if (status == AssertionStatus.NONE) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "FTP " + command + " message processing completed");
                    }
                } else {
                    handleMessageProcessingFailure(ftpSession, command, status);
                    return;
                }

                // get response message
                Message response = context.getResponse();

                if (!response.isInitialized()) {
                    logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                    reply550ProcessingError(ftpSession, command);
                    return;
                }

                // return reply code and data
                FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

                if (null == ftpResponseKnob) {
                    logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                    reply550ProcessingError(ftpSession, command);
                    return;
                }

                if (FtpReply.REPLY_226_CLOSING_DATA_CONNECTION != ftpResponseKnob.getReplyCode()) {
                    logger.log(Level.WARNING,
                            "Unexpected completion reply code returned: " + ftpResponseKnob.getReplyCode());
                    ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(),
                            ftpResponseKnob.getReplyText()));
                } else {
                    // write closing connection message
                    reply226ClosingConnection(ftpSession, ftpServerContext, ftpRequest);
                }
            } finally {
                ResourceUtils.closeQuietly(context);
            }
        } finally {
            resetStateAndCloseConnection(ftpSession);
        }
    }

    private void processDownload(final FtpIoSession ftpSession, final FtpServerContext ftpServerContext,
                                 final FtpRequest ftpRequest, final FtpCommand command) throws FtpException {
        // check connection factory state
        if (!validateConnectionFactoryState(ftpSession)) {
            reply503BadSequence(ftpSession);
            return;
        }

        Message request;

        // create request message
        try {
            request = createRequestMessage(ftpSession, ftpRequest, command, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            handleCreateRequestMessageFailure(ftpSession, command, e);
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        InputStream responseStream = null;

        try {
            // process request message
            AssertionStatus status = submitRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + command + " message processing completed");
                }
            } else {
                handleMessageProcessingFailure(ftpSession, command, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response Message is not initialized");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // get reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null == ftpResponseKnob) {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            if (FtpReply.REPLY_150_FILE_STATUS_OKAY != ftpResponseKnob.getReplyCode()) {
                logger.log(Level.WARNING,
                        "Unexpected completion reply code returned: " + ftpResponseKnob.getReplyCode());
                ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(), ftpResponseKnob.getReplyText()));
                return;
            }

            // get response input stream, destroying it as it's read
            try {
                logger.log(Level.INFO, "Getting response message input stream");
                responseStream = response.getMimeKnob().getEntireMessageBodyAsInputStream(true);
            } catch (NoSuchPartException | IOException e) {
                logger.log(Level.WARNING, "Error processing FTP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // indicate all ok, ready to start transfer
            reply150FileStatusOk(ftpSession, ftpServerContext, ftpRequest);

            // open a data connection
            DataConnection dataConnection;

            try {
                dataConnection = ftpSession.getDataConnection().openConnection();
            } catch (Exception e) {
                handleOpenConnectionFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // transfer response to client
            try {
                transferDataToClient(ftpSession.getFtpletSession(), dataConnection, responseStream);
            } catch (Exception e) {
                handleDataTransferFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // write closing connection message
            reply226ClosingConnection(ftpSession, ftpServerContext, ftpRequest);
        } finally {
            resetStateAndCloseConnection(ftpSession);

            ResourceUtils.closeQuietly(responseStream);
            ResourceUtils.closeQuietly(context);
        }
    }

    private boolean validateConnectionFactoryState(final FtpIoSession ftpSession) {
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

    private void processListCommand(final FtpIoSession ftpSession, final FtpServerContext ftpServerContext,
                                    final FtpRequest ftpRequest, final FtpCommand command) throws FtpException {
        // reset state variables
        ftpSession.resetState();

        // check connection factory state
        if (!validateConnectionFactoryState(ftpSession)) {
            reply503BadSequence(ftpSession);
            return;
        }

        reply150FileStatusOk(ftpSession, ftpServerContext, ftpRequest);

        // open a data connection
        DataConnection dataConnection;

        try {
            dataConnection = ftpSession.getDataConnection().openConnection();
        } catch (Exception e) {
            handleOpenConnectionFailure(ftpSession, ftpServerContext, ftpRequest, e);
            return;
        }

        Message request;

        // create request message
        try {
            request = createRequestMessage(ftpSession, ftpRequest, command, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            handleCreateRequestMessageFailure(ftpSession, command, e);
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        InputStream responseStream = null;

        try {
            // process request message
            AssertionStatus status = submitRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + command + " message processing completed");
                }
            } else {
                handleMessageProcessingFailure(ftpSession, command, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // get reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null == ftpResponseKnob) {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            if (FtpReply.REPLY_226_CLOSING_DATA_CONNECTION != ftpResponseKnob.getReplyCode()) {
                logger.log(Level.WARNING,
                        "Unexpected completion reply code returned: " + ftpResponseKnob.getReplyCode());
                ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(), ftpResponseKnob.getReplyText()));
                return;
            }

            // get response input stream
            try {
                responseStream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
            } catch (NoSuchPartException | IOException e) {
                logger.log(Level.WARNING, "Error processing FTP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // transfer response to client
            try {
                transferDataToClient(ftpSession.getFtpletSession(), dataConnection, responseStream);
            } catch (Exception e) {
                handleDataTransferFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // write closing connection message
            reply226ClosingConnection(ftpSession, ftpServerContext, ftpRequest);
        } finally {
            resetStateAndCloseConnection(ftpSession);

            ResourceUtils.closeQuietly(responseStream);
            ResourceUtils.closeQuietly(context);
        }
    }

    private void resetStateAndCloseConnection(FtpIoSession ftpSession) {
        // reset state variables
        ftpSession.resetState();

        // and abort any data connection
        ftpSession.getDataConnection().closeDataConnection();
    }

    private void processDirectoryNavigation(final FtpIoSession ftpSession, final FtpRequest ftpRequest,
                                            final FtpCommand command) throws FtpException {
        // reset state variables
        ftpSession.resetState();

        // create request message
        Message request;

        try {
            request = createRequestMessage(ftpSession, ftpRequest, command, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            handleCreateRequestMessageFailure(ftpSession, command, e);
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        try {
            // process request message
            AssertionStatus status = submitRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + command + " message processing completed");
                }
            } else {
                handleMessageProcessingFailure(ftpSession, command, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (!response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // get reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            // a null knob indicates a real problem for a directory nav command
            if (null == ftpResponseKnob) {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // if everything was successful, update our virtual directory and report success
            if (ftpResponseKnob.getReplyCode() >= 200 && ftpResponseKnob.getReplyCode() < 300) {
                FileSystemView fileSystemView = ftpSession.getFileSystemView();

                if (CDUP == command) {
                    fileSystemView.changeWorkingDirectory("..");
                } else {
                    fileSystemView.changeWorkingDirectory(ftpRequest.getArgument());
                }
            } else {
                logger.log(Level.WARNING, "Directory not changed: unexpected reply code returned: " + ftpResponseKnob.getReplyCode());
            }

            ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(), ftpResponseKnob.getReplyText()));
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    /**
     * Process a simple command that requires no data connection.
     */
    private void processSimpleCommand(FtpIoSession ftpSession, FtpRequest ftpRequest,
                                      FtpCommand command) throws FtpException {
        // reset state variables
        ftpSession.resetState();

        // create request message
        Message request;

        try {
            request = createRequestMessage(ftpSession, ftpRequest, command, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            handleCreateRequestMessageFailure(ftpSession, command, e);
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        try {
            // process request message
            AssertionStatus status = submitRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + command + " message processing completed");
                }
            } else {
                handleMessageProcessingFailure(ftpSession, command, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (!response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            // return reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null == ftpResponseKnob) {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                reply550ProcessingError(ftpSession, command);
                return;
            }

            ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(), ftpResponseKnob.getReplyText()));
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    private void handleCreateRequestMessageFailure(FtpIoSession ftpSession, FtpCommand command, IOException e) {
        logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                ExceptionUtils.getDebugException(e));
        reply550ProcessingError(ftpSession, command);
    }

    private void handleMessageProcessingFailure(FtpIoSession ftpSession, FtpCommand command, AssertionStatus status) {
        logger.log(Level.WARNING,
                "FTP " + command + " message processing failed: " + status.getMessage());
        reply550ProcessingError(ftpSession, command);
    }

    private void handleOpenConnectionFailure(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                             FtpRequest ftpRequest, Exception e) {
        logger.log(Level.WARNING, "Failed to open a data connection: " + ExceptionUtils.getMessage(e),
                ExceptionUtils.getDebugException(e));
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    private void handleDataTransferFailure(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                           FtpRequest ftpRequest, Exception e) {
        logger.log(Level.WARNING, "Error during data transfer: " + e.getMessage(),
                ExceptionUtils.getDebugException(e));
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    private void reply150FileStatusOk(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                      FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_150_FILE_STATUS_OKAY,
                ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    private void reply226ClosingConnection(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                                           FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                ftpRequest.getCommand(), ftpRequest.getArgument()));
    }

    private void reply501SyntaxError(FtpIoSession ftpSession, FtpServerContext ftpServerContext, FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS,
                ftpRequest.getCommand(), null));
    }

    private void reply503BadSequence(FtpIoSession ftpSession) {
        ftpSession.write(new DefaultFtpReply(
                FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                "PORT or PASV must be issued first"));
    }

    private void reply550ProcessingError(FtpIoSession ftpSession, FtpCommand command) {
        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                "Error processing FTP(S) " + command + " request."));
    }

    private AssertionStatus submitRequestMessage(PolicyEnforcementContext context) {
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

    private Message createRequestMessage(final FtpIoSession ftpSession, final FtpRequest ftpRequest,
                                         final FtpCommand command, final InputStream inputStream)
            throws FtpException, IOException {
        User user = ftpSession.getUser();
        String argument = ftpRequest.getArgument();
        String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();

        boolean secure = isSessionSecure(ftpSession);

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        // Create request message
        ContentTypeHeader cType =
                overriddenContentType != null
                        ? overriddenContentType
                        : ContentTypeHeader.OCTET_STREAM_DEFAULT;

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
                user));

        // to proxy FTP requests the listener must be hardwired to a service
        if (!Goid.isDefault(hardwiredServiceGoid)) {
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        } else {
            logger.log(Level.WARNING, "Couldn't route request: no service specified for listener.");
            throw new FtpException("Service not available.");
        }

        return request;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(Message request) {
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
    private FtpRequestKnob buildFtpKnob(final InetAddress serverAddress, final int port,
                                        final InetAddress clientAddress, final FtpCommand command, final String argument,
                                        final String path, final boolean secure, final User user) {
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
                return command == STOU;
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
    private InputStream transferDataFromClient(final FtpSession ftpSession, final DataConnection dataConnection,
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
                    logger.log(Level.WARNING, "Data transfer error.", ExceptionUtils.getDebugException(e));
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
    private void transferDataToClient(final FtpSession ftpSession,
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