package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.*;
import com.l7tech.objectmodel.FindException;
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
import org.apache.ftpserver.DefaultFtpReply;
import org.apache.ftpserver.ServerDataConnectionFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.interfaces.FtpServerSession;
import org.apache.ftpserver.listener.AbstractListener;
import org.w3c.dom.Document;

import java.io.*;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nilic
 * @author jwilliams
 */
public class MessageProcessingFtpletSubsystem extends DefaultFtplet {

    //- PUBLIC

    /*
     * Bean constructor
     */
    public MessageProcessingFtpletSubsystem( final FtpServerModule ftpServerModule,
                                      final MessageProcessor messageProcessor,
                                      final SoapFaultManager soapFaultManager,
                                      final StashManagerFactory stashManagerFactory,
                                      final EventChannel messageProcessingEventChannel,
                                      final ContentTypeHeader overriddenContentType,
                                      final long hardwiredServiceOid,
                                      final long maxRequestSize,
                                      final long connectorId,
                                      final ServiceManager serviceManager,
                                      final String initServiceUri) {
        this.ftpServerModule = ftpServerModule;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceOid = hardwiredServiceOid;
        this.maxRequestSize = maxRequestSize;
        this.connectorId = connectorId;
        this.serviceManager = serviceManager;
        this.initServiceUri = initServiceUri;
    }

    /**
     * Ensure that on initial connection the data connection is secure if the control connection is.
     */
    @Override
    public FtpletEnum onConnect(FtpSession ftpSession, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        DataConnectionFactory dataConnectionFactory = ftpSession.getDataConnection();
        if (dataConnectionFactory instanceof ServerDataConnectionFactory) {
            ServerDataConnectionFactory sdcf = (ServerDataConnectionFactory) dataConnectionFactory;

            boolean controlSecure = false;

            if (ftpSession instanceof FtpServerSession) {
                FtpServerSession ftpServerSession = (FtpServerSession) ftpSession;
                controlSecure = ((AbstractListener)ftpServerSession.getListener()).isImplicitSsl();
            }

            // init data connection security to the same as the control connection
            sdcf.setSecure( controlSecure );
        }

        return super.onConnect(ftpSession, ftpReplyOutput);
    }

    /**
     * Override the default SITE extensions.
     */
    @Override
    public FtpletEnum onSite(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleCommandStart(ftpSession, ftpRequest, ftpReplyOutput, false, FtpMethod.FTP_SITE);
    }

