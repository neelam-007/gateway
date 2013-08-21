package com.l7tech.server.transport.ftp;

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
import org.apache.ftpserver.DefaultFtpReply;
import org.apache.ftpserver.ServerDataConnectionFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.interfaces.FtpServerSession;
import org.apache.ftpserver.listener.AbstractListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ftplet implementation backed by our MessageProcessor.
 *
 * @author Steve Jones
 */
class MessageProcessingFtplet extends DefaultFtplet {

    //- PUBLIC

    /*
     * Bean constructor
     */
    MessageProcessingFtplet( final FtpServerManager ftpServerManager,
                             final MessageProcessor messageProcessor,
                             final SoapFaultManager soapFaultManager,
                             final StashManagerFactory stashManagerFactory,
                             final EventChannel messageProcessingEventChannel,
                             final ContentTypeHeader overriddenContentType,
                             final Goid hardwiredServiceGoid,
                             final long maxRequestSize,
                             final Goid connectorGoid ) {
        this.ftpServerManager = ftpServerManager;
        this.messageProcessor = messageProcessor;
        this.soapFaultManager = soapFaultManager;
        this.stashManagerFactory = stashManagerFactory;
        this.messageProcessingEventChannel = messageProcessingEventChannel;
        this.overriddenContentType = overriddenContentType;
        this.hardwiredServiceGoid = hardwiredServiceGoid;
        this.maxRequestSize = maxRequestSize;
        this.connectorGoid = connectorGoid;
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
        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_502_COMMAND_NOT_IMPLEMENTED, "Command SITE not implemented for " + ftpRequest.getArgument()));
        return FtpletEnum.RET_SKIP;
    }

    /**
     * Redirect uploads to the message processor
     */
    @Override
    public FtpletEnum onUploadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleUploadStart(ftpSession, ftpRequest, ftpReplyOutput, false);
    }

    /**
     * Redirect uploads to the message processor
     */
    @Override
    public FtpletEnum onUploadUniqueStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleUploadStart(ftpSession, ftpRequest, ftpReplyOutput, true);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MessageProcessingFtplet.class.getName());

    private static final int STORE_RESULT_OK = 0;
    private static final int STORE_RESULT_FAULT = 1;
    private static final int STORE_RESULT_DROP = 2;

    private final FtpServerManager ftpServerManager;
    private final MessageProcessor messageProcessor;
    private final SoapFaultManager soapFaultManager;
    private final StashManagerFactory stashManagerFactory;
    private final EventChannel messageProcessingEventChannel;
    private final ContentTypeHeader overriddenContentType;
    private final Goid hardwiredServiceGoid;
    private final long maxRequestSize;
    private final Goid connectorGoid;

    /*
     * Process a file upload 
     */
    private FtpletEnum handleUploadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, boolean unique) throws FtpException, IOException {
        FtpletEnum result = FtpletEnum.RET_SKIP;
        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling STOR for file ''{0}'' (unique:{1}).", new Object[]{fileName, unique});

        if (!ftpServerManager.isLicensed()) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Failing STOR (FTP server not licensed).");

            ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                    "Service not available (not licensed)."));
        }
        else {
            if (!ftpSession.getDataType().equals(DataType.BINARY)) {
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_550_REQUESTED_ACTION_NOT_TAKEN,
                        "Type '"+ftpSession.getDataType().toString()+"' not supported for this action."));
            }
            else {
                // request data
                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_150_FILE_STATUS_OKAY, "File status okay; about to open data connection."));
                DataConnectionFactory dataConnectionFactory = null;
                HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connectorGoid.toString() );
                HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.CLIENT_IP, ftpSession.getClientAddress().getHostAddress() );
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
                        try {
                            String[] message = {"Failed."};
                            User user = ftpSession.getUser();
                            String file = ftpRequest.getArgument();
                            String path = ftpSession.getFileSystemView().getCurrentDirectory().getFullName();
                            boolean secure = isSecure(dataConnectionFactory, ftpSession);

                            if (unique) {
                                ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_250_REQUESTED_FILE_ACTION_OKAY, file + ": Transfer started."));
                            }

                            int storeResult = onStore(dataConnection, ftpSession, user, path, file, secure, unique);

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

    /*
     * Store to message processor
     */
    private int onStore(final DataConnection dataConnection,
                        final FtpSession ftpSession,
                        final User user,
                        final String path,
                        final String file,
                        final boolean secure,
                        final boolean unique) throws IOException {
        int storeResult = STORE_RESULT_FAULT;

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[]{path, file});

        long maxSize = maxRequestSize == -1 ? Message.getMaxBytes() :maxRequestSize;

        // Create request message
        Message request;
        ContentTypeHeader ctype = overriddenContentType != null ? overriddenContentType : ContentTypeHeader.XML_DEFAULT;
        Message requestMessage = new Message();
        requestMessage.initialize(stashManagerFactory.createStashManager(), ctype, getDataInputStream(dataConnection, buildUri(path, file)),maxSize);
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

        if (!Goid.isDefault(hardwiredServiceGoid)) {
            requestMessage.attachKnob(HasServiceId.class, new HasServiceIdImpl(hardwiredServiceGoid));
        }

        // process request message
        if (request != null) {
            final PolicyEnforcementContext context =
                    PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, true);

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

                if (!context.isStealthResponseMode()) {
                    if (status == AssertionStatus.NONE) {
                        storeResult = STORE_RESULT_OK;
                    }
                }
                else {
                    storeResult = STORE_RESULT_DROP;
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

        uri += file;

        return uri;
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
                StringBuffer urlBuffer = new StringBuffer();

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
}
