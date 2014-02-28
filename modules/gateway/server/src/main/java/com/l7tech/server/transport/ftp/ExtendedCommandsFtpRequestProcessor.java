package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.FtpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.ftp.FtpCommand.CDUP;

/**
 * Implementation of request processing support for a large number of FTP commands.
 *
 * This processor enables proxying scenarios by handling the most commonly used FTP features
 * and providing meaningful feedback to the FTP client of command results. This implementation
 * is designed to work with a service that uses the FTP Routing Assertion.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class ExtendedCommandsFtpRequestProcessor extends AbstractFtpRequestProcessor {
    private static final Logger logger = Logger.getLogger(ExtendedCommandsFtpRequestProcessor.class.getName());

    public ExtendedCommandsFtpRequestProcessor(final MessageProcessor messageProcessor,
                                               final SoapFaultManager soapFaultManager,
                                               final StashManagerFactory stashManagerFactory,
                                               final EventChannel messageProcessingEventChannel,
                                               final ExecutorService transferTaskExecutor,
                                               final ContentTypeHeader overriddenContentType,
                                               final Goid hardwiredServiceGoid,
                                               final Goid connectorGoid,
                                               final long maxRequestSize) {
        super(messageProcessor, soapFaultManager, stashManagerFactory,
                messageProcessingEventChannel, transferTaskExecutor, overriddenContentType,
                hardwiredServiceGoid, connectorGoid, maxRequestSize, logger);
    }

    public void process(FtpIoSession session, FtpServerContext context,
                        FtpRequest request, FtpCommand command) throws FtpException {
        try {
            addDiagnosticContextInfo(session);

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
            removeDiagnosticContextInfo();
        }
    }

    /**
     * Process an upload command (APPE, STOR, STOU).
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

            // process request message
            try {
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

    @Override
    protected ContentTypeHeader getDefaultMessageContentType() {
        return ContentTypeHeader.OCTET_STREAM_DEFAULT;
    }
}