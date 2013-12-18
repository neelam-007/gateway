package com.l7tech.server.transport.ftp;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.message.*;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.service.ServiceManager;
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
import java.net.SocketException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private final ServiceManager serviceManager; // TODO jwilliams: maybe remove, maybe use for pre-icefish behaviour

    private final ContentTypeHeader overriddenContentType;
    private final Goid hardwiredServiceGoid;
    private final Goid connectorGoid;
    private final long maxRequestSize;

    public FtpRequestProcessor(final MessageProcessor messageProcessor,
                               final SoapFaultManager soapFaultManager,
                               final StashManagerFactory stashManagerFactory,
                               final EventChannel messageProcessingEventChannel,
                               final ServiceManager serviceManager,
                               final ContentTypeHeader overriddenContentType,
                               final Goid hardwiredServiceGoid,
                               final Goid connectorGoid,
                               final long maxRequestSize) {
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.serviceManager = serviceManager;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceGoid = hardwiredServiceGoid;
        this.connectorGoid = connectorGoid;
        this.maxRequestSize = maxRequestSize;
    }

    public void process(FtpIoSession session, FtpServerContext context,
                        FtpRequest request, FtpMethod method) throws FtpException {
        try {
            String clientIp = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, clientIp);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Processing ''{0}'' with argument ''{1}''.",
                        new Object[] {method.getWspName(), request.getArgument()});
            }

            switch (method.getFtpMethodEnum()) {
                case FTP_APPE:
                case FTP_STOR:
                case FTP_STOU:
                    handleUpload(session, context, request, method);
                    break;

                case FTP_RETR:
                    handleDownload(session, context, request, method);
                    break;

                case FTP_LIST:
                case FTP_MLSD:
                case FTP_NLST:
                    handleListCommand(session, context, request, method);
                    break;

                case FTP_CDUP:
                case FTP_CWD:
                    handleDirectoryNavigation(session, context, request, method);
                    break;

                case FTP_DELE:
                case FTP_NOOP:
                case FTP_MKD:
                case FTP_RMD:
                case FTP_SIZE:
                case FTP_MDTM:
                case FTP_MLST:
                case FTP_PWD:
                    handleSimpleCommand(session, request, method);
                    break;

                default:
                    // should never happen
                    throw new FtpException("Routing of \"" + method.getWspName() + "\" command not supported."); // TODO jwilliams: test - what does the user see?
            }
        } finally {
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
        }
    }

    /**
     * Process a simple command that requires no data connection.
     */
    private void handleUpload(FtpIoSession ftpSession, FtpServerContext ftpServerContext,
                              FtpRequest ftpRequest, FtpMethod ftpMethod) throws FtpException {
        // TODO jwilliams: check for unimplemented checks

        // argument not null or empty check
        if (null == ftpRequest.getArgument() || ftpRequest.getArgument().trim().isEmpty()) {
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, ftpRequest.getCommand(), null));
            return;
        }

        // check connection factory state
        DataConnectionFactory connFactory = ftpSession.getDataConnection();

        if (connFactory instanceof IODataConnectionFactory) {   // TODO jwilliams: consider handling in own separate method to avoid code duplication
            InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();

            if (address == null) {
                reply503BadSequence(ftpSession);
                return;
            }
        }

        // check Data Type is Binary
        if (!ftpSession.getDataType().equals(DataType.BINARY)) {
            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Type '" + ftpSession.getDataType().toString() + "' not supported for this action."));
            return;
        }

        reply150FileStatusOk(ftpSession, ftpServerContext, ftpRequest);

        // open a data connection
        DataConnection dataConnection;

        try {
            dataConnection = ftpSession.getDataConnection().openConnection();
        } catch (Exception e) {
//            LOG.debug("Exception getting the output data stream", e); // TODO jwilliams: handle logging?
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, ftpRequest.getCommand(), null));
            return;
        }

        // upload from client
        InputStream requestInputStream;

        try {
            requestInputStream = getDataInputStream(ftpSession.getFtpletSession(), dataConnection);
        } catch (SocketException e) { // TODO jwilliams: handle logging?
//            LOG.debug("SocketException during file upload", e);
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, ftpRequest.getCommand(), null));
            return;
        } catch (IOException e) { // TODO jwilliams: handle logging?
//            LOG.debug("IOException during file upload", e);
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN, ftpRequest.getCommand(), null));
            return;
        }

        // create request message
        Message request;

        try {
            request = createRequestMessage(ftpSession, ftpRequest, ftpMethod, requestInputStream);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            // TODO jwilliams: write fail reply
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        try {
            // process request message
            AssertionStatus status = processRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + ftpMethod.getWspName() + " message processing completed");
                }
            } else {
                handleFailedAssertion(ftpSession, ftpMethod, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, ftpMethod);
                return;
            }

            // return reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null != ftpResponseKnob) {
                if (FtpReply.REPLY_226_CLOSING_DATA_CONNECTION != ftpResponseKnob.getReplyCode()) {
                    ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(),
                            ftpResponseKnob.getReplyData()));
                } else {
                    ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                            FtpReply.REPLY_226_CLOSING_DATA_CONNECTION,
                            ftpRequest.getCommand(), ftpRequest.getArgument()));
                }
            } else {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                // TODO jwilliams: handle error, logging - a null knob may indicate a real problem
            }
        } finally {
            ftpSession.resetState();
            ftpSession.getDataConnection().closeDataConnection();

            ResourceUtils.closeQuietly(context);
        }
    }

    private void handleDownload(final FtpIoSession ftpSession, final FtpServerContext ftpServerContext,
                                final FtpRequest ftpRequest, final FtpMethod ftpMethod) throws FtpException {
        // argument not null or empty check // TODO jwilliams: check for unimplemented checks
        if (null == ftpRequest.getArgument() || ftpRequest.getArgument().trim().isEmpty()) {
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS, ftpRequest.getCommand(), null));
            return;
        }

        // check connection factory state
        DataConnectionFactory connFactory = ftpSession.getDataConnection();

        if (connFactory instanceof IODataConnectionFactory) {   // TODO jwilliams: consider handling in own separate method to avoid code duplication
            InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();

            if (address == null) {
                reply503BadSequence(ftpSession);
                return;
            }
        }

        Message request;

        // create request message
        try {
            request = createRequestMessage(ftpSession, ftpRequest, ftpMethod, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            // TODO jwilliams: write fail reply
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);


        try {
            // process request message
            AssertionStatus status = processRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + ftpMethod.getWspName() + " message processing completed");
                }
            } else {
                handleFailedAssertion(ftpSession, ftpMethod, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response Message is not initialized");
                reply550ProcessingError(ftpSession, ftpMethod);
                return;
            }

            // get reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null != ftpResponseKnob) { // TODO jwilliams: a null response knob should indicate a real problem, once all the refactoring is done
                // TODO jwilliams: use reply to determine course of action if code >= 400
                System.out.println("SERIOUS PROBLEM - Download FtpResponseKnob null!");
            }

            // get response input stream
            InputStream responseStream = null;  // TODO jwilliams: we should not get to this point if an error code was returned in the response knob

            try {
                logger.log(Level.INFO, "Getting entire");
                responseStream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
            } catch (NoSuchPartException e) {
                e.printStackTrace();
                logger.log(Level.WARNING, "Error processing FTP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                reply550ProcessingError(ftpSession, ftpMethod);
                // TODO jwilliams: handle return
            } catch (IOException e) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error.")); // TODO jwilliams: not data connection error?
                // TODO jwilliams: handle logging & return
            }

            if (null == responseStream) {
                logger.log(Level.WARNING, "Error during processing FTP");
                reply550ProcessingError(ftpSession, ftpMethod);
                // TODO jwilliams: handle
                return;
            }

            reply150FileStatusOk(ftpSession, ftpServerContext, ftpRequest);

            // open a data connection
            DataConnection dataConnection;

            try {
                dataConnection = ftpSession.getDataConnection().openConnection();
            } catch (Exception e) {
                // TODO jwilliams: log exception?
                ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                        FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, ftpRequest.getCommand(), null));
                return;
            }

            // transfer response to client
            try {
                transferDataToClient(ftpSession.getFtpletSession(), dataConnection, responseStream);
            } catch (SocketException e) {
//                LOG.debug("Socket exception during data transfer", e);
                ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                        FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                        ftpRequest.getCommand(), ftpRequest.getArgument()));
                return;
            } catch (IOException e) {
//                LOG.debug("IOException during data transfer", e);
                ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                        FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN, // TODO jwilliams: change to general 400/500 transfer error - error may be from underlying input stream
                        ftpRequest.getCommand(), ftpRequest.getArgument()));
                return;
            }

            // write closing connection message
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, ftpRequest.getCommand(), ftpRequest.getArgument()));
        } finally {
            ftpSession.resetState();
            ftpSession.getDataConnection().closeDataConnection();

            ResourceUtils.closeQuietly(context);
        }
    }

    private void handleListCommand(final FtpIoSession ftpSession, final FtpServerContext ftpServerContext,
                                   final FtpRequest ftpRequest, final FtpMethod ftpMethod) throws FtpException {
        logger.log(Level.INFO, "HANDLING LIST: " + ftpRequest.getCommand() + " " + ftpRequest.getArgument());
        // reset state variables
        ftpSession.resetState();

        // TODO jwilliams: check for unimplemented validation/replies

        DataConnectionFactory connFactory = ftpSession.getDataConnection();

        if (connFactory instanceof IODataConnectionFactory) {   // TODO jwilliams: consider handling in own separate method to avoid code duplication
            InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();

            if (address == null) {
                reply503BadSequence(ftpSession);
                return;
            }
        }

        reply150FileStatusOk(ftpSession, ftpServerContext, ftpRequest);

        // open a data connection
        DataConnection dataConnection;

        try {
            dataConnection = ftpSession.getDataConnection().openConnection();
        } catch (Exception e) {
            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, ftpMethod.getWspName()));
            return;
        }

        Message request;

        // create request message
        try {
            request = createRequestMessage(ftpSession, ftpRequest, ftpMethod, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            // TODO jwilliams: write fail reply
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        try {
            // process request message
            AssertionStatus status = processRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + ftpMethod.getWspName() + " message processing completed");
                }
            } else {
                handleFailedAssertion(ftpSession, ftpMethod, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, ftpMethod);
                return;
            }

            // get reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null != ftpResponseKnob) { // TODO jwilliams: a null response knob should indicate a real problem, once all the refactoring is done
                // TODO jwilliams: use reply to determine course of action if code >= 400 - reply data itself is irrelevant for list-type commands

                if (ftpResponseKnob.getReplyCode() >= 400) { // problem encountered: interpret, report, and don't initiate download // TODO jwilliams: maybe check for !(specific success code)?
                    logger.log(Level.WARNING, "ERROR REPLY CODE RETURNED! " + ftpResponseKnob.getReplyCode());
                    ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(), ftpResponseKnob.getReplyData()));
                    return;
                }
            } else {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                // TODO jwilliams: handle error, logging - a null knob indicates a real problem
            }

            // get response input stream
            InputStream responseStream = null;  // TODO jwilliams: we should not get to this point if an error code was returned in the response knob

            try {
                responseStream = response.getMimeKnob().getEntireMessageBodyAsInputStream();
            } catch (NoSuchPartException e) {
                logger.log(Level.WARNING, "Error processing FTP request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                reply550ProcessingError(ftpSession, ftpMethod);
                // TODO jwilliams: handle return
            } catch (IOException e) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
                // TODO jwilliams: handle logging & return
            }

            if (null == responseStream) {
                logger.log(Level.WARNING, "Error during processing FTP");
                // TODO jwilliams: handle
            }

            // transfer response to client
            try {
                transferDataToClient(ftpSession.getFtpletSession(), dataConnection, responseStream);
            } catch (SocketException e) {
//                LOG.debug("Socket exception during data transfer", e);
                ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                        FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                        ftpRequest.getCommand(), ftpRequest.getArgument()));
                return;
            } catch (IOException e) {
//                LOG.debug("IOException during data transfer", e);
                ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                        FtpReply.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN, // TODO jwilliams: change to general 400/500 transfer error - error may be from underlying input stream
                        ftpRequest.getCommand(), ftpRequest.getArgument()));
                return;
            }

            // write closing connection message
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, ftpRequest.getCommand(), ftpRequest.getArgument()));
        } finally {
            ftpSession.getDataConnection().closeDataConnection();
            ResourceUtils.closeQuietly(context);
        }
    }

    private void handleDirectoryNavigation(final FtpIoSession ftpSession, final FtpServerContext ftpServerContext,
                                           final FtpRequest ftpRequest, final FtpMethod ftpMethod) throws FtpException {
        boolean success = false;

        // reset state variables
        ftpSession.resetState();

        // TODO jwilliams: check for unimplemented validation/replies

        // create request message
        Message request;

        try {
            request = createRequestMessage(ftpSession, ftpRequest, ftpMethod, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            // TODO jwilliams: write fail reply
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        try {
            // process request message
            AssertionStatus status = processRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + ftpMethod.getWspName() + " message processing completed");
                }
            } else {
                handleFailedAssertion(ftpSession, ftpMethod, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, ftpMethod);
                return;
            }

            // get reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null != ftpResponseKnob) {
                if (ftpResponseKnob.getReplyCode() < 400) { // not an error // TODO jwilliams: check for specific return code
                    success = true;
                }
            } else {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                // TODO jwilliams: handle error, logging - a null knob indicates a real problem for a directory nav command
            }
        } finally {
            ResourceUtils.closeQuietly(context);
        }

        // if everything was successful, update our virtual directory and report success
        if (success) {
            FileSystemView fileSystemView = ftpSession.getFileSystemView();

            if (FtpMethod.FTP_CDUP == ftpMethod) {
                fileSystemView.changeWorkingDirectory("..");
            } else if (ftpRequest.hasArgument()) {
                fileSystemView.changeWorkingDirectory(ftpRequest.getArgument());
            } else {
                fileSystemView.changeWorkingDirectory("/");
            }

            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, ftpRequest.getCommand(),
                    fileSystemView.getWorkingDirectory().getAbsolutePath()));
        } else { // TODO jwilliams: report returned error, or generic error?
            ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                    FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, ftpRequest.getCommand(), null));
        }
    }

    /**
     * Process a simple command that requires no data connection.
     */
    private void handleSimpleCommand(FtpIoSession ftpSession, FtpRequest ftpRequest,
                                     FtpMethod ftpMethod) throws FtpException {
        // reset state variables
        ftpSession.resetState();

        // TODO jwilliams: check for unimplemented validation/replies

        // create request message
        Message request;

        try {
            request = createRequestMessage(ftpSession, ftpRequest, ftpMethod, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            // TODO jwilliams: write fail reply
            return;
        }

        // create PEC
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request);

        try {
            // process request message
            AssertionStatus status = processRequestMessage(context);

            if (status == AssertionStatus.NONE) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "FTP " + ftpMethod.getWspName() + " message processing completed");
                }
            } else {
                handleFailedAssertion(ftpSession, ftpMethod, status);
                return;
            }

            // get response message
            Message response = context.getResponse();

            if (response.getKnob(MimeKnob.class) == null || !response.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                reply550ProcessingError(ftpSession, ftpMethod);
                return;
            }

            // return reply code and data
            FtpResponseKnob ftpResponseKnob = response.getKnob(FtpResponseKnob.class);

            if (null != ftpResponseKnob) {
                ftpSession.write(new DefaultFtpReply(ftpResponseKnob.getReplyCode(), ftpResponseKnob.getReplyData()));
            } else {
                logger.log(Level.WARNING, "Error processing FTP request: FtpResponseKnob was not found");
                // TODO jwilliams: handle error, logging - a null knob may indicate a real problem
            }
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

