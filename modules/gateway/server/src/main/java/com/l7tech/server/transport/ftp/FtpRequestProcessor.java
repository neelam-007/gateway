package com.l7tech.server.transport.ftp;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
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
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.ResourceUtils;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.ServerDataConnectionFactory;
import org.w3c.dom.Document;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper for custom FTP Commands, FtpRequestKnob composition, and MessageProcessor preparation.
 *
 * Most of this class' functionality was extracted from {@link SsgFtplet}.
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpRequestProcessor {
    private static final Logger logger = Logger.getLogger(FtpRequestProcessor.class.getName());

    private static final int STORE_RESULT_OK = 0;
    private static final int STORE_RESULT_FAULT = 1;
    private static final int STORE_RESULT_DROP = 2;

    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ServiceManager serviceManager;

    private final ContentTypeHeader overriddenContentType;
    private final Goid hardwiredServiceGoid;
    private final String initServiceUri;    // TODO jwilliams: needed here? used in onLogin Ftplet method for some reason
    private final Goid connectorGoid;
    private final long maxRequestSize;

    public FtpRequestProcessor(final MessageProcessor messageProcessor,
                               final SoapFaultManager soapFaultManager,
                               final StashManagerFactory stashManagerFactory,
                               final EventChannel messageProcessingEventChannel,
                               final ServiceManager serviceManager,
                               final ContentTypeHeader overriddenContentType,
                               final Goid hardwiredServiceGoid,
                               final String initServiceUri,
                               final Goid connectorGoid,
                               final long maxRequestSize) {
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.serviceManager = serviceManager;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceGoid = hardwiredServiceGoid;
        this.initServiceUri = initServiceUri;
        this.connectorGoid = connectorGoid;
        this.maxRequestSize = maxRequestSize;
    }

    public void process(FtpMethod method, FtpRequest request, FtpIoSession session) {
        try {
            handleCommandStart(session.getFtpletSession(), request, false, method);
        } catch (FtpException | IOException e) {
            e.printStackTrace(); // TODO jwilliams: handle - look at FTP command implementations
        }
    }

    /**
     * Process commands involving transfer of data.
     */
    public FtpletResult handleTransportStart(FtpIoSession ftpSession, FtpRequest ftpRequest, boolean unique, FtpMethod ftpMethod) throws FtpException, IOException {
        // TODO jwilliams: compare this method to handleUploadStart()
//        FtpSession ftpSession = ftpIoSession.getFtpletSession();

        FtpletResult result = FtpletResult.SKIP;

        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                    "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).",
                    new Object[] {fileName, unique});
        }

        DataConnectionFactory dataConnectionFactory = null;

        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP,
                ((InetSocketAddress) ftpSession.getRemoteAddress()).getAddress().getHostAddress());

        try {
            dataConnectionFactory = ftpSession.getDataConnection();
            DataConnection dataConnection = null;

            try {
                dataConnection = dataConnectionFactory.openConnection();
            } catch(Exception ex) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "Can't open data connection."));
            }

            if (dataConnection != null) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
                // transfer data
                try {
                    User user = ftpSession.getUser();
                    String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();

                    if (initServiceUri != null && path.equals("/")) {
                        ftpSession.getFileSystemView().changeWorkingDirectory(initServiceUri);
                        path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                    }

                    String directory = ((VirtualFileSystem) ftpSession.getFileSystemView()).getChangedDirectory();

                    boolean secure = isSecure(dataConnectionFactory, ftpSession);

                    if (unique) {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, fileName + ": Transfer started."));
                    }

                    int storeResult = onStore(dataConnection, ftpSession, user, path, fileName, secure, unique, ftpMethod, directory);

                    if (storeResult == STORE_RESULT_DROP) {   // TODO jwilliams: this may be a bug introduced with the changes to stealth mode response code - STORE_RESULT_DROP doesn't get set anywhere
                        result = FtpletResult.DISCONNECT;
                    } else if (storeResult == STORE_RESULT_FAULT) {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, fileName + ": " + "Failed."));
                    } else {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer complete."));
                    }
                } catch(IOException ioe) {
                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
                }
            }
        } finally {
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);

            if (dataConnectionFactory != null) {
                dataConnectionFactory.closeDataConnection();
            }
        }

        return result;
    }

    /*
    * Process commands that don't involve data transfer.
    */
    public FtpletResult handleCommandStart(FtpSession ftpSession, FtpRequest ftpRequest, boolean unique, FtpMethod ftpMethod) throws FtpException, IOException {
        FtpletResult result = FtpletResult.SKIP;

        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});

        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getAddress().getHostAddress());

        User user = ftpSession.getUser();
        String file = ftpRequest.getArgument();

        if (initServiceUri != null) {
            ftpSession.getFileSystemView().changeWorkingDirectory(initServiceUri);
        }

        String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
        VirtualFileSystem vfs = (VirtualFileSystem)ftpSession.getFileSystemView();
        String directory;
        String previousDirectory = vfs.getChangedDirectory();

        if (ftpMethod == FtpMethod.FTP_CWD) {
            String uri = file;
            if (!file.startsWith("/")) {
                uri = "/" + file;
            }
            if (isService(uri)) {
                file = uri;
                ftpSession.getFileSystemView().changeWorkingDirectory(file);
                path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                directory = "/";
            } else if (file.startsWith("/")) {
                directory = file;
                vfs.setChangedDirectory(file);
            } else if (file.equals("..") || file.equals("../")) {
                directory = vfs.getParentDirectory();
                vfs.setChangedDirectory(directory);
            } else if (file.equals(".")) {
                directory = vfs.getChangedDirectory();
            } else {
                directory = vfs.getChangedDirectory();
                vfs.setCombinedChangedDirectory(file);
            }
        } else if (ftpMethod == FtpMethod.FTP_CDUP) {
            directory = vfs.getParentDirectory();
            vfs.setChangedDirectory(directory);
        } else {
            directory = vfs.getChangedDirectory();
        }

        boolean secure = isSecureSession(ftpSession);

        if (unique) {
            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
        }

        int storeResult = process(ftpSession, user, path, file, secure, unique, ftpMethod.getWspName(), directory);

        if (storeResult == STORE_RESULT_DROP) {
            result = FtpletResult.DISCONNECT;
        } else if (storeResult == STORE_RESULT_FAULT) {
            if (ftpMethod == FtpMethod.FTP_CWD) {
                vfs.setChangedDirectory(previousDirectory);
            }
        }

        HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
        HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);

        return result;
    }

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
//                            if (storeResult == STORE_RESULT_DROP) {
//                                result = FtpletEnum.RET_DISCONNECT;
//                            } else if (storeResult == STORE_RESULT_FAULT) {
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
     * @param dataConnectionFactory connection factory to check whether data connection is secure
     * @param ftpIoSession ftp session to check whether control connection is secure
     * @return whether the connection is secure
     */
    private boolean isSecure(DataConnectionFactory dataConnectionFactory, FtpIoSession ftpIoSession) { // TODO jwilliams: this checks both the control and data port are secure
        boolean secure = false;

        if (dataConnectionFactory instanceof ServerDataConnectionFactory) {
            ServerDataConnectionFactory serverDataConnectionFactory =
                    (ServerDataConnectionFactory) dataConnectionFactory;

            boolean dataSecure = serverDataConnectionFactory.isSecure();
            boolean controlSecure = ftpIoSession.getListener().isImplicitSsl();

            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Security levels, control secure ''{0}'', data secure ''{1}''.",
                        new Object[] {controlSecure, dataSecure});

            secure = dataSecure && controlSecure;
        }

        return secure;
    }

    /**
     * Check if the session is secure.
     *
     * <p>To be secure for delete control must be secured.</p>
     *
     * <p>NOTE: This will NOT WORK for explicit FTP, which is currently fine
     * since that is not enabled.</p>
     * @param ftpSession ftp session to check whether control connection is secure
     * @return true if the control connection is secure
     */
    private boolean isSecureSession(FtpSession ftpSession) { // TODO jwilliams: this only checks that the control port is secure
    // TODO jwilliams: uncomment and update this?
        boolean controlSecure = false;

//        if (ftpSession instanceof FtpServerSession) {
//            FtpServerSession ftpServerSession = (FtpServerSession) ftpSession;
//            controlSecure = ((AbstractListener)ftpServerSession.getListener()).isImplicitSsl();
//        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Security levels, control secure ''{0}'', data secure ''{1}''.",
                    new Object[] {controlSecure});

        return controlSecure;
    }

    /*
     * Store to message processor
     */
    private int onStore(final DataConnection dataConnection,
                        final FtpIoSession ftpIoSession,
                        final User user,
                        final String path,
                        final String file,
                        final boolean secure,
                        final boolean unique,
                        final FtpMethod ftpMethod,
                        final String directory) throws IOException {
        int storeResult = STORE_RESULT_FAULT;

        FtpSession ftpletSession = ftpIoSession.getFtpletSession();

        logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[] {path, file});

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        ContentTypeHeader ctype = overriddenContentType != null
                ? overriddenContentType
                : ContentTypeHeader.XML_DEFAULT;

        // Create request message
        Message request = new Message();

        if (ftpMethod == FtpMethod.FTP_PUT || ftpMethod == FtpMethod.FTP_APPE) {
            request.initialize(stashManagerFactory.createStashManager(), ctype,
                    getDataInputStream(ftpletSession, dataConnection, buildUri(path, file)), maxSize);
        } else {
            request.initialize(stashManagerFactory.createStashManager(), ctype,
                    new ByteArrayInputStream(new byte[0]), maxSize);
        }

        request.initialize(stashManagerFactory.createStashManager(), ctype,
                getDataInputStream(ftpletSession, dataConnection, buildUri(path, file)),maxSize);

        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();

        request.attachFtpKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
                file,
                path,
                secure,
                unique,
                user));

        if (!Goid.isDefault(hardwiredServiceGoid)) {
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        // Create response message
        Message response = new Message();

        // process request message
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        AssertionStatus status = AssertionStatus.UNDEFINED;
        String faultXml = null;

        //set the context variable to check the ftp method
        context.setVariable("ftp.command", ftpMethod.getWspName());
        context.setVariable("ftp.directory", directory);

        try {
            try {
                status = messageProcessor.processMessage(context);

                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
            } catch (PolicyVersionException pve) {
                logger.log(Level.INFO, "Request referred to an outdated version of policy");
                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Exception while processing FTP message: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
            }

            if (status != AssertionStatus.NONE) {
                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
            }

            if (faultXml != null) {
                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
            }

            if (!context.isStealthResponseMode() && status == AssertionStatus.NONE) {
                storeResult = STORE_RESULT_OK;

                if (ftpMethod != FtpMethod.FTP_GET && !containListCommand(ftpMethod)) {
                    logger.log(Level.INFO, "FTP " + ftpMethod.getWspName() + " completed");
                } else {
                    Message responseContext = context.getResponse();

                    if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
                        try {
                            InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();

                            if (responseStream != null) {
                                if (ftpMethod == FtpMethod.FTP_GET) {
                                    if (status == AssertionStatus.NONE) {
                                        long readLength = readFile(ftpletSession, dataConnection, responseStream);

                                        if (readLength < 0) {
                                            logger.log(Level.WARNING, "Error during reading the file");
                                            storeResult = STORE_RESULT_FAULT;
                                        }
                                    } else {
                                        logger.log(Level.WARNING, "Error during reading the file");
                                        storeResult = STORE_RESULT_FAULT;
                                    }
                                } else if (containListCommand(ftpMethod)) {
                                    FtpListUtil ftpListUtil = new FtpListUtil(responseStream);
                                    String responseMessage = ftpListUtil.writeMessageToOutput();
                                    if (!responseMessage.isEmpty()) {
                                        String rawMessage = responseMessage;

                                        if (ftpMethod != FtpMethod.FTP_NLST) {
                                            Document doc = FtpListUtil.createDoc(responseMessage);
                                            if (doc != null) {
                                                rawMessage = FtpListUtil.getRawListData(doc);
                                            }
                                        }

                                        boolean success = listFiles(ftpletSession, dataConnection, rawMessage);

                                        if (!success) {
                                            logger.log(Level.WARNING, "Error during list FTP");
                                            storeResult = STORE_RESULT_FAULT;
                                        }
                                    }
                                }
                            } else {
                                logger.log(Level.WARNING, "Error during processing FTP");
                                storeResult = STORE_RESULT_FAULT;
                            }
                        } catch(NoSuchPartException nsp) {
                            logger.log(Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp));
                            storeResult = STORE_RESULT_FAULT;
                        }
                    } else  {
                        logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
                        storeResult = STORE_RESULT_FAULT;
                    }
                }
            } else {
                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                storeResult = STORE_RESULT_FAULT;
            }

