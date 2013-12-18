package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.*;
import com.jscape.inet.ftps.Ftps;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.*;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.variable.ServerVariables;
import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.util.*;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.server.DefaultKey;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.io.ByteLimitInputStream.DataSizeLimitExceededException;
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

    private static final Object assertionExecutorInitLock = new Object();
    private static volatile ExecutorService assertionExecutor;

    private final X509TrustManager _trustManager;
    private final HostnameVerifier _hostnameVerifier;
    private final DefaultKey _keyFinder;
    private final String[] variablesUsed;

    private StashManagerFactory stashManagerFactory;
    private FtpConnectionPoolManager ftpConnectionPoolManager;
    private FtpsConnectionPoolManager ftpsConnectionPoolManager;
    private int ftpConnectionIdleTime;

    public ServerFtpRoutingAssertion(FtpRoutingAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext);
        _trustManager = applicationContext.getBean("routingTrustManager", X509TrustManager.class);
        _hostnameVerifier = applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        _keyFinder = applicationContext.getBean("defaultKey", DefaultKey.class);
        stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        this.variablesUsed = assertion.getVariablesUsed();
        Config config = applicationContext.getBean("serverConfig", ServerConfig.class);
        // Initialize the executor if necessary
        if (assertionExecutor == null)
            initializeAssertionExecutor(config);
        ftpConnectionIdleTime = config.getIntProperty(ClusterProperty.asServerConfigPropertyName(FtpRoutingAssertion.CP_BINDING_TIMEOUT),
                ConnectionPoolManager.DEFAULT_BINDING_TIMEOUT);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Message request;

        try {
            request = context.getTargetMessage(assertion.get_requestTarget());
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);

        if (mimeKnob == null) {
            // Uninitialized request
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        // DELETE CURRENT SECURITY HEADER IF NECESSARY
        try {
            handleProcessedSecurityHeader(request);
        } catch(SAXException se) {
            logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
        }

        final VariableExpander variableExpander = getVariableExpander(context);

        String userName = null;
        String password = null;

        if (assertion.getCredentialsSource() == FtpCredentialsSource.PASS_THRU) {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();

            if (credentials != null) {
                userName = credentials.getName();
                password = new String(credentials.getCredentials());
            }

            if (userName == null) {
                logAndAudit(AssertionMessages.FTP_ROUTING_PASSTHRU_NO_USERNAME);
                return AssertionStatus.FAILED;
            }
        } else if (assertion.getCredentialsSource() == FtpCredentialsSource.SPECIFIED) {
            userName = getUserName(variableExpander);

            try {
                password = getPassword(variableExpander);
            } catch (FindException e) {
                logAndAudit(AssertionMessages.FTP_ROUTING_UNABLE_TO_FIND_STORED_PASSWORD, e.getMessage());
                return AssertionStatus.FAILED;
            }
        }

        String arguments = null;

        if (assertion.getFileNameSource() == FtpFileNameSource.AUTO) {
            // Cannot use STOU because // TODO jwilliams: Should make that decision in the routing - not the inbound handling - to be less tightly coupled.
            // {@link com.jscape.inet.ftp.Ftp.uploadUnique(InputStream, String)}
            // sends a parameter as filename seed, which causes IIS to respond
            // with "500 'STOU seed': Invalid number of parameters".
            // This was reported (2007-05-07) to JSCAPE, who said they will add
            // a method to control STOU parameter.
            arguments = context.getRequestId().toString();
        } else if (assertion.getFileNameSource() == FtpFileNameSource.ARGUMENT) {
            arguments = variableExpander.expandVariables(assertion.getArguments());
        }

        final ClientIdentity identity = createClientIdentity(context, request, userName);

        final FtpClientWrapper ftpClient;

        // get ftp(s) client
        // TODO jwilliams: this might be implemented in a factory once the new connection pool is added
        try {
            ftpClient = getFtpClient(identity, userName, password, variableExpander, assertion.getSecurity());
        } catch (FtpException e) {
            e.printStackTrace();
            // TODO jwilliams: log and audit - couldn't create connection
            return AssertionStatus.FAILED;
        }

        FtpMethod ftpMethod;

        if (assertion.isCommandFromVariable()) {
            String otherCommand = variableExpander.expandVariables(Syntax.getVariableExpression(assertion.getFtpMethodOtherCommand()));
            ftpMethod = (FtpMethod) FtpMethod.getEnumTranslator().stringToObject(otherCommand);
        } else {
            ftpMethod = assertion.getFtpMethod();
        }

        try {
            switch (ftpMethod.getFtpMethodEnum()) {
                case FTP_APPE:
                case FTP_STOU:
                case FTP_STOR:
                    final InputStream messageBodyStream;

                    try {
                        messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream(); // TODO jwilliams: is this necessary at all before we know what command we're going to route?
                    } catch (NoSuchPartException e) {
                        throw new CausedIOException("Unable to get request body.", e); // TODO jwilliams: is just throwing an exception the right way to handle this?
                    }

                    final long bodyContentLength = mimeKnob.getContentLength();

                    routeUpload(context, ftpClient, messageBodyStream, bodyContentLength, ftpMethod, arguments);

                    break;
                case FTP_RETR:
                    routeDownload(context, ftpClient, arguments);

                    break;
                default:
                    routeOtherCommand(context, ftpClient, ftpMethod, arguments);
            }
        } catch (FtpRoutingException e) {
            logAndAudit(AssertionMessages.FTP_ROUTING_FAILED_UPLOAD, getHostName(variableExpander), e.getMessage()); // TODO jwilliams: use right error message
            return AssertionStatus.FAILED;
        } catch (DataSizeLimitExceededException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[] {"FTP routing failed: " + ExceptionUtils.getMessage(e)},
                    ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace(); // TODO jwilliams: handle properly
            throw e;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "INTERRUPTED EXCEPTION!!!!  " + getMessage(e));
            e.printStackTrace();
            // TODO jwilliams: handle properly
        } catch (Throwable throwable) {
            logger.log(Level.WARNING, "SERIOUS UNEXPECTED ISSUE!!!!  " + getMessage(throwable));
            throwable.printStackTrace();
        }

        return AssertionStatus.NONE;
    }

    private ClientIdentity createClientIdentity(PolicyEnforcementContext context, Message request, String userName) { // TODO jwilliams: if it's not HTTP or FTP then there will be no identity info!
        FtpRequestKnob ftpRequest = request.getKnob(FtpRequestKnob.class);

        if (ftpRequest != null) {
            return new ClientIdentity(ftpRequest.getCredentials().getUserName(), ftpRequest.getRemoteHost(),
                    ftpRequest.getRemotePort(), ftpRequest.getPath());
        } else {
            final HttpServletRequestKnob httpServletRequestKnob =
                    context.getRequest().getKnob(HttpServletRequestKnob.class);

            URL url = httpServletRequestKnob.getRequestURL();
            String host = url.getHost();
            int port = url.getPort();
            String path = url.getPath();

            return new ClientIdentity(userName, host, port, path);
        }
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

    private String getDirectory(final VariableExpander variableExpander) {
        return variableExpander.expandVariables(assertion.getDirectory());
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

    private void routeUpload(final PolicyEnforcementContext context, final FtpClientWrapper ftp, final InputStream is,
                             final long count, final FtpMethod ftpMethod, final String filename)
            throws FtpRoutingException, IOException {
        final FtpReplyListener replyListener = new FtpReplyListener() {
            @Override
            public void upload(final FtpUploadEvent ftpUploadEvent) {
                logger.log(Level.INFO, "--FTP UPLOAD EVENT: " + ftpUploadEvent.getFilename() + " uploaded in " + ftpUploadEvent.getTime() / 1000f + " seconds");

                setSize(ftpUploadEvent.getSize());
            }
        };

        try {
            ftp.addFtpListener(replyListener);

            switch (ftpMethod.getFtpMethodEnum()) {
                case FTP_STOR:
                    ftp.upload(is, filename);
                    break;
                case FTP_APPE:
                    ftp.upload(is, filename, true);
                    break;
                case FTP_STOU:
                    ftp.uploadUnique(is, filename);
                    break;
                default:
                    ftp.upload(is, filename);
            }
        } catch (FtpException e) {
//            if (replyListener.isError()) { // TODO jwilliams: evaluate by the reply code
//                throw new FtpRoutingException(replyListener.getError());
//            }
        } finally {
            ftp.removeFtpListener(replyListener);
        }

        if (replyListener.getSize() < count) {
            throw new FtpRoutingException("File '" + filename + "' upload truncated to " + replyListener.getSize() + " bytes.");
        }

        FtpReply ftpReply = new FtpReply(replyListener.getReplyCode(),
                replyListener.getReplyData(), new ByteArrayInputStream(new byte[0]));

        createResponseMessage(context, ftpReply);
    }

    private void routeDownload(final PolicyEnforcementContext context, final FtpClientWrapper ftpClient,
                               final String fileToDownload) throws Throwable {
        // used to wait for download to start
        final CountDownLatch startSignal = new CountDownLatch(1); // TODO jwilliams: use listener progress event to update? is a progress event guaranteed to be thrown at least once for each download?

        // holds any exceptions that may be thrown attempting to start the download
        final AtomicReference<Throwable> downloadException = new AtomicReference<>(null);

        // hold the details of the reply to the download command if one was returned
        final AtomicReference<FtpReply> downloadReply = new AtomicReference<>(null); // TODO jwilliams: may not have any value

        // response byte limit
        long byteLimit = getResponseByteLimit(context);

        final PipedInputStream pis = new PipedInputStream();

        try {
            final PipedOutputStream pos = new PipedOutputStream(pis);

            // start download task - it will run in the background after this assertion completes until the entire file has been received
            startFtpDownloadTask(pos, new Functions.NullaryVoidThrows<FtpException>() {
                @Override
                public void call() throws FtpException {
                    final FtpReplyListener replyListener = new FtpReplyListener() {
                        @Override
                        public void responseReceived(FtpResponseEvent ftpResponseEvent) {
                            super.responseReceived(ftpResponseEvent);

                            if (getReplyCode() == 150) {
                                logger.log(Level.INFO, "--150 - ABOUT TO OPEN DATA CONNECTION");
                                startSignal.countDown();
                            }
                        }

                        @Override
                        public void progress(FtpProgressEvent ftpProgressEvent) {
                            // indicate the download has started
                            startSignal.countDown();
                        }

                        @Override
                        public void download(FtpDownloadEvent ftpDownloadEvent) {
                            // indicate the download has completed in case there was no progress event
                            logger.log(Level.INFO, "Download COMPLETE, start signal: " + startSignal.getCount());
                            startSignal.countDown();
                        }
                    };

                    ftpClient.addFtpListener(replyListener);

                    try {
                        // TODO jwilliams: do something useful with listener results
                        logger.log(Level.INFO, "Download STARTING, start signal: " + startSignal.getCount());


                        ftpClient.download(pos, fileToDownload);

                        Ftp f = ftpClient.getClient();

                        f.getInputStream(fileToDownload, 0L);


                        logger.log(Level.INFO, "Download FINISHED, start signal: " + startSignal.getCount());
                    } catch (FtpException e) {
                        // save the exception thrown trying to download the file
                        downloadException.set(e); // TODO jwilliams: save listener results?
                        logger.log(Level.WARNING, "FTP Exception in download: " + e.getMessage());
                        // save the FTP reply details, if available
                        downloadReply.set(new FtpReply(replyListener.getReplyCode(),
                                replyListener.getReplyData(), null));

                        throw e;
                    } finally {
                        // call countdown in case it hasn't been called already
                        startSignal.countDown();

                        // remove the ftp reply listener
                        ftpClient.removeFtpListener(replyListener);

                        // return the client to the pool // TODO jwilliams: implement with connection pool
//                        sshSessionPool.returnObject(session.getKey(), session);
                    }
                }
            });

            logger.log(Level.INFO, "Returned from Executor submission");
        } catch (Throwable t) {
            // call countdown in case it hasn't been called already
            startSignal.countDown();

            logger.log(Level.WARNING, "CAUGHT AN UNEXPECTED PROBLEM: " + t.getMessage());

            throw t;
        }

        logger.log(Level.INFO, "Waiting for the download task to begin");
        // wait until the download has started
        startSignal.await();

        logger.log(Level.INFO, "Download task has begun");

        //check if there was an error retrieving the input stream.
        Throwable exceptionThrown = downloadException.get();

        if (exceptionThrown != null) {
//            logAndAudit(SSH_ROUTING_ERROR,
//                    new String[]{"Error opening file stream: " + getMessage(exceptionThrown)}, getDebugException(exceptionThrown));
            logger.log(Level.WARNING, "Error opening file stream: " + getMessage(exceptionThrown));

            throw exceptionThrown;
        }

        final Message response;

        try {
            response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        logger.log(Level.INFO, "Initializing response");
        response.initialize(stashManagerFactory.createStashManager(),
                ContentTypeHeader.OCTET_STREAM_DEFAULT, pis, byteLimit);
    }

    private void routeOtherCommand(final PolicyEnforcementContext context, final FtpClientWrapper ftpClient,
                                   final FtpMethod ftpMethod, final String arguments)
            throws FtpRoutingException, IOException {
        InputStream is = null;

        final FtpReplyListener replyListener = new FtpReplyListener() {};

        try {
            ftpClient.addFtpListener(replyListener);

            switch (ftpMethod.getFtpMethodEnum()) {
                case FTP_DELE:
                    ftpClient.deleteFile(arguments); // TODO jwilliams: look at recursive option - defined in arguments? refer to RFC & Apache FTP Server implementation
                    break;
                case FTP_MKD:
                    ftpClient.makeDir(arguments);
                    break;
                case FTP_RMD:
                    ftpClient.deleteDir(arguments, true);
                    break;
                case FTP_NOOP:
                    ftpClient.noop();
                    break;
                case FTP_CWD:
                    ftpClient.setDir(arguments);
                    break;
                case FTP_CDUP:
                    ftpClient.setDirUp();
                    break;
                case FTP_PWD:
                    ftpClient.getDir();
                    break;
                case FTP_SIZE:
                    ftpClient.getFilesize(arguments);
                    break;
                case FTP_MDTM:
                    ftpClient.getFileTimestamp(arguments); // TODO jwilliams: check actually issuing MDTM command (use listener event)
                    break;
                case FTP_MLST:
                    ftpClient.getMachineFileListing(arguments); // TODO jwilliams: check actually issuing MLST command (use listener event)
                    break;
                case FTP_MLSD:
                    Enumeration machineDirListingEnum = ftpClient.getMachineDirListing(arguments);
                    is = new ByteArrayInputStream(createRawListing(machineDirListingEnum).getBytes());
                    break;
                case FTP_LIST:
                    Enumeration dirListingEnum = ftpClient.getDirListing(); // TODO jwilliams: handle arguments case
                    is = new ByteArrayInputStream(createRawListing(dirListingEnum).getBytes());
                    break;
                case FTP_NLST:
                    Enumeration fileNames = ftpClient.getNameListing(); // TODO jwilliams: handle arguments case

                    StringBuilder sb = new StringBuilder();

                    while (fileNames.hasMoreElements()) {
                        String file = (String) fileNames.nextElement();
                        sb.append(file);
                        sb.append("\r\n");
                    }

                    is = new ByteArrayInputStream(sb.toString().getBytes());
                    break;
                default:
                    throw new FtpRoutingException("Unsupported command '" + ftpMethod.getWspName() + "'"); // TODO jwilliams: handle this in switch statement that picks upload/download/other
            }
        } catch (FtpException e) {
            // TODO jwilliams: check assertion setting for failure action, check code, throw FtpRoutingException or let processing continue
            e.printStackTrace();
        } finally {
            ftpClient.removeFtpListener(replyListener);
        }

        FtpReply ftpReply =
                new FtpReply(replyListener.getReplyCode(),
                        replyListener.getReplyData(),
                        null != is ? is : new ByteArrayInputStream(new byte[0]));

        createResponseMessage(context, ftpReply);
    }

    private Message createResponseMessage(PolicyEnforcementContext context, FtpReply ftpReply) throws IOException {
        final Message response;

        try {
            response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        response.initialize(stashManagerFactory.createStashManager(),
                ContentTypeHeader.OCTET_STREAM_DEFAULT, ftpReply.getDataStream(), getResponseByteLimit(context));

        response.attachFtpResponseKnob(buildFtpResponseKnob(ftpReply));

        // force all message parts to be initialized, it is by default lazy
        logger.log(Level.FINER, "Reading FTP(S) response.");
        response.getMimeKnob().getContentLength();
        logger.log(Level.FINER, "Read FTP(S) response.");

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
    private Future<Void> startFtpDownloadTask(final PipedOutputStream pos,
                                              final Functions.NullaryVoidThrows<FtpException> downloadFunction)
            throws CausedIOException {
        final CountDownLatch startedSignal = new CountDownLatch(1);

        logger.log(Level.INFO, "Submitting download task to Executor");

        final Future<Void> future = assertionExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException, FtpException {
                try {
                    logger.log(Level.INFO, "Executor initiating download task");
                    startedSignal.countDown();
                    downloadFunction.call();
                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }

                return null;
            }
        });

        // wait until processing has started before continuing
        try {
            logger.log(Level.INFO, "Waiting for download task to begin");
            startedSignal.await();
            logger.log(Level.INFO, "Received signal that download task has begun");
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for download.", ie);
        }

        logger.log(Level.INFO, "Returning FutureTask");
        return future;
    }

    private static void initializeAssertionExecutor(Config config) { // TODO jwilliams: inspect this
        int globalMaxConcurrency = config.getIntProperty(FtpRoutingAssertion.SC_MAX_CONC, 64);
        int globalCoreConcurrency = config.getIntProperty(FtpRoutingAssertion.SC_CORE_CONC, 32);
        int globalMaxWorkQueue = config.getIntProperty(FtpRoutingAssertion.SC_MAX_QUEUE, 64);
        
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

    private FtpConnectionPoolManager getFtpConnectionPoolManager() {
        FtpConnectionPoolManager ftpConnectionPoolManager = this.ftpConnectionPoolManager;

        if (ftpConnectionPoolManager == null) {
            ftpConnectionPoolManager = new FtpConnectionPoolManager();
            ftpConnectionPoolManager.setBindingTimeout(ftpConnectionIdleTime);
            this.ftpConnectionPoolManager = ftpConnectionPoolManager;
        }

        return ftpConnectionPoolManager;
    }

    private FtpsConnectionPoolManager getFtpsConnectionPoolManager() {
        FtpsConnectionPoolManager ftpsConnectionPoolManager = this.ftpsConnectionPoolManager;

        if (ftpsConnectionPoolManager == null) {
            ftpsConnectionPoolManager = new FtpsConnectionPoolManager();
            ftpsConnectionPoolManager.setBindingTimeout(ftpConnectionIdleTime);
            this.ftpsConnectionPoolManager = ftpsConnectionPoolManager;
        }

        return ftpsConnectionPoolManager;
    }

    private static FtpResponseKnob buildFtpResponseKnob(final FtpReply ftpReply) {
        return new FtpResponseKnob() {
            @Override
            public int getReplyCode() {
                return ftpReply.getReplyCode();
            }

            @Override
            public String getReplyData() {
                return ftpReply.getReplyString();
            }
        };
    }

    public FtpClientWrapper getFtpClient(ClientIdentity identity, String userName, String password,
                                         VariableExpander variableExpander, FtpSecurity security) throws FtpException {
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
            FtpConnectionPoolManager ftpConnectionManager = getFtpConnectionPoolManager();
            ftpConnectionManager.setId(identity);
            ftpConnectionManager.bind();

            final Ftp ftp = ftpConnectionManager.getConnection(config);
            ftpConnectionManager.setBoundFtp(identity, ftp, true);

            return buildFtpClient(ftp);
        } else {
            config.setSecurity(security);

            assert(!assertion.isVerifyServerCert() || _trustManager != null);
            assert(!assertion.isUseClientCert() ||
                    (null != assertion.getClientCertKeystoreId() && null != assertion.getClientCertKeyAlias()));

            X509TrustManager trustManager = null;
            HostnameVerifier hostnameVerifier = null;

            if (assertion.isVerifyServerCert()) {
                config.setVerifyServerCert(true);
                trustManager = _trustManager;
                hostnameVerifier = _hostnameVerifier;
            }

            DefaultKey keyFinder = null;

            if (assertion.isUseClientCert()) {
                config.setUseClientCert(true);
                config.setClientCertId(assertion.getClientCertKeystoreId());
                config.setClientCertAlias(assertion.getClientCertKeyAlias());
                keyFinder = _keyFinder;
            }

            FtpsConnectionPoolManager ftpsConnectionManager = getFtpsConnectionPoolManager();
            ftpsConnectionManager.setId(identity, trustManager, hostnameVerifier);
            ftpsConnectionManager.bind(trustManager, hostnameVerifier);

            Ftps ftps = ftpsConnectionManager.getConnection(config, keyFinder, trustManager, hostnameVerifier);

            ftpsConnectionManager.setBoundFtp(identity, ftps, true);

            return buildFtpsClient(ftps);
        }
    }

    private FtpClientWrapper buildFtpsClient(final Ftps ftps) {
        return new FtpClientWrapper() {
            @Override
            public Ftp getClient() {
                return null;
            }

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
            public void uploadUnique(InputStream inputStream, String remoteFile) throws FtpException {
                ftps.uploadUnique(inputStream, remoteFile);
            }
        };
    }

    private FtpClientWrapper buildFtpClient(final Ftp ftp) {
        return new FtpClientWrapper() {
            @Override
            public Ftp getClient() {
                return ftp;
            }

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
            public void uploadUnique(InputStream inputStream, String remoteFile) throws FtpException {
                ftp.uploadUnique(inputStream, remoteFile);
            }
        };
    }

    private static class FtpRoutingException extends Exception {
        public FtpRoutingException(String message) {
            super(message);
        }
    }

    private static interface FtpClientWrapper {

        public Ftp getClient();

        public void addFtpListener(FtpListener listener);

        public void removeFtpListener(FtpListener listener);

        public void makeDir(String remoteDir) throws FtpException;

        public void deleteDir(String remoteDir, boolean b) throws FtpException;

        public void deleteFile(String remoteFile) throws FtpException;

        public void download(OutputStream outputStream, String remoteFile) throws FtpException;

        /**
         * PWD
         */
        public String getDir() throws FtpException;

        public Enumeration getDirListing() throws FtpException;

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

        /**
         * SIZE
         */
        public long getFilesize(String remoteFile) throws FtpException;

        public Date getFileTimestamp(String remoteFile) throws FtpException;

        public void noop() throws FtpException;

        public void setDir(String remoteDir) throws FtpException;

        public void setDirUp() throws FtpException;

        public void upload(InputStream inputStream, String remoteFile) throws FtpException;

        public void upload(InputStream inputStream, String remoteFile, boolean append) throws FtpException;

        public void uploadUnique(InputStream inputStream, String remoteFile) throws FtpException;
    }

    private abstract static class FtpReplyListener extends FtpAdapter {
        private long size;

        private int replyCode;
        private String replyData;

        public FtpReplyListener() {
            super();
        }

        public long getSize() {
            return size;
        }

        protected void setSize(long size) {
            this.size = size;
        }

        public int getReplyCode() {
            return replyCode;
        }

        public String getReplyData() {
            return replyData;
        }

        @Override
        public void commandSent(FtpCommandEvent ftpCommandEvent) {
            logger.log(Level.INFO, "--FTP COMMAND SENT: " + ftpCommandEvent.getCommand());
        }

        @Override
        public void responseReceived(final FtpResponseEvent ftpResponseEvent) {
            try {
                replyCode = Integer.parseInt(ftpResponseEvent.getResponse().substring(0, 3));
                replyData = ftpResponseEvent.getResponse().substring(3);

                logger.log(Level.INFO, "--FTP RESPONSE EVENT: " + replyCode + " " + replyData);
            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                replyCode = 0; // TODO jwilliams: Spit bics
                replyData = null;
            }
        }
    }

    private class ClientIdentity {

        private String clientUser;
        private String clientServer;
        private int clientPort;
        private String clientPath;
        private volatile String cachedToString;

        public ClientIdentity (String clientUser, String clientServer, int clientPort, String clientPath) {
           this.clientUser = clientUser;
           this.clientServer = clientServer;
           this.clientPort = clientPort;
           this.clientPath = clientPath;
        }

        public void setClientUser(String clientUser) {
            this.clientUser = clientUser;
        }

        public String getClientUser() {
            return this.clientUser;
        }

        public void setClientServer(String clientServer) {
            this.clientServer = clientServer;
        }

        public String getClientServer() {
            return this.clientServer;
        }

        public void setClientPort(int clientPort) {
            this.clientPort = clientPort;
        }

        public int getClientPort() {
            return this.clientPort;
        }

        public void setClientPath(String clientPath) {
            this.clientPath = clientPath;
        }

        public String getClientPath() {
            return this.clientPath;
        }

        @Override
        public int hashCode() {
            int result;
            result = (clientUser != null ? clientUser.hashCode() : 0);
            result = 31 * result + (clientServer != null ? clientServer.hashCode() : 0);
            result = 31 * result + clientPort;
            result = 31 * result + (clientPath != null ? clientPath.hashCode() : 0);
            return result;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final ClientIdentity that = (ClientIdentity) o;

            if (clientUser != null ? !clientUser.equals(that.clientUser) : that.clientUser != null) return false;
            if (clientServer != null ? !clientServer.equals(that.clientServer) : that.clientServer != null) return false;
            if (String.valueOf(clientPort) != null ? !String.valueOf(clientPort).equals(String.valueOf(that.clientPort)) : String.valueOf(that.clientPort) != null) return false;
            if (clientPath != null ? !clientPath.equals(that.clientPath) : that.clientPath != null) return false;

            return true;
        }

        @Override
        public String toString() {
            if (cachedToString == null) {
                StringBuilder sb = new StringBuilder("<ClientIdentity user=\"");
                sb.append(clientUser);
                sb.append("\" ");
                sb.append("server=\"");
                sb.append(clientServer);
                sb.append("\" ");
                sb.append("port=\"");
                sb.append(clientPort);
                sb.append("\" ");
                sb.append("path=\"");
                sb.append(clientPath);
                sb.append("\"/>");

                cachedToString = sb.toString();
            }

            return cachedToString;
        }
    }

    private static class FtpReply {
        private final int replyCode;
        private final String replyString;
        private final InputStream dataStream;

        public FtpReply(int replyCode, String replyString, InputStream dataStream) {
            this.replyCode = replyCode;
            this.replyString = replyString;
            this.dataStream = dataStream;
        }

        public int getReplyCode() {
            return replyCode;
        }

        public String getReplyString() {
            return replyString;
        }

        public InputStream getDataStream() {
            return dataStream;
        }
    }
}