//    /*
//    * Process a file upload
//    */
//    private FtpletResult handleUpload_OLD(FtpIoSession ftpSession, FtpRequest ftpRequest, FtpMethod ftpMethod) throws FtpException, IOException {
//        FtpletResult result = FtpletResult.SKIP;
//
//        String file = ftpRequest.getArgument();
//
//        if (logger.isLoggable(Level.FINE))
//            logger.log(Level.FINE, "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).",
//                    new Object[] {file, FtpMethod.FtpMethodEnum.FTP_STOU == ftpMethod.getFtpMethodEnum()});
//
//        boolean unique = FtpMethod.FtpMethodEnum.FTP_STOU == ftpMethod.getFtpMethodEnum(); // TODO jwilliams: remove - change to use method instead of 'unique'
//
//        if (!ftpSession.getDataType().equals(DataType.BINARY)) {
//            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
//                    "Type '" + ftpSession.getDataType().toString() + "' not supported for this action."));
//        } else {
//            // request data
//            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
//            DataConnectionFactory dataConnectionFactory = null;
//            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
//            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP,
//                    ((InetSocketAddress) ftpSession.getRemoteAddress()).getAddress().getHostAddress());
//
//            try {
//                dataConnectionFactory = ftpSession.getDataConnection();
//                DataConnection dataConnection = null;
//
//                try {
//                    dataConnection = dataConnectionFactory.openConnection();
//                } catch(Exception ex) {
//                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "Can't open data connection."));
//                }
//
//                if (dataConnection != null) {
//                    try {
//                        User user = ftpSession.getUser();
//                        String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
//                        boolean secure = isSecure(ftpSession);
//
//                        if (unique) {
//                            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
//                        }
//
//                        int storeResult = stor(dataConnection, ftpSession, user, path, file, secure, unique);
//
//                        if ( storeResult == RESULT_DROP) {
//                        } else if ( storeResult == RESULT_FAULT) {
//                            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, file + ": Failed"));
//                        } else {
//                            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer complete."));
//                        }
//                    } catch(IOException ioe) {
//                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
//                    }
//                }
//            } finally {
//                HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
//                HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
//                if (dataConnectionFactory != null) dataConnectionFactory.closeDataConnection();
//            }
//        }
//
//        return result;
//    }