// TODO jwilliams: old code replaced by above chunk, keeping here until question of the unset 'DROP' result is resolved
//            if (!context.isStealthResponseMode()) {
//                if (status == AssertionStatus.NONE) {
//                    storeResult = STORE_RESULT_OK;
//                }
//            } else {
//                storeResult = STORE_RESULT_DROP;
//            }
        } finally {
            ResourceUtils.closeQuietly(context);
        }

        return storeResult;
    }

    /*
     *  Process commands that don't require Data Transaction
     */
    private int process(final FtpSession ftpSession,
                        final User user,
                        final String path,
                        final String file,
                        final boolean secure,
                        final boolean unique,
                        final String command,
                        final String directory) throws IOException, FtpException {
        int storeResult = STORE_RESULT_FAULT;

        logger.log(Level.FINE,
                "Processing " + command + " for path ''{0}'' and file ''{1}''.",
                new String[] {path, file});

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        // Create request message
        Message request;
        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
        Message requestMessage = new Message();

        requestMessage.initialize(stashManagerFactory.createStashManager(), cType, new ByteArrayInputStream(new byte[0]), maxSize);

        requestMessage.attachFtpKnob(buildFtpKnob(
                ftpSession.getServerAddress().getAddress(),
                ftpSession.getServerAddress().getPort(),
                ftpSession.getClientAddress().getAddress(),
                file,
                path,
                secure,
                unique,
                user));
        request = requestMessage;

        if (!Goid.isDefault(hardwiredServiceGoid)) {
            requestMessage.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        // Create response message
        Message response = new Message();

        // process request message
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

        AssertionStatus status = AssertionStatus.UNDEFINED;
        String faultXml = null;

        //set the context variable to check the ftp method
        context.setVariable("ftp.command", command);
        context.setVariable("ftp.directory", directory);

        try {
            try {
                status = messageProcessor.processMessage(context);

                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
            } catch (PolicyVersionException pve) {
                logger.log(Level.INFO, "Request referred to an outdated version of policy");
                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Exception while processing FTP message: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
            }

            if (status != AssertionStatus.NONE) {
                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
            }

            if (faultXml != null)
                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

            if (!context.isStealthResponseMode()) {
                String responseMessage = null;

                if (status == AssertionStatus.NONE) {
                    storeResult = STORE_RESULT_OK;
                    logger.log(Level.INFO, "FTP " + command + " completed");
                } else {
                    storeResult = STORE_RESULT_FAULT;
                }

                Message responseContext = context.getResponse();

                if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
                    try {
                        InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();

                        if (responseStream != null) {
                            FtpListUtil ftpListUtil = new FtpListUtil(responseStream);
                            responseMessage = ftpListUtil.writeMessageToOutput();
                            createReplyOutput(command, ftpSession, responseMessage, directory, storeResult);
                        }
                    } catch(NoSuchPartException nsp) {
                        logger.log(Level.WARNING, "Exception while processing FTP message: " + ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp));
                        storeResult = STORE_RESULT_FAULT;
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                    }
                } else if (!command.equals("APPE") && !command.equals("DELE")) {
                    logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
                    storeResult = STORE_RESULT_FAULT;
                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                } else {
                    if (status == AssertionStatus.NONE) {
                        createReplyOutput(command, ftpSession, responseMessage, directory, storeResult);
                    } else {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                    }
                }
            } else {
                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                storeResult = STORE_RESULT_FAULT;
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
            }
        } finally {
            ResourceUtils.closeQuietly(context);
        }

        return storeResult;
    }

    private String buildUri(String path, String file) { // TODO jwilliams: is this necessary?
        String uri = path;

        if (!uri.endsWith("/")) {
            uri += "/";
        }

        uri += file;

        return uri;
    }

    private void createReplyOutput(final String command, final FtpSession ftpSession, final String responseMessage, String directory, int storeResult) throws IOException, FtpException {
        if (directory != null && !directory.startsWith("/")) {
            directory = "/" + directory;
        }

        if (storeResult == STORE_RESULT_OK && !command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
            if (command.equals(FtpMethod.FTP_APPE.getWspName())) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer OK"));
            } else if (command.equals(FtpMethod.FTP_CDUP.getWspName())) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "CDUP successful. \"" + directory + "\" is current directory."));
            } else if (command.equals(FtpMethod.FTP_PWD.getWspName())) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_257_PATHNAME_CREATED, "\"" + directory + "\" is current directory."));
            } else if (command.equals(FtpMethod.FTP_MDTM.getWspName())) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_213_FILE_STATUS, org.apache.ftpserver.util.DateUtils.getFtpDate(Long.parseLong(responseMessage))));
            } else if (command.equals(FtpMethod.FTP_SIZE.getWspName())) {
                ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
            } else {
                if (responseMessage != null && !responseMessage.isEmpty()) {
                    int result = createReplyMessage(ftpSession, responseMessage);

                    if (result == -1) {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
                    }
                } else {
                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "OK"));
                }
            }
        } else {
            if (command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
                createReplyMessage(ftpSession, responseMessage);
            } else {
                if (responseMessage != null && !responseMessage.isEmpty()) {
                    int result = createReplyMessage(ftpSession, responseMessage);

                    if (result == -1) {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, responseMessage));
                    }
                } else {
                    ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Requested action not taken"));
                }
            }
        }
    }

    private int createReplyMessage(final FtpSession ftpSession, final String responseMessage) throws IOException, FtpException {
        try {
            int replyCode = Integer.parseInt(responseMessage.substring(0, 3));
            String replyMessage = responseMessage.substring(4, responseMessage.length());

            if (replyMessage.endsWith("\r\n")) {
                replyMessage = replyMessage.substring(0, replyMessage.indexOf("\r\n"));
            }

            ftpSession.write(new DefaultFtpReply(replyCode, replyMessage));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return -1;
        }

        return 0;
    }

    private boolean isService(String uri) {
        boolean isService = false;
        int countService = 0;

        try {
            Collection<PublishedService> publishedServices = serviceManager.findByRoutingUri(uri);

            for (PublishedService publishedService : publishedServices) {
                if (uri.equals(publishedService.getRoutingUri()) && !publishedService.isDisabled()) {
                    countService++;
                }
            }
        } catch (FindException fe) {
            return false;
        }

        if (countService == 0) {
            logger.log(Level.WARNING, "There is no service with uri " + uri);
        } else if (countService == 1) {
            isService = true;
        } else {
            logger.log(Level.WARNING, "There is more then one enabled service with the uri " + uri);
        }

        return isService;
    }

    /*
     * Create an FtpKnob for the given info.
     */
    private FtpRequestKnob buildFtpKnob(final InetAddress serverAddress, final int port, final InetAddress clientAddress, final String file, final String path, final boolean secure, final boolean unique, final User user) {
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
            public String getFile() {
                return file;
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
                urlBuilder.append(file);

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
     * Convert OutputStream to InputStream.
     */
    private InputStream getDataInputStream(final FtpSession ftpSession,
                                           final DataConnection dataConnection,
                                           final String fullPath) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                PipedOutputStream pos = null;
                try {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", fullPath);

                    //noinspection IOResourceOpenedButNotSafelyClosed
                    pos = new PipedOutputStream(pis);
                    startedSignal.countDown();
                    dataConnection.transferFromClient(ftpSession, pos);

                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", fullPath);
                }
                catch (IOException ioe) {
                    logger.log(Level.WARNING, "Data transfer error for '"+fullPath+"'.", ioe);
                }
                finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
            }
        }, "FtpServer-DataTransferThread-" + System.currentTimeMillis());

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

    private boolean listFiles(final FtpSession ftpSession, final DataConnection dataConnection, String dirList) {
        boolean success = false;

        try {
            dataConnection.transferToClient(ftpSession, dirList); // TODO jwilliams: maybe we can use something different to the DataConnection, something from core rather than use the ftplet api?
            success = true;
        } catch (SocketException ex) {
            logger.log(Level.WARNING, "Socket exception during list transfer", ex);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "IOException during list transfer", ex);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Illegal list syntax: ", e);
        }

        return success;
    }

    /*
    * Read the file from InputStream
    */
    private long readFile(final FtpSession ftpSession,
                          final DataConnection dataConnection,
                          final InputStream is)
            throws IOException {
        long length = 0L;

        try {
            logger.log(Level.FINE, "Starting data transfer");

            length = dataConnection.transferToClient(ftpSession, is);

            logger.log(Level.FINE, "Completed data transfer");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Data transfer error", e);
            length = -1;
        } finally {
            ResourceUtils.closeQuietly(is);
        }

        return length;
    }

    private boolean containListCommand(FtpMethod ftpMethod) {
        String[] listCommands = new String[] { // TODO jwilliams: handle better in the refactored FtpMethod/FtpMethodEnum
                FtpMethod.FTP_LIST.getWspName(),
                FtpMethod.FTP_MDTM.getWspName(),
                FtpMethod.FTP_MLSD.getWspName(),
                FtpMethod.FTP_NLST.getWspName()
        };

        return Arrays.asList(listCommands).contains(ftpMethod.getWspName());
    }
}
