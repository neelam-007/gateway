package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.*;
import com.jscape.inet.ftps.Ftps;
import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.message.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.util.*;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.server.DefaultKey;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;

/**
 * Assertion that routes the request to an FTP server.
 *
 * @since SecureSpan 4.0
 * @author rmak
 * @author nilic
 * @author jwilliams
 */
public class ServerFtpRoutingAssertion extends ServerRoutingAssertion<FtpRoutingAssertion> {
    private static final Logger logger = Logger.getLogger(ServerRoutingAssertion.class.getName());

    // 150 - File status okay; about to open data connection
    private static final int REPLY_CODE_150 = 150;

    private static final Object assertionExecutorInitLock = new Object();
    private static volatile ExecutorService assertionExecutor;

    private final X509TrustManager trustManager;
    private final HostnameVerifier hostnameVerifier;
    private final DefaultKey keyFinder;
    private final String[] variablesUsed;

    private StashManagerFactory stashManagerFactory;

    public ServerFtpRoutingAssertion(FtpRoutingAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext);

        trustManager = applicationContext.getBean("routingTrustManager", X509TrustManager.class);
        hostnameVerifier = applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        keyFinder = applicationContext.getBean("defaultKey", DefaultKey.class);
        stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);

        variablesUsed = assertion.getVariablesUsed();

        // Initialize the executor if necessary
        if (assertionExecutor == null)
            initializeAssertionExecutor(applicationContext);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message sourceTarget = getSourceMessage(context);

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        if (null != sourceTarget) {
            try {
                handleProcessedSecurityHeader(sourceTarget);
            } catch (SAXException se) {
                logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
            }
        }

        final VariableExpander variableExpander = getVariableExpander(context);

        Pair<String, String> usernamePasswordPair = getCredentials(context, variableExpander);

        String arguments = getArguments(variableExpander);

        FtpCommand ftpCommand = getFtpCommand(context);

        final FtpClientWrapper ftpClient;

        // get ftp(s) client
        try {
            ftpClient = createFtpClient(usernamePasswordPair.left, usernamePasswordPair.right,
                    variableExpander, assertion.getSecurity());
        } catch (FtpException e) {
            logAndAudit(FTP_ROUTING_CONNECTION_ERROR,
                    new String[] {e.getMessage()}, getDebugException(e));
            return AssertionStatus.FAILED;
        }

        FtpReply ftpReply;

        try {
            switch (ftpCommand) {
                case APPE:
                case STOU:
                case STOR:
                    final InputStream messageBodyStream;

                    if (sourceTarget == null) {
                        // Uninitialized request
                        logAndAudit(EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
                        return AssertionStatus.BAD_REQUEST;
                    }

                    final MimeKnob mimeKnob = sourceTarget.getKnob(MimeKnob.class);

                    if (mimeKnob == null) {
                        // Uninitialized request
                        logAndAudit(EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
                        return AssertionStatus.BAD_REQUEST;
                    }

                    try {
                        messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream();
                    } catch (NoSuchPartException e) {
                        logAndAudit(NO_SUCH_PART,
                                new String[] {assertion.getRequestTarget().getTargetName(), e.getWhatWasMissing()},
                                ExceptionUtils.getDebugException(e));

                        return AssertionStatus.BAD_REQUEST;
                    }

                    ftpReply = routeUpload(ftpClient, messageBodyStream, ftpCommand, arguments);

                    break;
                case RETR:
                    ftpReply = routeDownload(ftpClient, arguments);
                    break;
                default:
                    ftpReply = routeOtherCommand(ftpClient, ftpCommand, arguments);
            }
        } catch (IOException e) {
            logAndAudit(FTP_ROUTING_ERROR,
                    new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (FtpRoutingException e) {
            // if there was a reply it must be evaluated to decide the assertion result
            ftpReply = e.getFtpReply();

            if (ftpReply.isNoReply()) {
                // no reply to evaluate so log the error, then fail immediately
                logAndAudit(FTP_ROUTING_FAILED_NO_REPLY,
                        new String[] {
                                ftpReply.getLastCommand(),
                                null != e.getMessage()
                                        ? e.getMessage()
                                        : ExceptionUtils.getMessage(e.getCause())
                        },
                        ExceptionUtils.getDebugException(e));

                return AssertionStatus.FAILED;
            } else {
                // just log the error, then let the reply be evaluated
                logAndAudit(FTP_ROUTING_ERROR,
                        new String[] {e.getMessage()}, ExceptionUtils.getDebugException(e));
            }
        }

        AssertionStatus status = AssertionStatus.NONE;

        if (ftpReply.isPositiveCompletion() || ftpReply.isDownloadStarted()) {
            // completed successfully, or a download task is running in the background
            createResponseMessage(context, ftpReply);

            logAndAudit(FTP_ROUTING_SUCCEEDED);
        } else if (ftpReply.isTransientError()) {
            if (FtpRoutingAssertion.FAIL_ON_TRANSIENT != assertion.getFailureMode()) {
                // succeed
                createResponseMessage(context, ftpReply);

                logAndAudit(FTP_ROUTING_SUCCEEDED_TRANSIENT_FAILURE,
                        ftpReply.getLastCommand(), Integer.toString(ftpReply.getReplyCode()));
            } else {
                // fail
                logAndAudit(FTP_ROUTING_FAILED_TRANSIENT_FAILURE,
                        ftpReply.getLastCommand(), Integer.toString(ftpReply.getReplyCode()));
                status = AssertionStatus.FAILED;
            }
        } else if (ftpReply.isPermanentError()) {
            if (FtpRoutingAssertion.FAIL_ON_NO_REPLY == assertion.getFailureMode()) {
                // succeed
                createResponseMessage(context, ftpReply);

                logAndAudit(FTP_ROUTING_SUCCEEDED_PERMANENT_FAILURE,
                        ftpReply.getLastCommand(), Integer.toString(ftpReply.getReplyCode()));
            } else {
                // fail
                logAndAudit(FTP_ROUTING_FAILED_PERMANENT_FAILURE,
                        ftpReply.getLastCommand(), Integer.toString(ftpReply.getReplyCode()));
                status = AssertionStatus.FAILED;
            }
        } else {
            // unsupported reply, or indication of an incomplete operation
            logAndAudit(FTP_ROUTING_FAILED_INVALID_REPLY,
                    ftpReply.getLastCommand(), Integer.toString(ftpReply.getReplyCode()));
            status = AssertionStatus.SERVER_ERROR;
        }

        return status;
    }

    private Message getSourceMessage(PolicyEnforcementContext context) {
        Message request = null;

        try {
            if (null != assertion.getRequestTarget()) {
                request = context.getTargetMessage(assertion.getRequestTarget());
            }
        } catch (NoSuchVariableException e) {
            logAndAudit(MESSAGE_TARGET_ERROR, e.getVariable(), getMessage(e));
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        return request;
    }

    private String getHostName(final VariableExpander variableExpander) {
        return variableExpander.expandVariables(assertion.getHostName());
    }

    private int getPort(final VariableExpander variableExpander) {
        try {
            return Integer.parseInt(variableExpander.expandVariables(assertion.getPort()));
        } catch (NumberFormatException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }
    }

    private FtpCommand getFtpCommand(PolicyEnforcementContext context) {
        FtpCommand ftpCommand;

        if (assertion.isCommandFromVariable()) {
            final Object value;
            String ftpCommandString;

            try {
                value = context.getVariable(assertion.getFtpCommandVariable());
            } catch (NoSuchVariableException e) {
                logAndAudit(NO_SUCH_VARIABLE_WARNING, assertion.getFtpCommandVariable());
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            }

            if (value instanceof String) {
                ftpCommandString = (String) value;

                if (ftpCommandString.trim().isEmpty()) {
                    // no command specified in variable
                    logAndAudit(FTP_ROUTING_NO_COMMAND);
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }

                try {
                    ftpCommand = FtpCommand.valueOf(ftpCommandString);
                } catch (IllegalArgumentException e) {
                    // command from variable was unrecognized/unsupported - cannot perform routing
                    logAndAudit(FTP_ROUTING_UNSUPPORTED_COMMAND, ftpCommandString);
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            } else {
                logAndAudit(EXCEPTION_WARNING_WITH_MORE_INFO,
                        "Unsupported FTP command variable type of " + value.getClass().getName());
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR);
            }
        } else if (null != assertion.getFtpCommand()) {
            ftpCommand = assertion.getFtpCommand();
        } else {
            // no command specified - should not be possible under normal properties dialog use
            logAndAudit(FTP_ROUTING_NO_COMMAND);
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }

        return ftpCommand;
    }

    private String getDirectory(final VariableExpander variableExpander) {
        return variableExpander.expandVariables(assertion.getDirectory());
    }

    private Pair<String, String> getCredentials(PolicyEnforcementContext context, VariableExpander variableExpander) {
        String userName = null;
        String password = null;

        if (assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();

            if (credentials != null) {
                userName = credentials.getName();
                password = new String(credentials.getCredentials());
            }

            if (userName == null) {
                logAndAudit(FTP_ROUTING_PASSTHRU_NO_USERNAME);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            userName = getUserName(variableExpander);

            try {
                password = getPassword(variableExpander);
            } catch (FindException e) {
                logAndAudit(FTP_ROUTING_UNABLE_TO_FIND_STORED_PASSWORD, e.getMessage());
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }

        return new Pair<>(userName, password);
    }

    private String getArguments(VariableExpander variableExpander) {
        return variableExpander.expandVariables(assertion.getArguments());
    }

    private String getUserName(final VariableExpander variableExpander) {
        return variableExpander.expandVariables(assertion.getUserName());
    }

    private String getPassword(final VariableExpander variableExpander) throws FindException {
        if(null != assertion.getPasswordGoid()) {
            return ServerVariables.getSecurePasswordByGoid(new LoggingAudit(logger), assertion.getPasswordGoid());
        } else {
            return assertion.isPasswordUsesContextVariables()
                   ? variableExpander.expandVariables(assertion.getPassword())
                   : assertion.getPassword();
        }
    }

    private FtpReply routeUpload(final FtpClientWrapper ftpClient, final InputStream is, final FtpCommand ftpCommand,
                                 final String filename) throws FtpRoutingException {
        final FtpReplyListener replyListener = new FtpReplyListener();

        try {
            ftpClient.addFtpListener(replyListener);

            switch (ftpCommand) {
                case STOR:
                case STOU: // due to library limitations, STOU commands are routed as though they were STOR commands
                    ftpClient.upload(is, filename);
                    break;
                case APPE:
                    ftpClient.upload(is, filename, true);
                    break;
                default:
                    // command not supported/implemented - cannot perform upload routing
                    logAndAudit(FTP_ROUTING_UNSUPPORTED_COMMAND, ftpCommand.toString());
                    throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        } catch (FtpException e) {
            throw new FtpRoutingException("Failed to process upload to '" + filename +
                    "': " + e.getMessage(), e, replyListener.getFtpReply());
        } finally {
            ftpClient.removeFtpListener(replyListener);

            // disconnect from remote FTP server
            ftpClient.disconnect();
        }

        return replyListener.getFtpReply();
    }

    private FtpReply routeDownload(final FtpClientWrapper ftpClient, final String fileToDownload) throws IOException, FtpRoutingException {
        // used to wait for download to start
        final CountDownLatch startSignal = new CountDownLatch(1);

        // holds any exceptions that may be thrown attempting to start the download
        final AtomicReference<FtpException> downloadException = new AtomicReference<>(null);

        // hold the details of the reply to the download command if one was returned
        final AtomicReference<FtpReply> downloadReply = new AtomicReference<>(null);

        // input stream that will be connected to the FTP download output stream
        final PipedInputStream pis = new PipedInputStream();

        // FTP download output stream
        final PipedOutputStream pos = new PipedOutputStream(pis);

        // start download task - it will run in the background after this assertion completes until the entire file has been received
        startFtpDownloadTask(pos, new Runnable() {
            @Override
            public void run() {
                final FtpReplyListener replyListener = new FtpReplyListener() {
                    @Override
                    public void responseReceived(FtpResponseEvent ftpResponseEvent) {
                        super.responseReceived(ftpResponseEvent);

                        if (getLastReplyCode() == REPLY_CODE_150) {
                            // data connection being opened
                            startSignal.countDown();
                        }
                    }
                };

                ftpClient.addFtpListener(replyListener);

                try {
                    ftpClient.download(pos, fileToDownload);
                } catch (FtpException e) {
                    logger.log(Level.WARNING, "FtpException thrown during download: " + e.getMessage());

                    // save the exception thrown trying to download the file
                    downloadException.set(e);
                } finally {
                    // remove the ftp reply listener
                    ftpClient.removeFtpListener(replyListener);

                    // disconnect from remote FTP server
                    ftpClient.disconnect();

                    // save the FTP reply details
                    FtpReply ftpErrorResult = replyListener.getFtpReply();
                    ftpErrorResult.setDataStream(pis);
                    downloadReply.set(ftpErrorResult);

                    // call countdown in case it hasn't been called already
                    startSignal.countDown();
                }
            }
        });

        // wait until the download operation has started
        try {
            startSignal.await();
        } catch (InterruptedException e) {
            throw new CausedIOException("Interrupted waiting for download to begin.", e);
        }

        // the reply details should have been set in the case of an error or very fast completion
        FtpReply ftpResult = downloadReply.get();

        // check if there was any error starting the download
        FtpException exceptionThrown = downloadException.get();

        if (exceptionThrown != null) {
            throw new FtpRoutingException("Failed to retrieve '" + fileToDownload +
                    "': " + exceptionThrown.getMessage(), exceptionThrown, ftpResult);
        }

        // if a download is ongoing then we should not expect the reply details to have been set
        if (null == ftpResult) {
            // create an FtpReply for the RETR command with reply code indicating data connection has been opened
            ftpResult = new FtpReply(REPLY_CODE_150, null, FtpCommand.RETR.toString());
            ftpResult.setDataStream(pis);
        }

        return ftpResult;
    }

    private FtpReply routeOtherCommand(final FtpClientWrapper ftpClient,
                                       final FtpCommand ftpCommand, final String arguments)
            throws FtpRoutingException, CausedIOException {
        InputStream is = null;

        final FtpReplyListener replyListener = new FtpReplyListener();

        try {
            ftpClient.addFtpListener(replyListener);

            switch (ftpCommand) {
                case DELE:
                    ftpClient.deleteFile(arguments);
                    break;
                case MKD:
                    ftpClient.makeDir(arguments);
                    break;
                case RMD:
                    ftpClient.deleteDir(arguments, false); // don't recursively delete directory contents
                    break;
                case NOOP:
                    ftpClient.noop();
                    break;
                case CWD:
                    ftpClient.setDir(arguments);
                    break;
                case CDUP:
                    ftpClient.setDirUp();
                    break;
                case PWD:
                    ftpClient.getDir();
                    break;
                case SIZE:
                    ftpClient.getFilesize(arguments);
                    break;
                case MDTM:
                    ftpClient.getFileTimestamp(arguments);
                    break;
                case MLST:
                    ftpClient.getMachineFileListing(arguments);
                    break;
                case MLSD:
                    Enumeration machineDirListingEnum = ftpClient.getMachineDirListing(arguments);
                    is = new ByteArrayInputStream(createRawListing(machineDirListingEnum).getBytes());
                    break;
                case LIST:
                    Enumeration dirListingEnum;

                    if (null == arguments || arguments.isEmpty()) {
                        dirListingEnum = ftpClient.getDirListing();
                    } else {
                        dirListingEnum = ftpClient.getDirListing(arguments);
                    }

                    is = new ByteArrayInputStream(createRawListing(dirListingEnum).getBytes());
                    break;
                case NLST:
                    Enumeration fileNames;

                    if (null == arguments || arguments.isEmpty()) {
                        fileNames = ftpClient.getNameListing();
                    } else {
                        fileNames = ftpClient.getNameListing(arguments);
                    }

                    StringBuilder sb = new StringBuilder();

                    while (fileNames.hasMoreElements()) {
                        String file = (String) fileNames.nextElement();
                        sb.append(file);
                        sb.append("\r\n");
                    }

                    is = new ByteArrayInputStream(sb.toString().getBytes());
                    break;
                default:
                    // command not supported/implemented - cannot perform routing
                    logAndAudit(FTP_ROUTING_UNSUPPORTED_COMMAND, ftpCommand.toString());
                    throw new AssertionStatusException(AssertionStatus.FAILED);
            }

            // if successful, get the FtpReply
            FtpReply ftpResult = replyListener.getFtpReply();
            ftpResult.setDataStream(is);

            return ftpResult;
        } catch (FtpException e) {
            throw new FtpRoutingException(e.getMessage(), e, replyListener.getFtpReply());
        } finally {
            ftpClient.removeFtpListener(replyListener);

            // disconnect from remote FTP server
            ftpClient.disconnect();
        }
    }

    private Message createResponseMessage(PolicyEnforcementContext context, FtpReply ftpResult) throws IOException {
        final Message response;

        try {
            response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        InputStream dataStream = null == ftpResult.getDataStream()
                ? new ByteArrayInputStream(new byte[0])
                : ftpResult.getDataStream();

        response.initialize(stashManagerFactory.createStashManager(),
                ContentTypeHeader.OCTET_STREAM_DEFAULT, dataStream, getResponseByteLimit(context));

        response.attachFtpResponseKnob(buildFtpResponseKnob(ftpResult));

        return response;
    }

    private long getResponseByteLimit(PolicyEnforcementContext context) {
        final Map<String,?> variables = context.getVariableMap(variablesUsed, getAudit());

        long byteLimit = Message.getMaxBytes();

        if (assertion.getResponseByteLimit() != null) {
            String byteLimitStr = ExpandVariables.process(assertion.getResponseByteLimit(), variables, getAudit());

            try {
                byteLimit = Long.parseLong(byteLimitStr);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Used default response byte limit: " + byteLimit + ".  " + getMessage(e),
                        getDebugException(e));
            }
        }

        return byteLimit;
    }

    /*
     * Download the given file on a new thread using the ExecutorService.
     */
    private void startFtpDownloadTask(final PipedOutputStream pos, final Runnable downloadFunction)
            throws CausedIOException {
        final CountDownLatch startedSignal = new CountDownLatch(1);

        assertionExecutor.submit(new Runnable() {
            public void run() {
                try {
                    startedSignal.countDown();
                    downloadFunction.run();
                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
            }
        });

        // wait until processing has started before continuing
        try {
            startedSignal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for download task to begin execution.", e);
        }
    }

    private static void initializeAssertionExecutor(ApplicationContext applicationContext) {
        Config config = applicationContext == null
                ? ConfigFactory.getCachedConfig()
                : applicationContext.getBean("serverConfig", ServerConfig.class);

        int globalMaxConcurrency =
                config.getIntProperty(FtpRoutingAssertion.SC_MAX_CONC, FtpRoutingAssertion.MAX_CONC_DEFAULT);
        int globalCoreConcurrency =
                config.getIntProperty(FtpRoutingAssertion.SC_CORE_CONC, FtpRoutingAssertion.MIN_CONC_DEFAULT);
        int globalMaxWorkQueue =
                config.getIntProperty(FtpRoutingAssertion.SC_MAX_QUEUE, FtpRoutingAssertion.MAX_QUEUE_DEFAULT);
        
        synchronized (assertionExecutorInitLock) {
            if (assertionExecutor == null) {
                assertionExecutor = createAssertionExecutor(globalMaxConcurrency, globalCoreConcurrency, globalMaxWorkQueue);
            }
        }
    }

    private static ThreadPoolExecutor createAssertionExecutor(int globalMaxConcurrency, int globalCoreConcurrency, int globalMaxWorkQueue) {
        BlockingQueue<Runnable> assertionQueue = new ArrayBlockingQueue<>(globalMaxWorkQueue, true);
        
        return new ThreadPoolExecutor(globalCoreConcurrency, globalMaxConcurrency, 5L * 60L, TimeUnit.SECONDS, assertionQueue, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Reset the executor limits.  This method is intended to be used only by unit tests.
     * <p/>
     * This will shut down the existing executor and create a new one in its place.
     * <p/>
     * Caller must ensure that no other threads call {@link #checkRequest} during this process.
     *
     * @param maxConc  new max concurrency.
     * @param coreConc new core concurrency.
     * @param maxQueue new max queue length.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static void resetAssertionExecutor(int maxConc, int coreConc, int maxQueue) {
        synchronized (assertionExecutorInitLock) {
            assertionExecutor.shutdown();
            assertionExecutor = createAssertionExecutor(maxConc, coreConc, maxQueue);
        }
    }

    /**
     * Returns a StringBuilder of CRLF-separated raw listing lines for each FtpFile in the enumeration.
     * There doesn't seem to be a way to get the original raw response from the server, unfortunately, so we have to
     * reconstruct it.
     */
    private static String createRawListing(Enumeration ftpFiles) {
        StringBuilder sb = new StringBuilder();

        while (ftpFiles.hasMoreElements()) {
            FtpFile file = (FtpFile) ftpFiles.nextElement();
            sb.append(file.getLine());
            sb.append("\r\n");
        }

        return sb.toString();
    }

    /*
    * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
    * that would otherwise keep our instances from getting collected.
    */
    @SuppressWarnings("UnusedDeclaration")
    public static void onModuleUnloaded() {
        logger.log(Level.INFO, "ServerFtpRoutingAssertion is preparing itself to be unloaded; shutting down assertion executor");
        assertionExecutor.shutdownNow();
    }

    private static FtpResponseKnob buildFtpResponseKnob(final FtpReply ftpReply) {
        return new FtpResponseKnob() {
            @Override
            public int getReplyCode() {
                return ftpReply.getReplyCode();
            }

            @Override
            public String getReplyText() {
                return ftpReply.getReplyText();
            }
        };
    }

    public FtpClientWrapper createFtpClient(String userName, String password, VariableExpander variableExpander,
                                            FtpSecurity security) throws FtpException {
        //------COMMON------

        String hostName = getHostName(variableExpander);
        String directory = getDirectory(variableExpander);

        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(getPort(variableExpander)).setUser(userName).setPass(password).setDirectory(directory)
                .setTimeout(assertion.getTimeout());

        if (FtpSecurity.FTP_UNSECURED == security) {
            final Ftp ftp = FtpUtils.newFtpClient(config);

            return buildFtpClient(ftp);
        } else {
            config.setSecurity(security);

            assert(!assertion.isVerifyServerCert() || trustManager != null);
            assert(!assertion.isUseClientCert() ||
                    (null != assertion.getClientCertKeystoreId() && null != assertion.getClientCertKeyAlias()));

            X509TrustManager trustManager = null;
            HostnameVerifier hostnameVerifier = null;

            if (assertion.isVerifyServerCert()) {
                config.setVerifyServerCert(true);
                trustManager = this.trustManager;
                hostnameVerifier = this.hostnameVerifier;
            }

            DefaultKey keyFinder = null;

            if (assertion.isUseClientCert()) {
                config.setUseClientCert(true);
                config.setClientCertId(assertion.getClientCertKeystoreId());
                config.setClientCertAlias(assertion.getClientCertKeyAlias());
                keyFinder = this.keyFinder;
            }

            Ftps ftps = FtpClientUtils.newFtpsClient(config, keyFinder, trustManager, hostnameVerifier);

            return buildFtpsClient(ftps);
        }
    }

    private FtpClientWrapper buildFtpsClient(final Ftps ftps) {
        return new FtpClientWrapper() {
            @Override
            public void addFtpListener(FtpListener listener) {
                ftps.addFtpListener(listener);
            }

            @Override
            public void removeFtpListener(FtpListener listener) {
                ftps.removeFtpListener(listener);
            }

            @Override
            public void makeDir(String remoteDir) throws FtpException {
                ftps.makeDir(remoteDir);
            }

            @Override
            public void deleteDir(String remoteDir, boolean b) throws FtpException {
                ftps.deleteDir(remoteDir, b);
            }

            @Override
            public void deleteFile(String remoteFile) throws FtpException {
                ftps.deleteFile(remoteFile);
            }

            @Override
            public void download(OutputStream outputStream, String remoteFile) throws FtpException {
                ftps.download(outputStream, remoteFile);
            }

            @Override
            public String getDir() throws FtpException {
                return ftps.getDir();
            }

            @Override
            public Enumeration getDirListing() throws FtpException {
                return ftps.getDirListing();
            }

            @Override
            public Enumeration getDirListing(String filter) throws FtpException {
                return ftps.getDirListing(filter);
            }

            @Override
            public Enumeration getMachineDirListing(String remoteDir) throws FtpException {
                return ftps.getMachineDirListing(remoteDir);
            }

            @Override
            public FtpFile getMachineFileListing(String remoteFile) throws FtpException {
                return ftps.getMachineFileListing(remoteFile);
            }

            @Override
            public Enumeration getNameListing() throws FtpException {
                return ftps.getNameListing();
            }

            @Override
            public Enumeration getNameListing(String filter) throws FtpException {
                return ftps.getNameListing(filter);
            }

            @Override
            public long getFilesize(String remoteFile) throws FtpException {
                return ftps.getFilesize(remoteFile);
            }

            @Override
            public Date getFileTimestamp(String remoteFile) throws FtpException {
                return ftps.getFileTimestamp(remoteFile);
            }

            @Override
            public void noop() throws FtpException {
                ftps.noop();
            }

            @Override
            public void setDir(String remoteDir) throws FtpException {
                ftps.setDir(remoteDir);
            }

            @Override
            public void setDirUp() throws FtpException {
                ftps.setDirUp();
            }

            @Override
            public void upload(InputStream inputStream, String remoteFile) throws FtpException {
                ftps.upload(inputStream, remoteFile);
            }

            @Override
            public void upload(InputStream inputStream, String remoteFile, boolean append) throws FtpException {
                ftps.upload(inputStream, remoteFile, append);
            }

            @Override
            public void disconnect() {
                ftps.disconnect();
            }
        };
    }

    private FtpClientWrapper buildFtpClient(final Ftp ftp) {
        return new FtpClientWrapper() {
            @Override
            public void addFtpListener(FtpListener listener) {
                ftp.addFtpListener(listener);
            }

            @Override
            public void removeFtpListener(FtpListener listener) {
                ftp.removeFtpListener(listener);
            }

            @Override
            public void makeDir(String remoteDir) throws FtpException {
                ftp.makeDir(remoteDir);
            }

            @Override
            public void deleteDir(String remoteDir, boolean b) throws FtpException {
                ftp.deleteDir(remoteDir, b);
            }

            @Override
            public void deleteFile(String remoteFile) throws FtpException {
                ftp.deleteFile(remoteFile);
            }

            @Override
            public void download(OutputStream outputStream, String remoteFile) throws FtpException {
                ftp.download(outputStream, remoteFile);
            }

            @Override
            public String getDir() throws FtpException {
                return ftp.getDir();
            }

            @Override
            public Enumeration getDirListing() throws FtpException {
                return ftp.getDirListing();
            }

            @Override
            public Enumeration getDirListing(String filter) throws FtpException {
                return ftp.getDirListing(filter);
            }

            @Override
            public Enumeration getMachineDirListing(String remoteDir) throws FtpException {
                return ftp.getMachineDirListing(remoteDir);
            }

            @Override
            public FtpFile getMachineFileListing(String remoteFile) throws FtpException {
                return ftp.getMachineFileListing(remoteFile);
            }

            @Override
            public Enumeration getNameListing() throws FtpException {
                return ftp.getNameListing();
            }

            @Override
            public Enumeration getNameListing(String filter) throws FtpException {
                return ftp.getNameListing(filter);
            }

            @Override
            public long getFilesize(String remoteFile) throws FtpException {
                return ftp.getFilesize(remoteFile);
            }

            @Override
            public Date getFileTimestamp(String remoteFile) throws FtpException {
                return ftp.getFileTimestamp(remoteFile);
            }

            @Override
            public void noop() throws FtpException {
                ftp.noop();
            }

            @Override
            public void setDir(String remoteDir) throws FtpException {
                ftp.setDir(remoteDir);
            }

            @Override
            public void setDirUp() throws FtpException {
                ftp.setDirUp();
            }

            @Override
            public void upload(InputStream inputStream, String remoteFile) throws FtpException {
                ftp.upload(inputStream, remoteFile);
            }

            @Override
            public void upload(InputStream inputStream, String remoteFile, boolean append) throws FtpException {
                ftp.upload(inputStream, remoteFile, append);
            }

            @Override
            public void disconnect() {
                ftp.disconnect();
            }
        };
    }

    /**
     * Represents a problem executing the routed remote operation.
     * Used to wrap any exceptions thrown from the JSCAPE FTP library.
     */
    private static class FtpRoutingException extends Exception {
        private FtpReply ftpReply;

        public FtpRoutingException(String message, FtpException cause, FtpReply ftpResult) {
            super(message, cause);
            this.ftpReply = ftpResult;
        }

        public FtpReply getFtpReply() {
            return ftpReply;
        }
    }

    private static interface FtpClientWrapper {

        public void addFtpListener(FtpListener listener);

        public void removeFtpListener(FtpListener listener);

        /**
         * MKD
         */
        public void makeDir(String remoteDir) throws FtpException;

        /**
         * RMD
         */
        public void deleteDir(String remoteDir, boolean b) throws FtpException;

        /**
         * DELE
         */
        public void deleteFile(String remoteFile) throws FtpException;

        /**
         * RETR
         */
        public void download(OutputStream outputStream, String remoteFile) throws FtpException;

        /**
         * PWD
         */
        public String getDir() throws FtpException;

        /**
         * LIST
         */
        public Enumeration getDirListing() throws FtpException;

        public Enumeration getDirListing(String filter) throws FtpException;

        /**
         * MLSD
         */
        public Enumeration getMachineDirListing(String remoteDir) throws FtpException;

        /**
         * MLST
         */
        public FtpFile getMachineFileListing(String remoteFile) throws FtpException;

        /**
         * NLST
         */
        public Enumeration getNameListing() throws FtpException;

        public Enumeration getNameListing(String filter) throws FtpException;

        /**
         * SIZE
         */
        public long getFilesize(String remoteFile) throws FtpException;

        /**
         * MDTM
         */
        public Date getFileTimestamp(String remoteFile) throws FtpException;

        /**
         * NOOP
         */
        public void noop() throws FtpException;

        /**
         * CWD
         */
        public void setDir(String remoteDir) throws FtpException;

        /**
         * CDUP
         */
        public void setDirUp() throws FtpException;

        /**
         * STOR
         */
        public void upload(InputStream inputStream, String remoteFile) throws FtpException;

        /**
         * APPE
         */
        public void upload(InputStream inputStream, String remoteFile, boolean append) throws FtpException;

        public void disconnect();
    }

    private static class FtpReplyListener extends FtpAdapter {
        ReentrantLock lock = new ReentrantLock();

        private int lastReplyCode;
        private String lastReplyText;
        private String lastCommand;

        public FtpReplyListener() {
            super();
        }

        public FtpReply getFtpReply() {
            lock.lock();

            try {
                return new FtpReply(lastReplyCode, lastReplyText, lastCommand);
            } finally {
                lock.unlock();
            }
        }

        public int getLastReplyCode() {
            return lastReplyCode;
        }

        @Override
        public void commandSent(FtpCommandEvent ftpCommandEvent) {
            lock.lock();

            try {
                lastCommand = ftpCommandEvent.getCommand();
                lastReplyCode = 0;
                lastReplyText = null;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void responseReceived(final FtpResponseEvent ftpResponseEvent) {
            lock.lock();

            try {
                lastReplyCode = Integer.parseInt(ftpResponseEvent.getResponse().substring(0, 3));
                lastReplyText = ftpResponseEvent.getResponse().substring(3);
            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                lastReplyCode = 0; // should never happen
                lastReplyText = null;
            } finally {
                lock.unlock();
            }
        }
    }

    private static class FtpReply {
        private final int replyCode;
        private final String replyText;
        private final String lastCommand;

        private InputStream dataStream;

        public FtpReply(int replyCode, String replyText, String lastCommand) {
            this.lastCommand = lastCommand;
            this.replyCode = replyCode;
            this.replyText = replyText;
        }

        public int getReplyCode() {
            return replyCode;
        }

        public String getReplyText() {
            return replyText;
        }

        public String getLastCommand() {
            return lastCommand;
        }

        public void setDataStream(InputStream dataStream) {
            this.dataStream = dataStream;
        }

        public InputStream getDataStream() {
            return dataStream;
        }

        public boolean isNoReply() {
            return 0 == replyCode;
        }

        public boolean isPositiveCompletion() {
            return replyCode >= 200 && replyCode < 300;
        }

        public boolean isTransientError() {
            return replyCode >= 400 && replyCode < 500;
        }

        public boolean isPermanentError() {
            return replyCode >= 500 && replyCode < 600;
        }

        public boolean isDownloadStarted() {
            return FtpCommand.RETR.toString().equals(lastCommand) && REPLY_CODE_150 == replyCode && null != dataStream;
        }

        @Override
        public String toString() {
            return lastCommand + ": " + replyCode + " " + replyText;
        }
    }
}