//    /*
//     * Store to message processor
//     */
//    private int stor(final DataConnection dataConnection,
//                        final FtpIoSession ftpIoSession,
//                        final User user,
//                        final String path,
//                        final String argument,
//                        final boolean secure,
//                        final boolean unique) throws IOException {
//        int storeResult = RESULT_FAULT;
//
//        FtpSession ftpletSession = ftpIoSession.getFtpletSession();
//
//        if (logger.isLoggable(Level.FINE))
//            logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[] {path, argument});
//
//        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() :maxRequestSize;
//
//        // Create request message
//        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
//
//        Message request = new Message();
//        request.initialize(stashManagerFactory.createStashManager(), cType,
//                getDataInputStream(ftpletSession, dataConnection, buildUri(path, argument)), maxSize);
//
//        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();
//
//        request.attachFtpRequestKnob(buildFtpKnob(
//                serverAddress.getAddress(),
//                serverAddress.getPort(),
//                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
//                null, // TODO jwilliams: placeholder
//                argument,
//                path,
//                secure,
//                unique,
//                user));
//
//        if (!Goid.isDefault(hardwiredServiceGoid)) {
//            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
//        }
//
//        // process request message
//        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, true);
//
//        AssertionStatus status = AssertionStatus.UNDEFINED;
//        String faultXml = null;
//
//        try {
//            try {
//                status = messageProcessor.processMessage(context);
//
//                if (logger.isLoggable(Level.FINER))
//                    logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
//
//            } catch ( PolicyVersionException pve ) {
//                logger.log( Level.INFO, "Request referred to an outdated version of policy" );
//                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
//            } catch ( Throwable t ) {
//                logger.log( Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException( t ) );
//                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
//            }
//
//            if (status != AssertionStatus.NONE) {
//                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
//            }
//
//            if (faultXml != null)
//                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
//
//            if (!context.isStealthResponseMode()) {
//                if (status == AssertionStatus.NONE) {
//                    storeResult = RESULT_OK;
//                }
//            } else {
//                storeResult = RESULT_DROP;
//            }
//        } finally {
//            ResourceUtils.closeQuietly(context);
//        }
//
//        return storeResult;
//    }

    private void handleFailedAssertion(FtpIoSession ftpSession, FtpMethod ftpMethod, AssertionStatus status) {
        logger.log(Level.WARNING,
                "FTP " + ftpMethod.getWspName() + " message processing failed: " + status.getMessage());
        reply550ProcessingError(ftpSession, ftpMethod);
    }

    private void reply150FileStatusOk(FtpIoSession ftpSession, FtpServerContext ftpServerContext, FtpRequest ftpRequest) {
        ftpSession.write(LocalizedFtpReply.translate(ftpSession, ftpRequest, ftpServerContext,
                FtpReply.REPLY_150_FILE_STATUS_OKAY, ftpRequest.getCommand(), null));
    }

    private void reply550ProcessingError(FtpIoSession ftpSession, FtpMethod ftpMethod) {
        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                "Error processing FTP(S) " + ftpMethod.getWspName() + " request."));
    }

    private void reply503BadSequence(FtpIoSession ftpSession) {
        ftpSession.write(new DefaultFtpReply(
                FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                "PORT or PASV must be issued first"));
    }

    private AssertionStatus processRequestMessage(PolicyEnforcementContext context) {
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
            logger.log(Level.WARNING, "PROCESS MESSAGE PROCESSOR: Exception while processing FTP message: " +   // TODO jwilliams: handle in more detail?
                    ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
            t.printStackTrace();
            faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
        }

        if (faultXml != null)
            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

        return status;
    }

    private Message createRequestMessage(final FtpIoSession ftpIoSession, final FtpRequest ftpRequest,
                                         final FtpMethod ftpMethod, final InputStream inputStream)
            throws FtpException, IOException {
        User user = ftpIoSession.getUser();
        String argument = ftpRequest.getArgument();
        String path = ftpIoSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();

        boolean secure = isSecure(ftpIoSession);

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        // Create request message
        ContentTypeHeader cType =
                overriddenContentType != null
                        ? overriddenContentType
                        : ContentTypeHeader.OCTET_STREAM_DEFAULT; // TODO jwilliams: shouldn't have an overridden content type option?

        Message request = new Message();

        request.initialize(stashManagerFactory.createStashManager(), cType, inputStream, maxSize);

        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();

        request.attachFtpRequestKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
                ftpMethod,
                argument,
                path,
                secure,
                user));

        if (!Goid.isDefault(hardwiredServiceGoid)) { // TODO jwilliams: check this is working - either need to spit bics or accommodate no hardwired service (i.e. pre-icefish behaviour)
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        return request;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(Message request) { // TODO jwilliams: add preservable FtpResponseKnob facets to Response message?
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message(), true);
    }

