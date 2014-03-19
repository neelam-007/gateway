package com.l7tech.server.transport.ftp;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.ResourceUtils;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.FtpServerContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of limited request processing support for STOR/STOU commands.
 *
 * Most of this class' functionality was extracted from {@link com.l7tech.server.transport.ftp.SsgFtplet}
 * (formerly named MessageProcessingFtplet). The code was changed significantly to accommodate the upgrade
 * of the Apache FtpServer library, but the behaviour had been maintained.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class UploadOnlyFtpRequestProcessor extends AbstractFtpRequestProcessor {
    private static final Logger logger = Logger.getLogger(UploadOnlyFtpRequestProcessor.class.getName());

    public UploadOnlyFtpRequestProcessor(final MessageProcessor messageProcessor,
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
                logger.log(Level.FINE, "Handling STOR for file ''{0}'' (unique:{1}).",
                        new Object[] {command, request.getArgument()});
            }

            // validate a command argument is present if one is required
            if (command.isArgumentRequired() &&
                    (null == request.getArgument() || request.getArgument().trim().isEmpty())) {
                reply501SyntaxError(session, context, request);
                return;
            }

            switch (command) {
                case STOR:
                case STOU:
                    processUpload(session, context, request, command);
                    break;

                default:
                    // should never happen
                    throw new FtpException("Routing of \"" + command + "\" command not supported.");
            }
        } finally {
            removeDiagnosticContextInfo();
        }
    }

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

        String file = ftpRequest.getArgument();

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

            if (FtpCommand.STOU == command) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY,
                        file + ": Transfer started."));
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.",
                        new String[]{ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath(),
                                ftpRequest.getArgument()});
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
            } catch (IOException e) {
                handleDataTransferFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // create request message
            Message request;

            try {
                // if the command is STOU, set the 'unique' flag in the FtpRequestKnob to true
                // execute a STOR command regardless to preserve old behaviour
                request = createRequestMessage(ftpSession, ftpRequest,
                        FtpCommand.STOR, FtpCommand.STOU == command, requestInputStream);
            } catch (IOException e) {
                // this is preserved behaviour, but the response is not an accurate description
                // of the problem - using handleCreateRequestMessageFailure() would be better
                handleDataTransferFailure(ftpSession, ftpServerContext, ftpRequest, e);
                return;
            }

            // create PEC
            final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

            // process request message
            try {
                AssertionStatus status = submitRequestMessage(context);

                if (context.isStealthResponseMode()) {
                    // stealth response mode set, disconnecting the client
                    ftpSession.close(false).awaitUninterruptibly(10000);
                } else if (status == AssertionStatus.NONE) {
                    reply226ClosingConnection(ftpSession, ftpServerContext, ftpRequest);
                } else {
                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                            file + ": Failed"));
                }
            } finally {
                ResourceUtils.closeQuietly(context);
            }
        } finally {
            resetStateAndCloseConnection(ftpSession);
        }
    }

    @Override
    protected ContentTypeHeader getDefaultMessageContentType() {
        return ContentTypeHeader.XML_DEFAULT;
    }
}