    /**
     * Redirect uploads to the message processor
     */
    @Override
    public FtpletEnum onUploadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleTransportStart(ftpSession, ftpRequest, ftpReplyOutput, false, FtpMethod.FTP_PUT);
    }

    /**
     * Redirect uploads to the message processor
     */
    @Override
    public FtpletEnum onUploadUniqueStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleTransportStart(ftpSession, ftpRequest, ftpReplyOutput, true, FtpMethod.FTP_PUT);
    }

    @Override
    public FtpletEnum onDownloadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleTransportStart(ftpSession, ftpRequest, ftpReplyOutput, false, FtpMethod.FTP_GET);
    }

    @Override
    public FtpletEnum onDeleteStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleCommandStart(ftpSession, ftpRequest, ftpReplyOutput, false, FtpMethod.FTP_DELE);
    }
    @Override
    public FtpletEnum onLogin(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException  {
        return handleLogin(ftpSession, ftpRequest, ftpReplyOutput, false);
    }

    public FtpletEnum onListStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, FtpMethod method) throws FtpException, IOException {
        return handleTransportStart(ftpSession, ftpRequest, ftpReplyOutput, false, method);
    }

    public FtpletEnum onCommandStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, FtpMethod ftpMethod) throws FtpException, IOException{
        return handleCommandStart(ftpSession, ftpRequest, ftpReplyOutput, false, ftpMethod);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MessageProcessingFtpletSubsystem.class.getName());

    private static final int STORE_RESULT_OK = 0;
    private static final int STORE_RESULT_FAULT = 1;
    private static final int STORE_RESULT_DROP = 2;

    private final FtpServerModule ftpServerModule;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ContentTypeHeader overriddenContentType;
    private final long hardwiredServiceOid;
    private final long maxRequestSize;
    private final long connectorId;
    private ServiceManager serviceManager;
    private String initServiceUri;

    private FtpletEnum handleTransportStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, boolean unique, FtpMethod ftpMethod) throws FtpException, IOException {

        FtpletEnum result = FtpletEnum.RET_SKIP;
        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});
        }

        if (!ftpServerModule.isLicensed()) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing (FTP server not licensed).");

            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));
        }
        else  {
                DataConnectionFactory dataConnectionFactory = null;
                HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, Long.toString( connectorId ) );
                HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getHostAddress());
                try {

                    dataConnectionFactory = ftpSession.getDataConnection();
                    DataConnection dataConnection = null;
                    try {
                        dataConnection = dataConnectionFactory.openConnection();
                    }
                    catch(Exception ex) {
                        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_425_CANT_OPEN_DATA_CONNECTION, "Can't open data connection."));
                    }

                    if (dataConnection != null) {
                        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
                        // transfer data
                        try {
                            String[] message = {"Failed."};
                            User user = ftpSession.getUser();
                            String path = ftpSession.getFileSystemView().getCurrentDirectory().getFullName();
                            if (initServiceUri != null && path.equals("/")){
                                ftpSession.getFileSystemView().changeDirectory(initServiceUri);
                                path = ftpSession.getFileSystemView().getCurrentDirectory().getFullName();
                            }
                            String directory = ((VirtualFileSystem) ftpSession.getFileSystemView()).getChangedDirectory();

                            boolean secure = isSecure(dataConnectionFactory, ftpSession);

                            if (unique) {
                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, fileName + ": Transfer started."));
                            }
                            int storeResult = onStore(dataConnection, ftpSession, user, path, fileName, secure, unique, ftpMethod, directory, ftpReplyOutput);

                            if ( storeResult == STORE_RESULT_DROP ) {
                                result = FtpletEnum.RET_DISCONNECT;
                            } else if ( storeResult == STORE_RESULT_FAULT ) {
                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, fileName + ": " + message[0]));
                            } else {
                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer complete."));
                            }
                        }
                        catch(IOException ioe) {
                            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED, "Data connection error."));
                        }
                    }
                }
                finally {
                    HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
                    HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
                    if (dataConnectionFactory !=null) dataConnectionFactory.closeDataConnection();
                }
        }
        return result;
    }

    /*
    * Process a file command
    */
    private FtpletEnum handleCommandStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, boolean unique, FtpMethod ftpMethod) throws FtpException, IOException {
        FtpletEnum result = FtpletEnum.RET_SKIP;
        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling " + ftpMethod.getWspName() + " for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});

        if (!ftpServerModule.isLicensed()) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing " + ftpMethod.getWspName() + " (FTP server not licensed).");

            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));
        } else {
            HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, Long.toString( connectorId ) );
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getHostAddress());

            User user = ftpSession.getUser();
            String file = ftpRequest.getArgument();

            if (initServiceUri != null){
                ftpSession.getFileSystemView().changeDirectory(initServiceUri);
            }

            String path = ftpSession.getFileSystemView().getCurrentDirectory().getFullName();
            VirtualFileSystem vfs = (VirtualFileSystem)ftpSession.getFileSystemView();
            String directory;
            String previousDirectory = vfs.getChangedDirectory();

            if (ftpMethod == FtpMethod.FTP_CWD) {
                String uri = file;
                if (!file.startsWith("/")){
                    uri = "/" + file;
                }
                if (isService(uri)){
                    file = uri;
                    ftpSession.getFileSystemView().changeDirectory(file);
                    path = ftpSession.getFileSystemView().getCurrentDirectory().getFullName();
                    directory = "/";
                } else if (file.startsWith("/")){
                    directory = file;
                    vfs.setChangedDirectory(file);
                } else if (file.equals("..") || file.equals("../")) {
                    directory = vfs.getParentDirectory();
                    vfs.setChangedDirectory(directory);
                } else if (file.equals(".")){
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
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
            }

            int storeResult = process(ftpSession, user, path, file, secure, unique, ftpMethod.getWspName(), ftpReplyOutput, directory);

            if ( storeResult == STORE_RESULT_DROP ) {
                result = FtpletEnum.RET_DISCONNECT;
            } else if ( storeResult == STORE_RESULT_FAULT ) {
                if (ftpMethod == FtpMethod.FTP_CWD){
                    vfs.setChangedDirectory(previousDirectory);
                }
            }

            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
        }

        return result;
    }

    private FtpletEnum handleLogin(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, boolean unique) throws FtpException, IOException {

        FtpletEnum result = FtpletEnum.RET_SKIP;
        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling " + FtpMethod.FTP_LOGIN.getWspName() + " for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});

        if (!ftpServerModule.isLicensed()) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing " + FtpMethod.FTP_LOGIN.getWspName() + " (FTP server not licensed).");

            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));
        }
        else {
            HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, Long.toString( connectorId ) );
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getHostAddress());
            User user = ftpSession.getUser();
            String file = ftpRequest.getArgument();

            String path = initServiceUri;

            boolean secure = isSecureSession(ftpSession);

            if (unique) {
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
            }

            int storeResult = process(ftpSession, user, path, file, secure, unique, FtpMethod.FTP_LOGIN.getWspName(), ftpReplyOutput, "/");

            if ( storeResult == STORE_RESULT_FAULT ) {
                result = FtpletEnum.RET_DISCONNECT;
            } else
            {
                result = null;
            }

            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
        }

        return result;
    }

    /**
     * Check if the session is secure.
     *
     * <p>To be secure both the control and data connections must be secured.</p>
     *
     * <p>NOTE: This will NOT WORK for explicit FTP, which is currently fine
     * since that is not enabled.</p>
     * @param dataConnectionFactory connection factory to check whether data connection is secure
     * @param ftpSession ftp session to check whether control connection is secure
     * @return whether the connection is secure
     */
    private boolean isSecure(DataConnectionFactory dataConnectionFactory, FtpSession ftpSession) {
        boolean secure = false;

        if (dataConnectionFactory instanceof ServerDataConnectionFactory) {
            ServerDataConnectionFactory sdcf = (ServerDataConnectionFactory) dataConnectionFactory;
            boolean dataSecure = sdcf.isSecure();
            boolean controlSecure = false;

            if (ftpSession instanceof FtpServerSession) {
                FtpServerSession ftpServerSession = (FtpServerSession) ftpSession;
                controlSecure = ((AbstractListener)ftpServerSession.getListener()).isImplicitSsl();
            }

            if (logger.isLoggable(Level.FINE))
                logger.log(Level.INFO, "Security levels, control secure ''{0}'', data secure ''{1}''.",
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
     * @return whether the connection is secure
     */
    private boolean isSecureSession(FtpSession ftpSession) {

        boolean controlSecure = false;

        if (ftpSession instanceof FtpServerSession) {
            FtpServerSession ftpServerSession = (FtpServerSession) ftpSession;
            controlSecure = ((AbstractListener)ftpServerSession.getListener()).isImplicitSsl();
        }

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.INFO, "Security levels, control secure ''{0}'', data secure ''{1}''.",
                    new Object[] {controlSecure});

        return controlSecure;
    }

    /*
     * Store to message processor
     */
    private int onStore(final DataConnection dataConnection,
                        final FtpSession ftpSession,
                        final User user,
                        final String path,
                        final String fileName,
                        final boolean secure,
                        final boolean unique,
                        final FtpMethod ftpMethod,
                        final String directory,
                        final FtpReplyOutput ftpReplyOutput) throws IOException {
        int storeResult = STORE_RESULT_FAULT;

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[]{path, fileName});

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() :maxRequestSize;

        // Create request message
        Message request;
        ContentTypeHeader ctype = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
        Message requestMessage = new Message();
        Message response = new Message();

        if (ftpMethod == FtpMethod.FTP_PUT || ftpMethod == FtpMethod.FTP_APPE) {
            requestMessage.initialize(stashManagerFactory.createStashManager(), ctype, getDataInputStream(dataConnection, buildUri(path, fileName)), maxSize);
        } else {
            requestMessage.initialize(stashManagerFactory.createStashManager(), ctype, new ByteArrayInputStream(new byte[0]), maxSize);
        }

        requestMessage.attachFtpKnob(buildFtpKnob(
                ftpSession.getServerAddress(),
                ftpSession.getServerPort(),
                ftpSession.getClientAddress(),
                fileName,
                path,
                secure,
                unique,
                user));
        request = requestMessage;

        if (hardwiredServiceOid != -1) {
            requestMessage.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
        }

        // process request message
        if (request != null) {
            final PolicyEnforcementContext context =
                    PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            //set the context variable to check the ftp method
            context.setVariable("ftp.command", ftpMethod.getWspName());
            context.setVariable("ftp.directory", directory);

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
                    logger.log( Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException( t ) );
                    faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                }

                if ( status != AssertionStatus.NONE ) {
                    faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                }

                if (faultXml != null)
                    messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

                if (!context.isStealthResponseMode() && status == AssertionStatus.NONE) {
                    storeResult = STORE_RESULT_OK;
                    if (ftpMethod != FtpMethod.FTP_GET && !containListCommand(ftpMethod)){
                        logger.log(Level.INFO, "FTP " + ftpMethod.getWspName() + " completed");
                    }
                    else {
                        Message responseContext = context.getResponse();
                        if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
                            try{
                                InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();
                                if (responseStream != null) {
                                    if (ftpMethod == FtpMethod.FTP_GET){
                                        if (status == AssertionStatus.NONE){
                                            long readLength = readFile(dataConnection, responseStream);
                                            if (readLength < 0){
                                                logger.log(Level.WARNING, "Error during reading the file");
                                                storeResult = STORE_RESULT_FAULT;
                                            }
                                        } else {
                                            logger.log(Level.WARNING, "Error during reading the file");
                                            storeResult = STORE_RESULT_FAULT;
                                        }
                                    } else if (containListCommand(ftpMethod)){
                                        FtpListUtil ftpListUtil = new FtpListUtil(responseStream);
                                        String responseMessage = ftpListUtil.writeMessageToOutput();
                                        if (!responseMessage.isEmpty()){
                                            boolean failure;
                                            String rawMessage = responseMessage;
                                            if (ftpMethod != FtpMethod.FTP_NLST){
                                                Document doc = FtpListUtil.createDoc(responseMessage);
                                                if ( doc != null){
                                                    rawMessage = FtpListUtil.getRawListData(doc);
                                                }
                                            }
                                            failure = listFiles(dataConnection, rawMessage);
                                            if (failure) {
                                                logger.log(Level.WARNING, "Error during list FTP");
                                                storeResult =  STORE_RESULT_FAULT;
                                            }
                                        }
                                    }
                                } else {
                                    logger.log(Level.WARNING, "Error during processing FTP");
                                    storeResult =  STORE_RESULT_FAULT;
                                }
                            } catch(NoSuchPartException nsp){
                                logger.log(Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException( nsp ));
                                storeResult =  STORE_RESULT_FAULT;
                            }
                        } else  {
                            logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
                            storeResult =  STORE_RESULT_FAULT;
                        }
                    }
                }
                else {
                    logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                    storeResult = STORE_RESULT_FAULT;
                }
            } finally {
                ResourceUtils.closeQuietly(context);
            }
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
                        final FtpReplyOutput ftpReplyOutput,
                        final String directory) throws IOException {
        int storeResult = STORE_RESULT_FAULT;

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Processing " + command + " for path ''{0}'' and file ''{1}''.", new String[]{path, file});

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() : maxRequestSize;

        // Create request message
        Message request;
        ContentTypeHeader cType = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
        Message requestMessage = new Message();
        Message response = new Message();

        requestMessage.initialize(stashManagerFactory.createStashManager(), cType, new ByteArrayInputStream(new byte[0]), maxSize);

        requestMessage.attachFtpKnob(buildFtpKnob(
                ftpSession.getServerAddress(),
                ftpSession.getServerPort(),
                ftpSession.getClientAddress(),
                file,
                path,
                secure,
                unique,
                user));
        request = requestMessage;

        if (hardwiredServiceOid != -1) {
            requestMessage.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
        }

        // process request message
        if (request != null) {
            final PolicyEnforcementContext context =
                    PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);

            //set the context variable to check the ftp method
            context.setVariable("ftp.command", command);
            context.setVariable("ftp.directory", directory);

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
                    logger.log( Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException( t ) );
                    faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                }

                if ( status != AssertionStatus.NONE ) {
                    faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                }

                if (faultXml != null)
                    messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

                if (!context.isStealthResponseMode()){
                    String responseMessage = null;
                    if (status == AssertionStatus.NONE){
                        storeResult = STORE_RESULT_OK;
                        logger.log(Level.INFO, "FTP " + command + " completed");
                    } else  {
                        storeResult =  STORE_RESULT_FAULT;
                    }

                    Message responseContext = context.getResponse();
                    if (responseContext.getKnob(MimeKnob.class) != null && responseContext.isInitialized()) {
                        try{
                            InputStream responseStream = responseContext.getMimeKnob().getEntireMessageBodyAsInputStream();
                            if (responseStream != null) {
                                    FtpListUtil ftpListUtil = new FtpListUtil(responseStream);
                                    responseMessage = ftpListUtil.writeMessageToOutput();
                                    createReplyOutput(command, ftpReplyOutput, responseMessage, directory, storeResult);
                                }
                            } catch(NoSuchPartException nsp){
                            logger.log(Level.WARNING, "Exception while processing FTP message: "+ ExceptionUtils.getMessage(nsp), ExceptionUtils.getDebugException( nsp ));
                            storeResult =  STORE_RESULT_FAULT;
                            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                        }
                    } else if (!command.equals("APPE") && !command.equals("DELE")){
                        logger.log(Level.WARNING, "Error during processing FTP. Response is not initialized");
                        storeResult =  STORE_RESULT_FAULT;
                        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                    } else {
                        if (status == AssertionStatus.NONE){
                            createReplyOutput(command, ftpReplyOutput, responseMessage, directory, storeResult);
                        } else {
                            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                        }
                    }

                }
                else {
                    logger.log(Level.WARNING, "Error during processing FTP. Context is empty");
                    storeResult = STORE_RESULT_FAULT;
                    ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Error in FTP(S) " + command + " Request."));
                }
            } finally {
                ResourceUtils.closeQuietly(context);
            }
        }

        return storeResult;
    }


    private String buildUri(String path, String file) {
        String uri = path;

        if (!uri.endsWith("/")) {
            uri += "/";
        }

        if (file.startsWith("/")){
            file = file.substring(file.indexOf("/") + 1, file.length());
        }

        uri += file;

        return uri;
    }

    private void createReplyOutput(final String command, final FtpReplyOutput ftpReplyOutput, final String responseMessage, String directory, int storeResult) throws IOException {

        if (directory != null && !directory.startsWith("/")) {
            directory = "/" + directory;
        }

        if (storeResult ==  STORE_RESULT_OK &&  !command.equals(FtpMethod.FTP_LOGIN.getWspName())){
            if (command.equals(FtpMethod.FTP_APPE.getWspName())) {
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_226_CLOSING_DATA_CONNECTION, "Transfer OK"));
            } else if (command.equals(FtpMethod.FTP_CDUP.getWspName())) {
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY,  "CDUP successful. \"" + directory + "\" is current directory."));
            } else if (command.equals(FtpMethod.FTP_PWD.getWspName())){
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_257_PATHNAME_CREATED, "\"" + directory + "\" is current directory."));
            } else if (command.equals(FtpMethod.FTP_MDTM.getWspName())){
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_213_FILE_STATUS, org.apache.ftpserver.util.DateUtils.getFtpDate(Long.parseLong(responseMessage))));
            } else if (command.equals(FtpMethod.FTP_SIZE.getWspName())){
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
            } else {
                if (responseMessage != null && !responseMessage.isEmpty()){
                        int result = createReplyMessage(ftpReplyOutput, responseMessage);
                    if (result == -1) {
                        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, responseMessage));
                    }
                } else {
                    ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_200_COMMAND_OKAY, "OK"));
                }
            }
        } else {
            if (command.equals(FtpMethod.FTP_LOGIN.getWspName())) {
                createReplyMessage(ftpReplyOutput, responseMessage);
            } else {
                if (responseMessage != null && !responseMessage.isEmpty()){
                    int result = createReplyMessage(ftpReplyOutput, responseMessage);
                    if (result == -1){
                        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, responseMessage));
                    }
                } else {
                    ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN, "Requested action not taken"));
                }
            }
        }
    }

    private int createReplyMessage(final FtpReplyOutput ftpReplyOutput, final String responseMessage) throws IOException {

        try{
            int replyCode = Integer.parseInt(responseMessage.substring(0, 3));
            String replyMessage = responseMessage.substring(4, responseMessage.length());
            if (replyMessage.endsWith("\r\n")){
                replyMessage = replyMessage.substring(0, replyMessage.indexOf("\r\n"));
            }
            ftpReplyOutput.write(new DefaultFtpReply(replyCode, replyMessage));
        } catch(NumberFormatException ne){
            return -1;
        } catch (StringIndexOutOfBoundsException sie){
            return -1;
        }

        return 0;
    }

    private boolean isService(String uri){
        boolean isService = false;
        int countService = 0;

        try{
            Collection<PublishedService> publishedServices = serviceManager.findByRoutingUri(uri) ;
                for (PublishedService publishedService : publishedServices){
                if (uri.equals(publishedService.getRoutingUri()) && !publishedService.isDisabled()) {
                    countService++;
                }
            }
        } catch (FindException fe) {
            return false;
        }

        if (countService == 0){
            logger.log(Level.WARNING, "There is no service with uri " + uri);
        } else if (countService == 1){
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
        return new FtpRequestKnob(){
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
                StringBuilder urlBuffer = new StringBuilder();

                urlBuffer.append(secure ? "ftps" : "ftp");
                urlBuffer.append("://");
                urlBuffer.append(InetAddressUtil.getHostForUrl(serverAddress.getHostAddress()));
                urlBuffer.append(":");
                urlBuffer.append(port);
                urlBuffer.append(path);
                if (!path.endsWith("/"))
                    urlBuffer.append("/");
                urlBuffer.append(file);

                return urlBuffer.toString();
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
                if (user.getPassword()!=null) {
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
    private InputStream getDataInputStream(final DataConnection dataConnection,
                                           final String fullPath) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                PipedOutputStream pos = null;
                try {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", fullPath);

                    //noinspection IOResourceOpenedButNotSafelyClosed
                    pos = new PipedOutputStream(pis);
                    startedSignal.countDown();
                    dataConnection.transferFromClient(pos);

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

    private boolean listFiles(final DataConnection dataConnection, String dirList){

        boolean failure = false;

        try {
            dataConnection.transferToClient(dirList);
        } catch (SocketException ex) {
            logger.log(Level.WARNING, "Socket exception during list transfer", ex);
            failure = true;
        } catch (IOException ex) {
            logger.log(Level.WARNING, "IOException during list transfer", ex);
            failure = true;
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Illegal list syntax: ", e);
            failure = true;
        }

        return failure;
    }

    /*
    * Read the file from InputStream
    */
    private long readFile(final DataConnection dataConnection, final InputStream is ) throws IOException {

        long length = 0L;

        try {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Starting data transfer");
            length = dataConnection.transferToClient(is);
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Completed data transfer");
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Data transfer error", ioe);
            length = -1;
        }
        finally {
            ResourceUtils.closeQuietly(is);
        }
        return length;
    }

    private boolean containListCommand(FtpMethod ftpMethod){

        String [] listCommands = new String[] { FtpMethod.FTP_LIST.getWspName(),
                                                FtpMethod.FTP_MDTM.getWspName(),
                                                FtpMethod.FTP_MLSD.getWspName(),
                                                FtpMethod.FTP_NLST.getWspName()};

        return Arrays.asList(listCommands).contains(ftpMethod.getWspName());
    }

}