//    /*
//    * Convert OutputStream to InputStream.
//    */
//    private InputStream readRequestDataFromClient(final FtpIoSession ftpSession,
//                                                  final DataConnection dataConnection,
//                                                  final String fullPath) throws IOException {
//        final PipedInputStream pis = new PipedInputStream();
//        final CountDownLatch startedSignal = new CountDownLatch(1);
//
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                PipedOutputStream pos = null;
//
//                try {
//                    if (logger.isLoggable(Level.FINE))
//                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", fullPath);
//
//                    //noinspection IOResourceOpenedButNotSafelyClosed
//                    pos = new PipedOutputStream(pis);
//                    startedSignal.countDown();
//                    dataConnection.transferFromClient(ftpSession.getFtpletSession(), pos);
//
//                    if (logger.isLoggable(Level.FINE))
//                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", fullPath);
//                } catch (IOException ioe) {
//                    logger.log(Level.WARNING, "Data transfer error for '" + fullPath + "'.", ioe);
//                } finally {
//                    ResourceUtils.closeQuietly(pos);
//                    startedSignal.countDown();
//                }
//            }
//        }, "FtpServer-DataTransferThread-" + System.currentTimeMillis());
//
//        thread.setDaemon(true);
//        thread.start();
//
//        try {
//            startedSignal.await();
//        } catch(InterruptedException ie) {
//            Thread.currentThread().interrupt();
//            throw new CausedIOException("Interrupted waiting for data.", ie);
//        }
//
//        return pis;
//    }

