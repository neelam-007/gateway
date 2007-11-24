package com.l7tech.server.transport.ftp;

import com.l7tech.cluster.ClusterPropertyManager;
import com.l7tech.common.message.FtpRequestKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.SoapFaultManager;
import org.apache.ftpserver.DefaultFtpReply;
import org.apache.ftpserver.ServerDataConnectionFactory;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.interfaces.FtpServerSession;
import org.apache.ftpserver.listener.AbstractListener;
import org.springframework.context.ApplicationContext;

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

    /**
     * Bean constructor
     */
    public MessageProcessingFtplet(final ApplicationContext applicationContext,
                                   final FtpServerManager ftpServerManager,
                                   final MessageProcessor messageProcessor,
                                   final AuditContext auditContext,
                                   final SoapFaultManager soapFaultManager,
                                   final ClusterPropertyManager clusterPropertyManager,
                                   final StashManagerFactory stashManagerFactory) {
        this.applicationContext = applicationContext;
        this.ftpServerManager = ftpServerManager;
        this.messageProcessor = messageProcessor;
        this.auditContext = auditContext;
        this.soapFaultManager = soapFaultManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.stashManagerFactory = stashManagerFactory;
    }

    /**
     * Ensure that on initial connection the data connection is secure if the control connection is.
     */
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
    public FtpletEnum onSite(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        ftpReplyOutput.write(new DefaultFtpReply(FtpReply.REPLY_502_COMMAND_NOT_IMPLEMENTED, "Command SITE not implemented for " + ftpRequest.getArgument()));
        return FtpletEnum.RET_SKIP;
    }

    /**
     * Redirect uploads to the message processor
     */
    public FtpletEnum onUploadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleUploadStart(ftpSession, ftpRequest, ftpReplyOutput, false);
    }

    /**
     * Redirect uploads to the message processor
     */
    public FtpletEnum onUploadUniqueStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput) throws FtpException, IOException {
        return handleUploadStart(ftpSession, ftpRequest, ftpReplyOutput, true);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(MessageProcessingFtplet.class.getName());

    private static final int STORE_RESULT_OK = 0;
    private static final int STORE_RESULT_FAULT = 1;
    private static final int STORE_RESULT_DROP = 2;

    private final ApplicationContext applicationContext;
    private final FtpServerManager ftpServerManager;
    private final MessageProcessor messageProcessor;
    private final AuditContext auditContext;
    private final SoapFaultManager soapFaultManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final StashManagerFactory stashManagerFactory;

    /**
     * Process a file upload 
     */
    private FtpletEnum handleUploadStart(FtpSession ftpSession, FtpRequest ftpRequest, FtpReplyOutput ftpReplyOutput, boolean unique) throws FtpException, IOException {
        FtpletEnum result = FtpletEnum.RET_SKIP;
        String fileName = ftpRequest.getArgument();

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Handling STOR for file ''{0}'' (unique:{1}).", new Object[]{fileName, Boolean.valueOf(unique)});

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

                            int storeResult = onStore(dataConnection, ftpSession, message, user, path, file, secure, unique);

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
                        new Object[] {Boolean.valueOf(controlSecure), Boolean.valueOf(dataSecure)});

            secure = dataSecure && controlSecure;
        }
        return secure;
    }

    /**
     * Store to message processor 
     */
    private int onStore( final DataConnection dataConnection,
                         final FtpSession ftpSession,
                         final String[] messageHolder, 
                         final User user,
                         final String path,
                         final String file,
                         final boolean secure,
                         final boolean unique) throws IOException {
        int storeResult = STORE_RESULT_FAULT;

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Processing STOR for path ''{0}'' and file ''{1}''.", new String[]{path, file});
        
        // Create request message
        Message request = null;
        try {
            ContentTypeHeader ctype = ContentTypeHeader.XML_DEFAULT;
            Message requestMessage = new Message();
            requestMessage.initialize(stashManagerFactory.createStashManager(), ctype, getDataInputStream(dataConnection, buildUri(path, file)));
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
        } catch (NoSuchPartException nspe) {
            messageHolder[0] = "Invalid multipart data.";
        }

        // process request message
        if (request != null) {
            final PolicyEnforcementContext context = new PolicyEnforcementContext(request, new Message());

            AssertionStatus status = AssertionStatus.UNDEFINED;
            String faultXml = null;

            try {
                context.setAuditContext(auditContext);
                context.setSoapFaultManager(soapFaultManager);
                context.setClusterPropertyManager(clusterPropertyManager);
                context.setReplyExpected(false);

                try {
                    status = messageProcessor.processMessage(context);

                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);

                } catch ( PolicyVersionException pve ) {
                    logger.log( Level.INFO, "Request referred to an outdated version of policy" );
                    faultXml = soapFaultManager.constructExceptionFault(pve, context);
                } catch ( Throwable t ) {
                    logger.log( Level.WARNING, "Exception while processing FTP message", t );
                    faultXml = soapFaultManager.constructExceptionFault(t, context);
                }

                if ( status != AssertionStatus.NONE ) {
                    faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context);
                }

                if (faultXml != null)
                    applicationContext.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));

                if (!context.isStealthResponseMode()) {
                    if (status == AssertionStatus.NONE) {
                        storeResult = STORE_RESULT_OK;
                    }
                }
                else {
                    storeResult = STORE_RESULT_DROP;
                }
            } finally {
                try {
                    auditContext.flush();
                }
                finally {
                    if (context != null) {
                        try {
                            context.close();
                        } catch (Throwable t) {
                            logger.log(Level.SEVERE, "Error closing context.", t);
                        }
                    }
                }
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

    /**
     * Create an FtpKnob for the given info. 
     */
    private FtpRequestKnob buildFtpKnob(final InetAddress serverAddress, final int port, final InetAddress clientAddress, final String file, final String path, final boolean secure, final boolean unique, final User user) {
        return new FtpRequestKnob(){
            public int getLocalPort() {
                return port;
            }
            public String getRemoteAddress() {
                return clientAddress.getHostAddress();
            }
            public String getRemoteHost() {
                return clientAddress.getHostAddress();
            }
            public String getFile() {
                return file;
            }
            public String getPath() {
                return path;
            }
            public String getRequestUri() {
                return path;
            }
            public String getRequestUrl() {
                StringBuffer urlBuffer = new StringBuffer();

                urlBuffer.append(secure ? "ftps" : "ftp");
                urlBuffer.append("://");
                urlBuffer.append(serverAddress.getHostAddress());
                urlBuffer.append(":");
                urlBuffer.append(port);
                urlBuffer.append(path);
                if (!path.endsWith("/"))
                    urlBuffer.append("/");
                urlBuffer.append(file);

                return urlBuffer.toString();
            }
            public boolean isSecure() {
                return secure;
            }
            public boolean isUnique() {
                return unique;
            }
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

    /**
     * Convert OutputStream to InputStream.
     */
    private InputStream getDataInputStream(final DataConnection dataConnection,
                                           final String fullPath) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable(){
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
