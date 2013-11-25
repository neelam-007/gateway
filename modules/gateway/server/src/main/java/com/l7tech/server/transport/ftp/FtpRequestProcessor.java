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
import com.l7tech.util.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.impl.FtpIoSession;
import org.apache.ftpserver.impl.IODataConnectionFactory;
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
 * Most of this class' functionality was extracted from {@link SsgFtplet} (formerly named MessageProcessingFtplet).
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class FtpRequestProcessor {
    private static final Logger logger = Logger.getLogger(FtpRequestProcessor.class.getName());

    private static final int RESULT_OK = 0;
    private static final int RESULT_FAULT = 1;
    private static final int RESULT_DROP = 2; // TODO jwilliams: should really be changed - the decision to disconnect is independent of the success/fault result

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
            switch (method.getFtpMethodEnum()) {
                case FTP_APPE:
                    System.out.println("FTP_APPE");
                    handleUpload(session, request, false);
                    break;
                case FTP_PUT:
                    System.out.println("FTP_PUT");
                    handleUpload(session, request, false);
                    break;
                case FTP_STOU:
                    System.out.println("FTP_STOU");
                    handleUpload(session, request, true);
                    break;

//                case FTP_MLST: // TODO jwilliams: uncomment and remove direct invocation of handleTransportStart from FtpCommand implementations
//                    break;

                case FTP_LIST:
                case FTP_MLSD:
                case FTP_NLST:
                    handleListCommand(session, request, method);
                    break;

//                case FTP_GET:
//                    break;
//                case FTP_DELE:
//                    break;
//                case FTP_ABOR:
//                    break;
//                case FTP_ACCT:
//                    break;
//                case FTP_ADAT:
//                    break;
//                case FTP_ALLO:
//                    break;
//                case FTP_AUTH:
//                    break;
//                case FTP_CCC:
//                    break;
//                case FTP_CDUP:
//                    break;
//                case FTP_CONF:
//                    break;
//                case FTP_CWD: // TODO jwilliams: only update VFS currentDirectory on success of directory commands
//                    break;
//                case FTP_ENC:
//                    break;
//                case FTP_EPRT:
//                    break;
//                case FTP_EPSV:
//                    break;
//                case FTP_FEAT:
//                    break;
//                case FTP_HELP:
//                    break;
//                case FTP_LANG:
//                    break;
//                case FTP_MDTM:
//                    break;
//                case FTP_MIC:
//                    break;
//                case FTP_MKD:
//                    break;
//                case FTP_MODE:
//                    break;
//                case FTP_NOOP:
//                    break;
//                case FTP_OPTS:
//                    break;
//                case FTP_PASS:
//                    break;
//                case FTP_PASV:
//                    break;
//                case FTP_PBSZ:
//                    break;
//                case FTP_PORT:
//                    break;
//                case FTP_PROT:
//                    break;
//                case FTP_PWD:
//                    break;
//                case FTP_QUIT:
//                    break;
//                case FTP_REIN:
//                    break;
//                case FTP_RMD:
//                    break;
//                case FTP_RNFR:
//                    break;
//                case FTP_RNTO:
//                    break;
//                case FTP_SITE:
//                    break;
//                case FTP_SIZE:
//                    break;
//                case FTP_STAT:
//                    break;
//                case FTP_STRU:
//                    break;
//                case FTP_SYST:
//                    break;
//                case FTP_TYPE:
//                    break;
//                case FTP_USER:
//                    break;
//                case FTP_LOGIN:
//                    break;

                default:
                    handleCommandStart(session, request, false, method);
            }
        } catch (FtpException | IOException e) {
            e.printStackTrace(); // TODO jwilliams: handle - look at FTP command implementations
        }
    }

    /*
    * Process a file upload
    */
    private FtpletResult handleUpload(FtpIoSession ftpSession, FtpRequest ftpRequest, boolean unique) throws FtpException, IOException {
        FtpletResult result = FtpletResult.SKIP;
        String file = ftpRequest.getArgument();

        System.out.println("Handling Upload Start: STOR for file ''" + file + "''");

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling STOR for file ''{0}'' (unique:{1}).", new Object[] {file, unique});

        if (!ftpSession.getDataType().equals(DataType.BINARY)) {
            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Type '" + ftpSession.getDataType().toString() + "' not supported for this action."));
        } else {
            // request data
            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
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
                    try {
                        User user = ftpSession.getUser();
                        String path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                        boolean secure = isSecure(ftpSession);

                        if (unique) {
                            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
                        }

                        int storeResult = stor(dataConnection, ftpSession, user, path, file, secure, unique);

                        if ( storeResult == RESULT_DROP) {
                            result = FtpletResult.DISCONNECT;
                        } else if ( storeResult == RESULT_FAULT) {
                            ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, file + ": Failed"));
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
                if (dataConnectionFactory != null) dataConnectionFactory.closeDataConnection();
            }
        }

        return result;
    }

    /*
     * Store to message processor
     */
    private int stor(final DataConnection dataConnection,
                        final FtpIoSession ftpIoSession,
                        final User user,
                        final String path,
                        final String file,
                        final boolean secure,
                        final boolean unique) throws IOException {
        int storeResult = RESULT_FAULT;

        FtpSession ftpletSession = ftpIoSession.getFtpletSession();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[] {path, file});

        System.out.println("Processing STOR for path " + path + " and file " + file + ".");

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() :maxRequestSize;

        // Create request message
        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;

        Message request = new Message();
        request.initialize(stashManagerFactory.createStashManager(), cType,
                getDataInputStream(ftpletSession, dataConnection, buildUri(path, file)), maxSize);

        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();

        request.attachFtpKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
                null, // TODO jwilliams: placeholder
                file,
                path,
                secure,
                unique,
                user));

        if (!Goid.isDefault(hardwiredServiceGoid)) {
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        // process request message
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, true);

        AssertionStatus status = AssertionStatus.UNDEFINED;
        String faultXml = null;

        context.setVariable("ftp.directory", path); // TODO jwilliams: remove (?)

        try {
            try {
                status = messageProcessor.processMessage(context);

                if (logger.isLoggable(Level.FINER))
                    logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);

            } catch ( PolicyVersionException pve ) {
                logger.log( Level.INFO, "Request referred to an outdated version of policy" );
                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
            } catch ( Throwable t ) {
                logger.log( Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException( t ) );
                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
            }

            if (status != AssertionStatus.NONE) {
                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
            }

            if (faultXml != null)
                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

            if (!context.isStealthResponseMode()) {
                if (status == AssertionStatus.NONE) {
                    storeResult = RESULT_OK;
                }
            } else {
                storeResult = RESULT_DROP;
            }
        } finally {
            ResourceUtils.closeQuietly(context);
        }

        return storeResult;
    }

    public void handleListCommand(final FtpIoSession session, final FtpRequest ftpRequest, final FtpMethod ftpMethod) throws FtpException {
        System.out.println("PROCESSING LIST COMMAND: " + ftpMethod.getWspName());

        // reset state variables
        session.resetState();

        // check connection factory state
        DataConnectionFactory connFactory = session.getDataConnection();

        if (connFactory instanceof IODataConnectionFactory) {   // TODO jwilliams: consider handling in own separate method to avoid code duplication
            InetAddress address = ((IODataConnectionFactory) connFactory).getInetAddress();

            if (address == null) {
                session.write(new DefaultFtpReply(
                        FtpReply.REPLY_503_BAD_SEQUENCE_OF_COMMANDS,
                        "PORT or PASV must be issued first"));
                return;
            }
        }

        session.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY,
                "File status okay; about to open data connection."));

        // open a data connection
        DataConnection dataConnection;

        try {
            dataConnection = session.getDataConnection().openConnection();
        } catch (Exception e) {
            session.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, ftpMethod.getWspName()));
            return;
        }

        Message request;

        // create request message
        try {
            request = createRequestMessage(session, ftpRequest, ftpMethod, new ByteArrayInputStream(new byte[0]));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error creating request message: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
            // TODO jwilliams: write fail reply
            return;
        }

        // create PEC
        final PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message(), true);

        int storeResult;

        try {
            // process request message
            AssertionStatus status = processRequestMessage(context);

            if (context.isStealthResponseMode()) { // TODO jwilliams: need a common way of handling this - should it only be for legacy mode?
                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                storeResult = RESULT_FAULT; // TODO jwilliams: should the connection drop at this point? or only for STOR?
                session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + ftpMethod.getWspName() + " Request."));
                return;
            }

            if (status == AssertionStatus.NONE) {
                storeResult = RESULT_OK;
                logger.log(Level.INFO, "FTP " + ftpMethod.getWspName() + " completed");
            } else {
                logger.log(Level.WARNING, "Error during processing FTP");
                storeResult = RESULT_FAULT; // TODO jwilliams: handle
            }

            // get response input stream
            Message responseContext = context.getResponse();

            if (responseContext.getKnob(MimeKnob.class) == null || !responseContext.isInitialized()) {
                logger.log(Level.WARNING, "Error processing FTP request: Response is not initialized");
                storeResult = RESULT_FAULT;
                session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + ftpMethod.getWspName() + " Request."));
                return;
            }

            InputStream responseStream = null;

            try {
                responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream(); // TODO jwilliams: find out if we're making session messages available from JScape
            } catch (NoSuchPartException nsp) {
                logger.log(Level.WARNING, "Error processing FTP request: " + ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp)); // TODO jwilliams: should we be using debugException?
                storeResult = RESULT_FAULT;
                session.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + ftpMethod.getWspName() + " Request."));
                // TODO jwilliams: handle return
            } catch (IOException ioe) {
                session.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
                // TODO jwilliams: handle logging & return
            }

            if (null == responseStream) {
                logger.log(Level.WARNING, "Error during processing FTP");
                storeResult = RESULT_FAULT;
                // TODO jwilliams: handle
            }

            // transfer response to client
            long bytesTransferred = 0;

            try {
                bytesTransferred = transferDataToClient(session.getFtpletSession(), dataConnection, responseStream);
            } catch (IOException e) {
                e.printStackTrace();
                session.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
                // TODO jwilliams: handle logging & return
            }

            if (bytesTransferred < 0) {
                logger.log(Level.WARNING, "Error transferring data to client.");
                storeResult = RESULT_FAULT;
                // TODO jwilliams: fix logging & handle return
            }

            // write success reply
            session.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Listing completed"));
        } finally {
            session.getDataConnection().closeDataConnection();
            ResourceUtils.closeQuietly(context);
        }

        // TODO jwilliams: handle storeResult meaningfully or get rid of it
    }

    private AssertionStatus processRequestMessage(PolicyEnforcementContext context) {
        FtpRequestKnob ftpRequest = context.getRequest().getKnob(FtpRequestKnob.class);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Processing " + ftpRequest.getCommand() + " for path ''{0}'' and file ''{1}''.",
                    new String[] {ftpRequest.getPath(), ftpRequest.getFile()});
        }

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
            logger.log(Level.WARNING, "PROCESS MESSAGE PROCESSOR: Exception while processing FTP message: " +
                    ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
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
        String file = ftpRequest.getArgument();
        String path = ftpIoSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();

        boolean secure = isSecure(ftpIoSession);
        boolean unique = ftpMethod.getFtpMethodEnum() == FtpMethod.FtpMethodEnum.FTP_STOU;

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() :maxRequestSize;

        // Create request message
        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;

//        InputStream inputStream;
//
//        if (ftpMethod == FtpMethod.FTP_PUT || ftpMethod == FtpMethod.FTP_APPE) {
//            inputStream = readRequestDataFromClient(ftpIoSession, dataConnection, buildUri(path, file));
//        } else {
//            inputStream = new ByteArrayInputStream(new byte[0]);
//        }

        Message request = new Message();

        request.initialize(stashManagerFactory.createStashManager(), cType, inputStream, maxSize);

        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();

        request.attachFtpKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
                ftpMethod.getWspName(),
                file,
                path,
                secure,
                unique,
                user));

        if (!Goid.isDefault(hardwiredServiceGoid)) {
            request.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        return request;
    }

    /*
    * Convert OutputStream to InputStream.
    */
    private InputStream readRequestDataFromClient(final FtpIoSession ftpSession,
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
                    dataConnection.transferFromClient(ftpSession.getFtpletSession(), pos);

                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", fullPath);
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Data transfer error for '" + fullPath + "'.", ioe);
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
        } catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for data.", ie);
        }

        return pis;
    }

    /**
     * Process commands involving transfer of data.
     */
    public FtpletResult handleTransportStart(FtpIoSession ftpSession, FtpRequest ftpRequest, boolean unique, FtpMethod ftpMethod) throws FtpException {
        FtpletResult result = FtpletResult.SKIP;

        String fileName = ftpRequest.getArgument(); // TODO jwilliams: change to "argument" for clarity and consistency

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

                    if (initServiceUri != null && path.equals("/")) { // TODO jwilliams: investigate directory issue here
                        ftpSession.getFileSystemView().changeWorkingDirectory(initServiceUri);
                        path = ftpSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                    }

                    String directory = ((VirtualFileSystem) ftpSession.getFileSystemView()).getChangedDirectory();

                    boolean secure = isSecure(ftpSession);

                    if (unique) {
                        ftpSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, fileName + ": Transfer started."));
                    }

                    int storeResult = onStore(dataConnection, ftpSession, user, path, fileName, secure, unique, ftpMethod, directory);

                    if (storeResult == RESULT_DROP) {   // TODO jwilliams: this may be a bug introduced with the changes to stealth mode response code - RESULT_DROP doesn't get set anywhere
                        result = FtpletResult.DISCONNECT;
                    } else if (storeResult == RESULT_FAULT) {
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
    public FtpletResult handleCommandStart(FtpIoSession ftpIoSession, FtpRequest ftpRequest, boolean unique, FtpMethod ftpMethod) throws FtpException, IOException {
        FtpletResult result = FtpletResult.SKIP;

        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});

        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString());
        HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress().getHostAddress());

        User user = ftpIoSession.getUser();
        String argument = ftpRequest.getArgument();

        if (initServiceUri != null) {
            ftpIoSession.getFileSystemView().changeWorkingDirectory(initServiceUri); // TODO jwilliams: why CWD for every command?
        }

        VirtualFileSystem vfs = (VirtualFileSystem) ftpIoSession.getFileSystemView();

        String path = vfs.getWorkingDirectory().getAbsolutePath();
        String previousDirectory = vfs.getChangedDirectory(); // TODO jwilliams: store CWD in case the policy fails and we need to revert to it

        String directory;

        if (ftpMethod == FtpMethod.FTP_CWD) {
            String uri;

            if (argument.startsWith("/")) {
                uri = argument;
            } else {
                uri = "/" + argument;
            }

            if (isService(uri)) {
                argument = uri; // TODO jwilliams: this has to be a bug - if we had a service named "/example" then a CWD request for any subdir named "example" would change dir to the service!
                ftpIoSession.getFileSystemView().changeWorkingDirectory(argument);
                path = ftpIoSession.getFileSystemView().getWorkingDirectory().getAbsolutePath();
                directory = "/";
            } else if (argument.startsWith("/")) {
                directory = argument;
                vfs.setChangedDirectory(argument);
            } else if (argument.equals("..") || argument.equals("../")) {
                directory = vfs.getParentDirectory();
                vfs.setChangedDirectory(directory);
            } else if (argument.equals(".")) {
                directory = vfs.getChangedDirectory();
            } else {
                directory = vfs.getChangedDirectory();
                vfs.setCombinedChangedDirectory(argument);
            }
        } else if (ftpMethod == FtpMethod.FTP_CDUP) {
            directory = vfs.getParentDirectory();   // TODO jwilliams: directory only used to generate response
            vfs.setChangedDirectory(directory);
        } else {
            directory = vfs.getChangedDirectory();
        }

        boolean secure = isSecureSession(ftpIoSession);

        if (unique) {
            ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, argument + ": Transfer started.")); // TODO jwilliams: unique is unnecessary - remove
        }

        int storeResult = process(ftpIoSession, user, path, argument, secure, unique, ftpMethod.getWspName(), directory);

        if (storeResult == RESULT_DROP) { // TODO jwilliams: unreliable - routing could succeed but the policy fail
            result = FtpletResult.DISCONNECT;
        } else if (storeResult == RESULT_FAULT) {
            if (ftpMethod == FtpMethod.FTP_CWD) { // TODO jwilliams: should this reversion apply to CDUP as well? could there be an odd permissions corner case?
                vfs.setChangedDirectory(previousDirectory); // TODO jwilliams: if the attempted operation was a CD and the assertion fails, revert VFS working directory
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

    /**
     * Check if the session is secure.
     *
     * <p>To be secure for delete control must be secured.</p>
     *
     * <p>NOTE: This will NOT WORK for explicit FTP, which is currently fine
     * since that is not enabled.</p>
     * @param ftpIoSession ftp session to check whether control connection is secure
     * @return true if the control connection is secure
     */
    private boolean isSecureSession(FtpIoSession ftpIoSession) {
        boolean controlSecure = ftpIoSession.getListener().isImplicitSsl();

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
        int storeResult = RESULT_FAULT;

        FtpSession ftpletSession = ftpIoSession.getFtpletSession();

        if (logger.isLoggable(Level.FINE))
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

        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();

        request.attachFtpKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
                ftpMethod.getWspName(),
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
                logger.log(Level.WARNING, "MESSAGE PROCESSOR: Exception while processing FTP message: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
            }

            if (status != AssertionStatus.NONE) {
                faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
            }

            if (faultXml != null) {
                messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
            }

            if (!context.isStealthResponseMode() && status == AssertionStatus.NONE) {
                storeResult = RESULT_OK;

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
                                        long readLength = transferDataToClient(ftpletSession, dataConnection, responseStream);

                                        if (readLength < 0) {
                                            logger.log(Level.WARNING, "Error during reading the file");
                                            storeResult = RESULT_FAULT;
                                        }
                                    } else {
                                        logger.log(Level.WARNING, "Error during reading the file");
                                        storeResult = RESULT_FAULT;
                                    }
                                } else if (containListCommand(ftpMethod)) {
                                    String responseMessage = FtpListUtil.writeMessageToOutput(responseStream);
                                    if (!responseMessage.isEmpty()) {
                                        String rawMessage = responseMessage;

                                        if (ftpMethod != FtpMethod.FTP_NLST) {
                                            Document doc = FtpListUtil.createDoc(responseMessage);
                                            if (doc != null) {
                                                rawMessage = FtpListUtil.getRawListData(doc);
                                            }
                                        }

                                        boolean success = listFiles(ftpIoSession, dataConnection, rawMessage);

                                        if (!success) {
                                            logger.log(Level.WARNING, "Error during list FTP");
                                            storeResult = RESULT_FAULT;
                                        }
                                    }
                                }
                            } else {
                                logger.log(Level.WARNING, "Error during processing FTP");
                                storeResult = RESULT_FAULT;
                            }
                        } catch (NoSuchPartException nsp) {
                            logger.log(Level.WARNING, "ON STORE GET RESPONSE MESSAGE BODY: Exception while processing FTP message: "+ ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp));
                            storeResult = RESULT_FAULT;
                        }
                    } else  {
                        logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
                        storeResult = RESULT_FAULT;
                    }
                }
            } else {
                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                storeResult = RESULT_FAULT;
            }