//    /**
//     * Process commands involving transfer of data.
//     */
//    public FtpletResult handleTransportStart(FtpIoSession ftpSession, FtpRequest ftpRequest, boolean unique, FtpMethod ftpMethod) throws FtpException {
//        FtpletResult result = FtpletResult.SKIP;
//
//        String argument = ftpRequest.getArgument();
//
//        if (logger.isLoggable(Level.FINE)) {
//            logger.log(Level.FINE,
//                    "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).",
//                    new Object[] {argument, unique});
//        }
//
//        DataConnectionFactory dataConnectionFactory = null;
//
//        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
//        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP,
//                ((InetSocketAddress) ftpSession.getRemoteAddress()).getAddress().getHostAddress());
//
//        try {
//            dataConnectionFactory = ftpSession.getDataConnection();
//            DataConnection dataConnection = null;
//
//            try {
//                dataConnection = dataConnectionFactory.openConnection();
//            } catch(Exception ex) {
//                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "Can't open data connection."));
//            }
//
//            if (dataConnection != null) {
//                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
//
//                // transfer data
//                try {
//                    User user = ftpSession.getUser();
//                    String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
//
//                    if (initServiceUri != null && path.equals("/")) {
//                        ftpSession.getFileSystemView().changeWorkingDirectory(initServiceUri);
//                        path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
//                    }
//
//                    String directory = ((VirtualFileSystem) ftpSession.getFileSystemView()).getChangedDirectory();
//
//                    boolean secure = isSecure(ftpSession);
//
//                    if (unique) {
//                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, argument + ": Transfer started."));
//                    }
//
//                    int storeResult = onStore(dataConnection, ftpSession, user, path, argument, secure, unique, ftpMethod, directory);
//
//                    if (storeResult == RESULT_DROP) {   // TODO jwilliams: this may be a bug introduced with the changes to stealth mode response code - RESULT_DROP doesn't get set anywhere
//                        result = FtpletResult.DISCONNECT;
//                    } else if (storeResult == RESULT_FAULT) {
//                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, argument + ": " + "Failed."));
//                    } else {
//                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer complete."));
//                    }
//                } catch(IOException ioe) {
//                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
//                }
//            }
//        } finally {
//            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
//            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
//
//            if (dataConnectionFactory != null) {
//                dataConnectionFactory.closeDataConnection();
//            }
//        }
//
//        return result;
//    }

//    /*
//    * Process commands that don't involve data transfer.
//    */
//    public FtpletResult handleCommandStart(FtpIoSession ftpIoSession, FtpRequest ftpRequest, FtpMethod ftpMethod) throws FtpException, IOException {
//        System.out.println("USING DEFAULT HANDLER FOR: " + ftpRequest.getCommand() + " " + ftpRequest.getArgument());
//
//        FtpletResult result = FtpletResult.SKIP;
//
//        User user = ftpIoSession.getUser();
//        String argument = ftpRequest.getArgument();
//
//        if (logger.isLoggable(Level.FINE))
//            logger.log(Level.FINE, "Handling " + ftpMethod.getWspName() +
//                    " with argument ''{0}''.", new Object[] {argument});
//
//        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
//        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress().getHostAddress());
//
//        if (initServiceUri != null) {
//            ftpIoSession.getFileSystemView().changeWorkingDirectory(initServiceUri); // TODO jwilliams: why CWD for every command?
//        }
//
//        VirtualFileSystem vfs = (VirtualFileSystem) ftpIoSession.getFileSystemView();
//
//        String path = vfs.getWorkingDirectory().getAbsolutePath();
//        String previousDirectory = vfs.getChangedDirectory(); // TODO jwilliams: store CWD in case the policy fails and we need to revert to it
//
//        String directory;
//
//        if (ftpMethod == FtpMethod.FTP_CWD) {
//            String uri;
//
//            if (argument.startsWith("/")) {
//                uri = argument;
//            } else {
//                uri = "/" + argument;
//            }
//
//            if (isService(uri)) {
//                argument = uri; // TODO jwilliams: this has to be a bug - if we had a service named "/example" then a CWD request for any subdir named "example" would change dir to the service!
//                ftpIoSession.getFileSystemView().changeWorkingDirectory(argument);
//                path = ftpIoSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
//                directory = "/";
//            } else if (argument.startsWith("/")) {
//                directory = argument;
//                vfs.setChangedDirectory(argument);
//            } else if (argument.equals("..") || argument.equals("../")) {
//                directory = vfs.getParentDirectory();
//                vfs.setChangedDirectory(directory);
//            } else if (argument.equals(".")) {
//                directory = vfs.getChangedDirectory();
//            } else {
//                directory = vfs.getChangedDirectory();
//                vfs.setCombinedChangedDirectory(argument);
//            }
//        } else if (ftpMethod == FtpMethod.FTP_CDUP) {
//            directory = vfs.getParentDirectory();   // TODO jwilliams: directory only used to generate response
//            vfs.setChangedDirectory(directory);
//        } else {
//            directory = vfs.getChangedDirectory();
//        }
//
//        boolean secure = isSecureSession(ftpIoSession);
//
//        int storeResult = process(ftpIoSession, user, path, argument, secure, false, ftpMethod.getWspName(), directory); // TODO jwilliams: 'false' is placeholder, unnecessary
//
//        if (storeResult == RESULT_DROP) { // TODO jwilliams: unreliable - routing could succeed but the policy fail
//            result = FtpletResult.DISCONNECT;
//        } else if (storeResult == RESULT_FAULT) {
//            if (ftpMethod == FtpMethod.FTP_CWD) { // TODO jwilliams: should this reversion apply to CDUP as well? could there be an odd permissions corner case?
//                vfs.setChangedDirectory(previousDirectory); // TODO jwilliams: if the attempted operation was a CD and the assertion fails, revert VFS working directory
//            }
//        }
//
//        HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
//        HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
//
//        return result;
//    }

    /*
    * Process a file upload
    */
//    private FtpletEnum handleUploadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, boolean unique) throws FtpException, IOException {
//        FtpletEnum result = FtpletEnum.RET_SKIP;
//        String fileName = ftpRequest.getArgument();
//
//        if (logger.isLoggable(Level.FINE))
//            logger.log(Level.FINE, "Handling STOR for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});
//
//        if (!ftpServerManager.isLicensed()) {
//            if (logger.isLoggable(Level.INFO))
//                logger.log(Level.INFO, "Failing STOR (FTP server not licensed).");
//
//            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
//                    "Service not available (not licensed)."));
//        }
//        else {
//            if (!ftpSession.getDataType().equals(DataType.BINARY)) {
//                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
//                        "Type '"+ftpSession.getDataType().toString()+"' not supported for this action."));
//            }
//            else {
//                // request data
//                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
//                DataConnectionFactory dataConnectionFactory = null;
//                HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
//                HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getHostAddress());
//                try {
//                    dataConnectionFactory = ftpSession.getDataConnection();
//                    DataConnection dataConnection = null;
//                    try {
//                        dataConnection = dataConnectionFactory.openConnection();
//                    }
//                    catch(Exception ex) {
//                        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "Can't open data connection."));
//                    }
//
//                    if (dataConnection != null) {
//                        try {
//                            String[] message = {"Failed."};
//                            User user = ftpSession.getUser();
//                            String file = ftpRequest.getArgument();
//                            String path = ftpSession.getFileSystemView().getCurrentDirectory().getFullName();
//                            boolean secure = isSecure(dataConnectionFactory, ftpSession);
//
//                            if (unique) {
//                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
//                            }
//
//                            int storeResult = onStore(dataConnection, ftpSession, user, path, file, secure, unique);
//
//                            if (storeResult == RESULT_DROP) {
//                                result = FtpletEnum.RET_DISCONNECT;
//                            } else if (storeResult == RESULT_FAULT) {
//                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, fileName + ": " + message[0]));
//                            } else {
//                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer complete."));
//                            }
//                        }
//                        catch(IOException ioe) {
//                            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
//                        }
//                    }
//                }
//                finally {
//                    HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
//                    HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
//                    if (dataConnectionFactory !=null) dataConnectionFactory.closeDataConnection();
//                }
//            }
//        }
//
//        return result;
//    }

    /**
     * Check if the session is secure.
     *
     * <p>To be secure both the control and data connections must be secured.</p>
     *
     * <p>NOTE: This will NOT WORK for explicit FTP, which is currently fine
     * since that is not enabled.</p>
     * @param ftpIoSession ftp session to check whether control connection is secure
     * @return whether the connection is secure
     */
    private boolean isSecure(FtpIoSession ftpIoSession) {
        boolean dataSecure = ftpIoSession.getDataConnection().isSecure();
        boolean controlSecure = ftpIoSession.getListener().isImplicitSsl();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Security levels, control secure ''{0}'', data secure ''{1}''.",
                    new Object[] {controlSecure, dataSecure});

        return dataSecure && controlSecure;
    }

//    /**
//     * Check if the session is secure.
//     *
//     * <p>To be secure for delete control must be secured.</p>
//     *
//     * <p>NOTE: This will NOT WORK for explicit FTP, which is currently fine
//     * since that is not enabled.</p>
//     * @param ftpIoSession ftp session to check whether control connection is secure
//     * @return true if the control connection is secure
//     */
//    private boolean isSecureSession(FtpIoSession ftpIoSession) {
//        boolean controlSecure = ftpIoSession.getListener().isImplicitSsl();
//
//        if (logger.isLoggable(Level.FINE))
//            logger.log(Level.FINE, "Security levels, control secure ''{0}'', data secure ''{1}''.",
//                    new Object[] {controlSecure});
//
//        return controlSecure;
//    }

//    /*
//     * Store to message processor
//     */
//    private int onStore(final DataConnection dataConnection,
//                        final FtpIoSession ftpIoSession,
//                        final User user,
//                        final String path,
//                        final String file,
//                        final boolean secure,
//                        final boolean unique,
//                        final FtpMethod ftpMethod,
//                        final String directory) throws IOException {
//        int storeResult = RESULT_FAULT;
//
//        FtpSession ftpletSession = ftpIoSession.getFtpletSession();
//
//        if (logger.isLoggable(Level.FINE))
//            logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[] {path, file});
//
//        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;
//
//        ContentTypeHeader ctype = overriddenContentType != null
//                ? overriddenContentType
//                : ContentTypeHeader.XML_DEFAULT;
//
//        // Create request message
//        Message request = new Message();
//
//        if (ftpMethod == FtpMethod.FTP_STOR || ftpMethod == FtpMethod.FTP_APPE) {
//            request.initialize(stashManagerFactory.createStashManager(), ctype,
//                    getDataInputStream(ftpletSession, dataConnection, buildUri(path, file)), maxSize);
//        } else {
//            request.initialize(stashManagerFactory.createStashManager(), ctype,
//                    new ByteArrayInputStream(new byte[0]), maxSize);
//        }
//
//        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();
//
//        request.attachFtpRequestKnob(buildFtpKnob(
//                serverAddress.getAddress(),
//                serverAddress.getPort(),
//                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
//                ftpMethod.getWspName(),
//                file,
//                path,
//                secure,
//                unique,
//                user));
//
//        if (!Goid.isDefault(hardwiredServiceGoid)) {
//            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
//        }
//
//        // Create response message
//        Message response = new Message();
//
//        // process request message
//        final PolicyEnforcementContext context =
//                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
//
//        AssertionStatus status = AssertionStatus.UNDEFINED;
//        String faultXml = null;
//
//        //set the context variable to check the ftp method
//        context.setVariable("ftp.command", ftpMethod.getWspName());
//        context.setVariable("ftp.directory", directory);
//
//        try {
//            try {
//                status = messageProcessor.processMessage(context);
//
//                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
//            } catch (PolicyVersionException pve) {
//                logger.log(Level.INFO, "Request referred to an outdated version of policy");
//                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
//            } catch (Throwable t) {
//                logger.log(Level.WARNING, "MESSAGE PROCESSOR: Exception while processing FTP message: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
//                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
//            }
//
//            if (status != AssertionStatus.NONE) {
//                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
//            }
//
//            if (faultXml != null) {
//                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
//            }
//
//            if (!context.isStealthResponseMode() && status == AssertionStatus.NONE) {
//                storeResult = RESULT_OK;
//
//                if (ftpMethod != FtpMethod.FTP_RETR && !containListCommand(ftpMethod)) {
//                    logger.log(Level.INFO, "FTP " + ftpMethod.getWspName() + " message processing completed");
//                } else {
//                    Message responseContext = context.getResponse();
//
//                    if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
//                        try {
//                            InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();
//
//                            if (responseStream != null) {
//                                if (ftpMethod == FtpMethod.FTP_RETR) {
//                                    if (status == AssertionStatus.NONE) {
//                                        long readLength = transferDataToClient(ftpletSession, dataConnection, responseStream);
//
//                                        if (readLength < 0) {
//                                            logger.log(Level.WARNING, "Error during reading the file");
//                                            storeResult = RESULT_FAULT;
//                                        }
//                                    } else {
//                                        logger.log(Level.WARNING, "Error during reading the file");
//                                        storeResult = RESULT_FAULT;
//                                    }
//                                } else if (containListCommand(ftpMethod)) {
//                                    String responseMessage = FtpListUtil.writeMessageToOutput(responseStream);
//                                    if (!responseMessage.isEmpty()) {
//                                        String rawMessage = responseMessage;
//
//                                        if (ftpMethod != FtpMethod.FTP_NLST) {
//                                            Document doc = FtpListUtil.createDoc(responseMessage);
//                                            if (doc != null) {
//                                                rawMessage = FtpListUtil.getRawListData(doc);
//                                            }
//                                        }
//
//                                        boolean success = listFiles(ftpIoSession, dataConnection, rawMessage);
//
//                                        if (!success) {
//                                            logger.log(Level.WARNING, "Error during list FTP");
//                                            storeResult = RESULT_FAULT;
//                                        }
//                                    }
//                                }
//                            } else {
//                                logger.log(Level.WARNING, "Error during processing FTP");
//                                storeResult = RESULT_FAULT;
//                            }
//                        } catch (NoSuchPartException nsp) {
//                            logger.log(Level.WARNING, "ON STORE GET RESPONSE MESSAGE BODY: Exception while processing FTP message: "+ ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp));
//                            storeResult = RESULT_FAULT;
//                        }
//                    } else  {
//                        logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
//                        storeResult = RESULT_FAULT;
//                    }
//                }
//            } else {
//                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
//                storeResult = RESULT_FAULT;
//            }
//
//// TODO jwilliams: old code replaced by above chunk, keeping here until question of the unset 'DROP' result is resolved
////            if (!context.isStealthResponseMode()) {
////                if (status == AssertionStatus.NONE) {
////                    storeResult = RESULT_OK;
////                }
////            } else {
////                storeResult = RESULT_DROP;
////            }
//        } finally {
//            ResourceUtils.closeQuietly(context);
//        }
//
//        return storeResult;
//    }