// TODO jwilliams: old code replaced by above chunk, keeping here until question of the unset 'DROP' result is resolved
//            if (!context.isStealthResponseMode()) {
//                if (status == AssertionStatus.NONE) {
//                    storeResult = RESULT_OK;
//                }
//            } else {
//                storeResult = RESULT_DROP;
//            }
        } finally {
            ResourceUtils.closeQuietly(context);
        }

        return storeResult;
    }

    /*
     *  Process commands that don't require Data Transaction
     */
    private int process(final FtpIoSession ftpIoSession,
                        final User user,
                        final String path,
                        final String file,
                        final boolean secure,
                        final boolean unique,
                        final String command,
                        final String directory) throws IOException, FtpException {
        int storeResult = RESULT_FAULT;

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Processing " + command + " for path ''{0}'' and file ''{1}''.",
                    new String[] {path, file});
        }

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        // Create request message
        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
        Message request = new Message();

        request.initialize(stashManagerFactory.createStashManager(), cType, new ByteArrayInputStream(new byte[0]), maxSize);

        InetSocketAddress serverAddress = (InetSocketAddress) ftpIoSession.getLocalAddress();

        request.attachFtpKnob(buildFtpKnob(
                serverAddress.getAddress(),
                serverAddress.getPort(),
                ((InetSocketAddress) ftpIoSession.getRemoteAddress()).getAddress(),
                command,
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
        System.out.println("PROCESS: ftp.command - " + command);
        context.setVariable("ftp.directory", directory);

        try {
            try {
                status = messageProcessor.processMessage(context);

                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
            } catch (PolicyVersionException pve) {
                logger.log(Level.INFO, "Request referred to an outdated version of policy");
                faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "PROCESS MESSAGE PROCESSOR: Exception while processing FTP message: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                t.printStackTrace();
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
                    storeResult = RESULT_OK;
                    logger.log(Level.INFO, "FTP " + command + " request processing completed.");
                } else {
                    storeResult = RESULT_FAULT;
                }

                Message responseContext = context.getResponse();

                if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
                    try {
                        InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();

                        if (responseStream != null) {
                            responseMessage = FtpListUtil.writeMessageToOutput(responseStream);
                            createReplyOutput(command, ftpIoSession, responseMessage, directory, storeResult);
                        }
                    } catch (NoSuchPartException nsp) {
                        logger.log(Level.WARNING, "Exception while processing FTP message: " + ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException(nsp));
                        storeResult = RESULT_FAULT;
                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                    }
                } else if (!command.equals("APPE") && !command.equals("DELE")) {
                    logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
                    storeResult = RESULT_FAULT;
                    ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                } else {
                    if (status == AssertionStatus.NONE) {
                        createReplyOutput(command, ftpIoSession, responseMessage, directory, storeResult);
                    } else {
                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                    }
                }
            } else {
                logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                storeResult = RESULT_FAULT;
                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
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

    private void createReplyOutput(final String command, final FtpIoSession ftpIoSession, final String responseMessage, String directory, int storeResult) throws IOException, FtpException {
        if (directory != null && !directory.startsWith("/")) {
            directory = "/" + directory;
        }

        if (storeResult == RESULT_OK && !command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
            if (command.equals(FtpMethod.FTP_APPE.getWspName())) {
                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer OK"));
            } else if (command.equals(FtpMethod.FTP_CDUP.getWspName())) {
                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "CDUP successful. \"" + directory + "\" is current directory."));
            } else if (command.equals(FtpMethod.FTP_PWD.getWspName())) {
                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_257_PATHNAME_CREATED, "\"" + directory + "\" is current directory."));
            } else if (command.equals(FtpMethod.FTP_MDTM.getWspName())) {
                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_213_FILE_STATUS, org.apache.ftpserver.util.DateUtils.getFtpDate(Long.parseLong(responseMessage))));
            } else if (command.equals(FtpMethod.FTP_SIZE.getWspName())) {
                ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
            } else {
                if (responseMessage != null && !responseMessage.isEmpty()) {
                    int result = createReplyMessage(ftpIoSession, responseMessage);

                    if (result == -1) {
                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
                    }
                } else {
                    ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "OK"));
                }
            }
        } else {
            if (command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
                createReplyMessage(ftpIoSession, responseMessage);
            } else {
                if (responseMessage != null && !responseMessage.isEmpty()) {
                    int result = createReplyMessage(ftpIoSession, responseMessage);

                    if (result == -1) {
                        ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, responseMessage));
                    }
                } else {
                    ftpIoSession.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Requested action not taken"));
                }
            }
        }
    }

    private int createReplyMessage(final FtpIoSession ftpIoSession, final String responseMessage) throws IOException, FtpException {
        try {
            int replyCode = Integer.parseInt(responseMessage.substring(0, 3));
            String replyMessage = responseMessage.substring(4, responseMessage.length());

            if (replyMessage.endsWith("\r\n")) {
                replyMessage = replyMessage.substring(0, replyMessage.indexOf("\r\n")); // TODO jwilliams: shouldn't this be lastIndexOf? in that case, just use the reply length minus return chars
            }

            ftpIoSession.write(new DefaultFtpReply(replyCode, replyMessage));
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
    private FtpRequestKnob buildFtpKnob(final InetAddress serverAddress, final int port,
                                        final InetAddress clientAddress, final String command, final String file,
                                        final String path, final boolean secure, final boolean unique, final User user) {
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
                return command;
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
                } catch (IOException ioe) {
                    logger.log(Level.WARNING, "Data transfer error for '"+fullPath+"'.", ioe);
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

    private boolean listFiles(final FtpIoSession ftpIoSession, final DataConnection dataConnection, String dirList) {
        boolean success = false;

        try {
            dataConnection.transferToClient(ftpIoSession.getFtpletSession(), dirList);
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

    /**
     * Read the data from InputStream and transfer to client.
     *
     * @return the number of bytes transferred
     */
    private long transferDataToClient(final FtpSession ftpSession,
                                      final DataConnection dataConnection,
                                      final InputStream is)
            throws IOException {
        long length = 0L;

        try {
            logger.log(Level.FINE, "Starting data transfer");

            length = dataConnection.transferToClient(ftpSession, is);

            logger.log(Level.FINE, "Completed data transfer");
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error transferring data to client", e);
            length = -1;
        } finally {
            ResourceUtils.closeQuietly(is);
        }

        return length;
    }

    private boolean containListCommand(FtpMethod ftpMethod) {
        String[] listCommands = new String[] { // TODO jwilliams: handle better in the refactored FtpMethod/FtpMethodEnum
                FtpMethod.FTP_LIST.getWspName(),
                FtpMethod.FTP_MLSD.getWspName(),
//                FtpMethod.FTP_MLST.getWspName(), // TODO jwilliams: this is a list command, but doesn't use a data connection and returns info about a single file only - what to do?
                FtpMethod.FTP_NLST.getWspName()
        };

        return Arrays.asList(listCommands).contains(ftpMethod.getWspName());
    }
}