//    /*
//     *  Process commands that don't require Data Transaction
//     */
//    private int process(final FtpIoSession ftpIoSession,
//                        final User user,
//                        final String path,
//                        final String file,
//                        final boolean secure,
//                        final boolean unique,
//                        final String command,
//                        final String directory) throws IOException, FtpException {
//        int storeResult = RESULT_FAULT;
//
//        if (logger.isLoggable(Level.FINE)) {
//            logger.log(Level.FINE, "Processing " + command + " for path ''{0}'' and file ''{1}''.",
//                    new String[] {path, file});
//        }
//
//        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;
//
//        // Create request message
//        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
//        Message request = new Message();
//
//        request.initialize(stashManagerFactory.createStashManager(), cType, new ByteArrayInputStream(new byte[0]), maxSize);
//
//        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();
//
//        request.attachFtpRequestKnob(buildFtpKnob(
//                serverAddress.getAddress(),
//                serverAddress.getPort(),
//                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
//                command,
//                file,
//                path,
//                secure,
//                unique,
//                user));
//
//        if (!Goid.isDefault(hardwiredServiceGoid)) {
//            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
//        }
//
//        // Create response message
//        Message response = new Message();
//
//        // process request message
//        final PolicyEnforcementContext context =
//                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
//
//        AssertionStatus status = AssertionStatus.UNDEFINED;
//        String faultXml = null;
//
//        try {
//            try {
//                status = messageProcessor.processMessage(context);
//
//                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
//            } catch (PolicyVersionException pve) {
//                logger.log(Level.INFO, "Request referred to an outdated version of policy");
//                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
//            } catch (Throwable t) {
//                logger.log(Level.WARNING, "PROCESS MESSAGE PROCESSOR: Exception while processing FTP message: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
//                t.printStackTrace();
//                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
//            }
//
//            if (status != AssertionStatus.NONE) {
//                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
//            }
//
//            if (faultXml != null)
//                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
//
//            if (!context.isStealthResponseMode()) {
//                String responseMessage = null;
//
//                if (status == AssertionStatus.NONE) {
//                    storeResult = RESULT_OK;
//                    logger.log(Level.INFO, "FTP " + command + " request processing completed.");
//                } else {
//                    storeResult = RESULT_FAULT;
//                }
//
//                Message responseContext = context.getResponse();
//
//                if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
//                    try {
//                        InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();
//
//                        if (responseStream != null) {
//                            responseMessage = FtpListUtil.writeMessageToOutput(responseStream);
//                            createReplyOutput(command, ftpIoSession, responseMessage, directory, storeResult);
//                        }
//                    } catch (NoSuchPartException nsp) {
//                        logger.log(Level.WARNING, "Exception while processing FTP message: " + ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp));
//                        storeResult = RESULT_FAULT;
//                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
//                    }
//                } else if (!command.equals("APPE") && !command.equals("DELE")) {
//                    logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
//                    storeResult = RESULT_FAULT;
//                    ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
//                } else {
//                    if (status == AssertionStatus.NONE) {
//                        createReplyOutput(command, ftpIoSession, responseMessage, directory, storeResult);
//                    } else {
//                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
//                    }
//                }
//            } else {
//                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
//                storeResult = RESULT_FAULT;
//                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
//            }
//        } finally {
//            ResourceUtils.closeQuietly(context);
//        }
//
//        return storeResult;
//    }

//    private String buildUri(String path, String file) { // TODO jwilliams: is this necessary?
//        String uri = path;
//
//        if (!uri.endsWith("/")) {
//            uri += "/";
//        }
//
//        uri += file;
//
//        return uri;
//    }

//    private void createReplyOutput(final String command, final FtpIoSession ftpIoSession, final String responseMessage, String directory, int storeResult) throws IOException, FtpException {
//        if (directory != null && !directory.startsWith("/")) {
//            directory = "/" + directory;
//        }
//
//        if (storeResult == RESULT_OK && !command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
//            if (command.equals(FtpMethod.FTP_APPE.getWspName())) {
//                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer OK"));
//            } else if (command.equals(FtpMethod.FTP_CDUP.getWspName())) {
//                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "CDUP successful. \"" + directory + "\" is current directory."));
//            } else if (command.equals(FtpMethod.FTP_PWD.getWspName())) {
//                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_257_PATHNAME_CREATED, "\"" + directory + "\" is current directory."));
//            } else if (command.equals(FtpMethod.FTP_MDTM.getWspName())) {
//                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_213_FILE_STATUS, org.apache.ftpserver.util.DateUtils.getFtpDate(Long.parseLong(responseMessage))));
//            } else if (command.equals(FtpMethod.FTP_SIZE.getWspName())) {
//                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
//            } else {
//                if (responseMessage != null && !responseMessage.isEmpty()) {
//                    int result = createReplyMessage(ftpIoSession, responseMessage);
//
//                    if (result == -1) {
//                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
//                    }
//                } else {
//                    ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "OK"));
//                }
//            }
//        } else {
//            if (command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
//                createReplyMessage(ftpIoSession, responseMessage);
//            } else {
//                if (responseMessage != null && !responseMessage.isEmpty()) {
//                    int result = createReplyMessage(ftpIoSession, responseMessage);
//
//                    if (result == -1) {
//                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, responseMessage));
//                    }
//                } else {
//                    ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Requested action not taken"));
//                }
//            }
//        }
//    }

//    private int createReplyMessage(final FtpIoSession ftpIoSession, final String responseMessage) throws IOException, FtpException {
//        try {
//            int replyCode = Integer.parseInt(responseMessage.substring(0, 3));
//            String replyMessage = responseMessage.substring(4, responseMessage.length());
//
//            if (replyMessage.endsWith("\r\n")) {
//                replyMessage = replyMessage.substring(0, replyMessage.indexOf("\r\n")); // TODO jwilliams: shouldn't this be lastIndexOf? in that case, just use the reply length minus return chars
//            }
//
//            ftpIoSession.write(new DefaultFtpReply(replyCode, replyMessage));
//        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
//            return -1;
//        }
//
//        return 0;
//    }

//    private boolean isService(String uri) {
//        boolean isService = false;
//        int countService = 0;
//
//        try {
//            Collection<PublishedService> publishedServices = serviceManager.findByRoutingUri(uri);
//
//            for (PublishedService publishedService : publishedServices) {
//                if (uri.equals(publishedService.getRoutingUri()) && !publishedService.isDisabled()) {
//                    countService++;
//                }
//            }
//        } catch (FindException fe) {
//            return false;
//        }
//
//        if (countService == 0) {
//            logger.log(Level.WARNING, "There is no service with uri " + uri);
//        } else if (countService == 1) {
//            isService = true;
//        } else {
//            logger.log(Level.WARNING, "There is more then one enabled service with the uri " + uri);
//        }
//
//        return isService;
//    }

    /*
     * Create an FtpKnob for the given info.
     */
    private FtpRequestKnob buildFtpKnob(final InetAddress serverAddress, final int port,
                                        final InetAddress clientAddress, final FtpMethod command, final String argument,
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
                return command.getWspName();
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
                return command.getFtpMethodEnum() == FtpMethod.FtpMethodEnum.FTP_STOU;
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
     * Convert OutputStream to InputStream.
     */
    private InputStream getDataInputStream(final FtpSession ftpSession,
                                           final DataConnection dataConnection) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                PipedOutputStream pos = null;

                try {
//                    if (logger.isLoggable(Level.FINE))
//                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", fullPath);

                    //noinspection IOResourceOpenedButNotSafelyClosed
                    pos = new PipedOutputStream(pis);
                    startedSignal.countDown();
                    dataConnection.transferFromClient(ftpSession, pos);

//                    if (logger.isLoggable(Level.FINE))
//                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", fullPath);
                } catch (IOException ioe) {
//                    logger.log(Level.WARNING, "Data transfer error for '"+fullPath+"'.", ioe);
                    logger.log(Level.WARNING, "Data transfer error.", ioe);
                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
            }
        }, "FtpServer-DataTransferThread-" + System.currentTimeMillis());

        thread.setDaemon(true);
        thread.start();

        try {
            startedSignal.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for data.", ie);
        }

        return pis;
    }

//    private boolean listFiles(final FtpIoSession ftpIoSession, final DataConnection dataConnection, String dirList) {
//        boolean success = false;
//
//        try {
//            dataConnection.transferToClient(ftpIoSession.getFtpletSession(), dirList);
//            success = true;
//        } catch (SocketException ex) {
//            logger.log(Level.WARNING, "Socket exception during list transfer", ex);
//        } catch (IOException ex) {
//            logger.log(Level.WARNING, "IOException during list transfer", ex);
//        } catch (IllegalArgumentException e) {
//            logger.log(Level.WARNING, "Illegal list syntax: ", e);
//        }
//
//        return success;
//    }

    /**
     * Read the data from InputStream and transfer to client.
     */
    private void transferDataToClient(final FtpSession ftpSession,
                                      final DataConnection dataConnection,
                                      final InputStream is) throws IOException {
        try {
            logger.log(Level.INFO, "Starting data transfer to client"); // TODO jwilliams: change back to FINE

            dataConnection.transferToClient(ftpSession, is);

            logger.log(Level.INFO, "Completed data transfer to client");
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

//    private boolean containListCommand(FtpMethod ftpMethod) {
//        String[] listCommands = new String[] { // TODO jwilliams: handle better in the refactored FtpMethod/FtpMethodEnum
//                FtpMethod.FTP_LIST.getWspName(),
//                FtpMethod.FTP_MLSD.getWspName(),
//                FtpMethod.FTP_NLST.getWspName()
//        };
//
//        return Arrays.asList(listCommands).contains(ftpMethod.getWspName());
//    }
}
