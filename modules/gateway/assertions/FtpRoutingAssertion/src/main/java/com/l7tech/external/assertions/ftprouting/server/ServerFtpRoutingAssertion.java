package com.l7tech.external.assertions.ftprouting.server;

import com.jscape.inet.ftp.*;
import com.jscape.inet.ftps.Ftps;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
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
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.transport.ftp.FtpClientUtils;
import com.l7tech.server.DefaultKey;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.HostnameVerifier;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.message.Message.getMaxBytes;
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

    private final X509TrustManager _trustManager;
    private final HostnameVerifier _hostnameVerifier;
    private final DefaultKey _keyFinder;
    @Inject
    private StashManagerFactory stashManagerFactory;
    private final String[] variablesUsed;
    private static final Object assertionExecutorInitLock = new Object();
    private static volatile ExecutorService assertionExecutor;
    private FtpConnectionPoolManager ftpConnectionPoolManager;
    private FtpsConnectionPoolManager ftpsConnectionPoolManager;
    private int ftpConnectionIdleTime;

    public ServerFtpRoutingAssertion(FtpRoutingAssertion assertion, ApplicationContext applicationContext) {
        super(assertion, applicationContext);
        _trustManager = applicationContext.getBean("routingTrustManager", X509TrustManager.class);
        _hostnameVerifier = applicationContext.getBean("hostnameVerifier", HostnameVerifier.class);
        _keyFinder = applicationContext.getBean("defaultKey", DefaultKey.class);
        this.variablesUsed = assertion.getVariablesUsed();
        Config config = applicationContext.getBean("serverConfig", ServerConfig.class);
        // Initialize the executor if necessary
        if (assertionExecutor == null)
            initializeAssertionExecutor(config);
        ConnectionPoolManager connectionPoolManager = new ConnectionPoolManager();
        ftpConnectionIdleTime = config.getIntProperty(ClusterProperty.asServerConfigPropertyName(FtpRoutingAssertion.CP_BINDING_TIMEOUT),
                ConnectionPoolManager.DEFAULT_BINDING_TIMEOUT);
        connectionPoolManager.setBindingTimeout(ftpConnectionIdleTime);
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

        if (mimeKnob == null ) {
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
            // Cannot use STOU because
            // {@link com.jscape.inet.ftp.Ftp.uploadUnique(InputStream, String)}
            // sends a parameter as filename seed, which causes IIS to respond
            // with "500 'STOU seed': Invalid number of parameters".
            // This was reported (2007-05-07) to JSCAPE, who said they will add
            // a method to control STOU parameter.
            arguments = context.getRequestId().toString();
        } else if (assertion.getFileNameSource() == FtpFileNameSource.ARGUMENT) {
            arguments = variableExpander.expandVariables(assertion.getArguments());
        }

        FtpMethod ftpMethod;

        if (assertion.getOtherCommand()) {
            String ftpMethodOtherCommand = expandVariables(context, assertion.getFtpMethodOtherCommand());
            ftpMethod = (FtpMethod) FtpMethod.getEnumTranslator().stringToObject(ftpMethodOtherCommand);
        } else {
            ftpMethod = assertion.getFtpMethod();
        }

        try {
            FtpRequestKnob ftpRequest = request.getKnob(FtpRequestKnob.class);
            ClientIdentity identity;

            if (ftpRequest != null) {
                identity = new ClientIdentity(ftpRequest.getCredentials().getUserName(), ftpRequest.getRemoteHost(),
                        ftpRequest.getRemotePort(), ftpRequest.getPath());
            } else {
                final HttpServletRequestKnob httpServletRequestKnob =
                        context.getRequest().getKnob(HttpServletRequestKnob.class);

                URL url = httpServletRequestKnob.getRequestURL();
                String host = url.getHost();
                int port = url.getPort();
                String path = url.getPath();
                identity = new ClientIdentity(userName, host, port, path);
            }

            final InputStream messageBodyStream = mimeKnob.getEntireMessageBodyAsInputStream();
            final long bodyBytes = mimeKnob.getContentLength();
            final FtpSecurity security = assertion.getSecurity();

            if (security == FtpSecurity.FTP_UNSECURED) {
                ftpConnectionPoolManager = getFtpConnectionPoolManager();
                doFtp(context, variableExpander, ftpConnectionPoolManager, identity, userName, password,
                        messageBodyStream, ftpMethod, bodyBytes, arguments);
            } else if (security == FtpSecurity.FTPS_EXPLICIT) {
                ftpsConnectionPoolManager = getFtpsConnectionPoolManager();
                doFtps(context, variableExpander, ftpsConnectionPoolManager, identity, true, userName, password,
                        messageBodyStream, ftpMethod, bodyBytes, arguments);
            } else if (security == FtpSecurity.FTPS_IMPLICIT) {
                ftpsConnectionPoolManager = getFtpsConnectionPoolManager();
                doFtps(context, variableExpander, ftpsConnectionPoolManager, identity, false, userName, password,
                        messageBodyStream, ftpMethod, bodyBytes, arguments);
            }

            return AssertionStatus.NONE;
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Unable to get request body.", e);
        } catch (FtpException e) {
            logAndAudit(AssertionMessages.FTP_ROUTING_FAILED_UPLOAD, getHostName(variableExpander), e.getMessage());
            return AssertionStatus.FAILED;
        } catch (InterruptedException | ExecutionException | NoSuchVariableException e) {
            return AssertionStatus.FAILED;
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
        if(null != assertion.getPasswordOid()) {
            return ServerVariables.getSecurePasswordByOid(new LoggingAudit(logger), assertion.getPasswordOid());
        } else {
            return assertion.isPasswordUsesContextVariables()
                   ? variableExpander.expandVariables(assertion.getPassword())
                   : assertion.getPassword();
        }
    }

    private void doFtp(PolicyEnforcementContext context,
                      final VariableExpander variableExpander,
                      FtpConnectionPoolManager ftpConnectionManager,
                      Object identity,
                      String userName,
                      String password,
                      InputStream is,
                      FtpMethod ftpMethod,
                      long count,
                      String arguments)
            throws FtpException, IOException, InterruptedException, ExecutionException, NoSuchVariableException {

        String hostName = getHostName(variableExpander);
        String directory = getDirectory(variableExpander);

        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(getPort(variableExpander)).setUser(userName).setPass(password).
                setDirectory(directory).setTimeout(assertion.getTimeout());

        ftpConnectionManager.setId(identity);
        ftpConnectionManager.bind();

        try {
            Ftp ftp = ftpConnectionManager.getConnection(config);
            ftpConnectionManager.setBoundFtp(identity, ftp, true);

            final FtpMethod.FtpMethodEnum ftpMethodEnum = ftpMethod.getFtpMethodEnum();

            switch (ftpMethodEnum) {
                case FTP_APPE:
                case FTP_STOU:
                case FTP_PUT:
                    upload(ftp, is, count, ftpMethod.getFtpMethodEnum(), arguments);
                    break;
                case FTP_GET:
                    download(context, config, ftp, arguments);
                    break;
                default:
                    command(context, config, ftp, ftpMethod, arguments);
            }
        } catch (FtpException ex) {
            is = new ByteArrayInputStream(ex.getMessage().getBytes());

            final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
            response.initialize(stashManagerFactory.createStashManager(),
                    ContentTypeHeader.create(assertion.getDownloadedContentType()), is, getMaxBytes());

            throw new FtpException(ex.getMessage());
        }
    }

    private void doFtps(PolicyEnforcementContext context,
                        final VariableExpander variableExpander,
                        FtpsConnectionPoolManager ftpsConnectionManager,
                        Object identity,
                        boolean isExplicit,
                        String userName,
                        String password,
                        InputStream is,
                        FtpMethod ftpMethod,
                        long count,
                        String arguments)
            throws FtpException, IOException, InterruptedException, ExecutionException, NoSuchVariableException {

        boolean verifyServerCert = assertion.isVerifyServerCert();
        String hostName = getHostName(variableExpander);
        boolean useClientCert = assertion.isUseClientCert();
        long clientCertKeystoreId = assertion.getClientCertKeystoreId();
        String clientCertKeyAlias = assertion.getClientCertKeyAlias();
        String directory = getDirectory(variableExpander);

        assert(!verifyServerCert || _trustManager != null);
        assert(hostName != null);
        assert(userName != null);
        assert(password != null);
        assert(!useClientCert || (clientCertKeystoreId != -1L && clientCertKeyAlias != null));
        assert(directory != null);

        FtpClientConfig config = FtpClientUtils.newConfig(hostName);
        config.setPort(getPort(variableExpander)).setUser(userName).setPass(password).setDirectory(directory).
                setTimeout(assertion.getTimeout()).setSecurity(isExplicit ? FtpSecurity.FTPS_EXPLICIT : FtpSecurity.FTPS_IMPLICIT);

        X509TrustManager trustManager = null;
        HostnameVerifier hostnameVerifier = null;
        if (verifyServerCert) {
            config.setVerifyServerCert(true);
            trustManager = _trustManager;
            hostnameVerifier = _hostnameVerifier;
        }

        DefaultKey keyFinder = null;
        if (useClientCert) {
            config.setUseClientCert(true).setClientCertId(clientCertKeystoreId).setClientCertAlias(clientCertKeyAlias);
            keyFinder = _keyFinder;
        }

        ftpsConnectionManager.setId(identity, trustManager, hostnameVerifier);
        ftpsConnectionManager.bind(trustManager, hostnameVerifier);

        try {
            Ftps ftps = ftpsConnectionManager.getConnection(config, keyFinder, trustManager, hostnameVerifier);

            ftpsConnectionManager.setBoundFtp(identity, ftps, true);

            final FtpMethod.FtpMethodEnum ftpMethodEnum = ftpMethod.getFtpMethodEnum();

            switch (ftpMethodEnum) {
                case FTP_APPE:
                case FTP_STOU:
                case FTP_PUT:
                    upload(ftps, is, count, arguments, ftpMethodEnum);
                    break;
                case FTP_GET:
                    download(context, config, ftps, arguments);
                    break;
                default:
                    command(context, config, ftps, ftpMethod, arguments);
            }

        } catch (FtpException ex) {
            is = new ByteArrayInputStream(ex.getMessage().getBytes());

            final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
            response.initialize(stashManagerFactory.createStashManager(),
                    ContentTypeHeader.create(assertion.getDownloadedContentType()), is, getMaxBytes());

            throw new FtpException(ex.getMessage());
        }
    }

    private void download(final PolicyEnforcementContext context,
                          final FtpClientConfig config,
                          final Object connection,
                          final String fileToDownload)
            throws IOException, InterruptedException, ExecutionException, NoSuchVariableException {
            final Map<String,?> variables = context.getVariableMap(variablesUsed, getAudit());
            // response byte limit
            long byteLimit = getMaxBytes();
            if (assertion.getResponseByteLimit() != null) {
                String byteLimitStr = ExpandVariables.process(assertion.getResponseByteLimit(), variables, getAudit());
                try {
                    byteLimit = Long.parseLong(byteLimitStr);
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "Used default response byte limit: " + byteLimit + ".  " + getMessage(e),
                            getDebugException(e));
                }
            }
            logger.log(Level.FINE, "Response byte limit: " + byteLimit + ".");
            final PipedInputStream pis = new PipedInputStream();
            final PipedOutputStream pos = new PipedOutputStream(pis);
            // start download task
            final Future<Void> future = startFtpDownloadTask(config, connection, fileToDownload, pos);
            final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
            response.initialize(stashManagerFactory.createStashManager(),
                    ContentTypeHeader.create(assertion.getDownloadedContentType()), pis, byteLimit);
            // force all message parts to be initialized, it is by default lazy
            logger.log(Level.FINER, "Reading FTP(S) response.");
            response.getMimeKnob().getContentLength();
            logger.log(Level.FINER, "Read FTP(S) response.");
            future.get();
    }

    private void command(final PolicyEnforcementContext context,
                         final FtpClientConfig config,
                         final Object connection,
                         final FtpMethod ftpMethod,
                         final String arguments)
            throws IOException, InterruptedException, ExecutionException, FtpException, NoSuchVariableException {
        final Map<String,?> variables = context.getVariableMap(variablesUsed, getAudit());

        // response byte limit
        long byteLimit = getMaxBytes();
        if (assertion.getResponseByteLimit() != null) {
            String byteLimitStr = ExpandVariables.process(assertion.getResponseByteLimit(), variables, getAudit());
            try {
                byteLimit = Long.parseLong(byteLimitStr);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Used default response byte limit: " + byteLimit + ".  " + getMessage(e),
                        getDebugException(e));
            }
        }

        logger.log(Level.FINE, "Response byte limit: " + byteLimit + ".");

        InputStream is = null;

        if (FtpSecurity.FTP_UNSECURED == config.getSecurity()) {
            Ftp ftp = (Ftp) connection;
            if (FtpListUtil.isInputStreamCommand(ftpMethod)) {
                is = inputStreamFtp(ftp, arguments, ftpMethod);
            } else {
                is = issueFtpCommand(ftp, ftpMethod, arguments);
            }
        } else if (FtpSecurity.FTPS_EXPLICIT == config.getSecurity() || FtpSecurity.FTPS_IMPLICIT == config.getSecurity()) {
            Ftps ftps = (Ftps) connection;
            if (FtpListUtil.isInputStreamCommand(ftpMethod)) {
                is = inputStreamFtps(ftps, arguments, ftpMethod);
            } else {
                is = issueFtpsCommand(ftps, ftpMethod, arguments);
            }
        }

        final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);

        if (is != null){
            response.initialize(stashManagerFactory.createStashManager(),
                    ContentTypeHeader.create(assertion.getDownloadedContentType()), is, byteLimit);
        } else {
            response.initialize(stashManagerFactory.createStashManager(),
                    ContentTypeHeader.create(assertion.getDownloadedContentType()), new ByteArrayInputStream(new byte[0]), byteLimit);
        }

        // force all message parts to be initialized, it is by default lazy
        logger.log(Level.FINER, "Reading FTP(S) response.");
        response.getMimeKnob().getContentLength();
        logger.log(Level.FINER, "Read FTP(S) response.");
    }

    private static InputStream issueFtpCommand(final Ftp ftp,
                                               final FtpMethod ftpMethod,
                                               final String arguments) throws FtpException {
        String response;
        final FtpListener listener = new FtpListener();
        ftp.addFtpListener(listener);

        switch (ftpMethod.getFtpMethodEnum()) {
            case FTP_DELE:
                ftp.deleteFile(arguments);
                response = "250 File deleted successfully";
                break;
            case FTP_MKD:
                ftp.makeDir(arguments);
                response = "\"/" + arguments + "\" created successfully";
                break;
            case FTP_RMD:
                ftp.deleteDir(arguments, true);
                response = "250 Directory deleted successfully";
                break;
            case FTP_NOOP:
                ftp.noop();
                response = "200 OK";
                break;
            case FTP_CWD:
                if (arguments.startsWith("/")){
                    response = "250 CWD successful. " + arguments + " is current directory/service.";
                }
                else {
                    response = ftp.issueCommand(ftpMethod.getWspName() + " " + arguments);
                    listener.responseReceived(new FtpResponseEvent(ftp, response));
                }
                break;
            case FTP_LOGIN:
                response = "Logged on";
                break;
            default:
                response = ftp.issueCommand(ftpMethod.getWspName() + " " + arguments);
                listener.responseReceived(new FtpResponseEvent(ftp, response));
        }

        if (listener.isError()) {
            throw new FtpException(listener.getError());
        }

        return new ByteArrayInputStream(response.getBytes());
    }

    private static InputStream issueFtpsCommand(final Ftps ftps,
                                                final FtpMethod ftpMethod,
                                                final String arguments) throws FtpException {
        String response;
        final FtpListener listener = new FtpListener();
        ftps.addFtpListener(listener);

        switch(ftpMethod.getFtpMethodEnum()) {
            case FTP_DELE:
                ftps.deleteFile(arguments);
                response = "250 File deleted successfully";
                break;
            case FTP_MKD:
                ftps.makeDir(arguments);
                response = "\"/" + arguments + "\" created successfully";
                break;
            case FTP_RMD:
                ftps.deleteDir(arguments, true);
                response = "250 Directory deleted successfully";
                break;
            case FTP_NOOP:
                ftps.noop();
                response = "200 OK";
                break;
            case FTP_CWD:
                if (arguments.startsWith("/")){
                    response = "250 CWD successful. " + arguments + " is current directory/service.";
                }
                else {
                    response = ftps.issueCommand(ftpMethod.getWspName() + " " + arguments);
                    listener.responseReceived(new FtpResponseEvent(ftps, response));
                }
                break;
            case FTP_LOGIN:
                response = "Logged on";
                break;
            default:
                response = ftps.issueCommand(ftpMethod.getWspName() + " " + arguments);
                listener.responseReceived(new FtpResponseEvent(ftps, response));

        }

        if (listener.isError()) {
            throw new FtpException(listener.getError());
        }

        return new ByteArrayInputStream(response.getBytes());
    }


    /*
     * Download the given file on a new thread.
     */
    private Future<Void> startFtpDownloadTask(final FtpClientConfig config,
                                              final Object connection,
                                              final String fileToDownload,
                                              final PipedOutputStream pos) throws IOException {

        final CountDownLatch startedSignal = new CountDownLatch(1);

        final Future<Void> future = assertionExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                try {
                    startedSignal.countDown();
                    try {
                        if (FtpSecurity.FTP_UNSECURED == config.getSecurity()) {
                             Ftp ftp = (Ftp) connection;
                             ftp.download(pos, fileToDownload);
                        } else if (FtpSecurity.FTPS_EXPLICIT == config.getSecurity() || FtpSecurity.FTPS_IMPLICIT == config.getSecurity()) {
                            final Ftps ftps = (Ftps) connection;
                            ftps.download(pos, fileToDownload);
                        }
                    } catch (FtpException e) {
                        logger.log(Level.WARNING, "Unable to download the file: " + fileToDownload + getMessage(e),
                                getDebugException(e));
                        throw new CausedIOException("Ftp exception during download.", e);
                    }

                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }

                return null;
            }
        });

        try {
            startedSignal.await();
        } catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for download.", ie);
        }

        return future;
    }

    private static void initializeAssertionExecutor(Config config) {
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
    public static void resetAssertionExecutor(int maxConc, int coreConc, int maxQueue) {
        synchronized (assertionExecutorInitLock) {
            assertionExecutor.shutdown();
            assertionExecutor = createAssertionExecutor(maxConc, coreConc, maxQueue);
        }
    }

    private static InputStream inputStreamFtp(final Ftp ftp,
                                              final String arguments,
                                              final FtpMethod ftpMethod) throws FtpException {

        InputStream is = null;

        final FtpListener listener = new FtpListener();
        ftp.addFtpListener(listener);
        switch (ftpMethod.getFtpMethodEnum()) {
            case FTP_MLSD:
            case FTP_LIST:
                List<FtpFile> fileList = new ArrayList<>();
                Enumeration<FtpFile> fileEnum = ftp.getDirListing();
                while (fileEnum.hasMoreElements()) {
                    FtpFile file = fileEnum.nextElement();
                    fileList.add(file);
                }
                //Build XML as a String
                Document doc = createXml(fileList);
                String files = getStringFromDoc(doc);
                is = new ByteArrayInputStream(files.getBytes());
                break;
            case FTP_MDTM:
                Date lastModifiedTime = ftp.getFileTimestamp(arguments);
                long time = lastModifiedTime.getTime();
                is = new ByteArrayInputStream(String.valueOf(time).getBytes());
                break;
            case FTP_NLST:
                StringBuilder sb = new StringBuilder();
                Enumeration<String> fileNames = ftp.getNameListing();

                while (fileNames.hasMoreElements()) {
                    String file = fileNames.nextElement();
                    sb.append(file);
                    sb.append("\r\n");
                }

                is = new ByteArrayInputStream(sb.toString().getBytes());
                break;
            case FTP_SIZE:
                long fileSize = ftp.getFilesize(arguments);
                is = new ByteArrayInputStream(Long.toString(fileSize).getBytes());
                break;
            case FTP_USER:
                String userName = ftp.getUsername();
                is = new ByteArrayInputStream(userName.getBytes());
                break;
            case FTP_PASS:
                String password = ftp.getPassword();
                is = new ByteArrayInputStream(password.getBytes());
                break;
            default:
                String response = ftp.issueCommand(ftpMethod.getWspName());
                listener.responseReceived(new FtpResponseEvent(ftp, response));
                is = new ByteArrayInputStream(response.getBytes());
            case FTP_GET:
                break;
            case FTP_PUT:
                break;
            case FTP_DELE:
                break;
            case FTP_ABOR:
                break;
            case FTP_ACCT:
                break;
            case FTP_ADAT:
                break;
            case FTP_ALLO:
                break;
            case FTP_APPE:
                break;
            case FTP_AUTH:
                break;
            case FTP_CCC:
                break;
            case FTP_CDUP:
                break;
            case FTP_CONF:
                break;
            case FTP_CWD:
                break;
            case FTP_ENC:
                break;
            case FTP_EPRT:
                break;
            case FTP_EPSV:
                break;
            case FTP_FEAT:
                break;
            case FTP_HELP:
                break;
            case FTP_LANG:
                break;
            case FTP_MIC:
                break;
            case FTP_MKD:
                break;
            case FTP_MLST:
                break;
            case FTP_MODE:
                break;
            case FTP_NOOP:
                break;
            case FTP_OPTS:
                break;
            case FTP_PASV:
                break;
            case FTP_PBSZ:
                break;
            case FTP_PORT:
                break;
            case FTP_PROT:
                break;
            case FTP_PWD:
                break;
            case FTP_QUIT:
                break;
            case FTP_REIN:
                break;
            case FTP_RMD:
                break;
            case FTP_RNFR:
                break;
            case FTP_RNTO:
                break;
            case FTP_SITE:
                break;
            case FTP_STAT:
                break;
            case FTP_STOU:
                break;
            case FTP_STRU:
                break;
            case FTP_SYST:
                break;
            case FTP_TYPE:
                break;
            case FTP_LOGIN:
                break;
        }

        if (listener.isError()) {
            throw new FtpException(listener.getError());
        }

        return is;
    }

    private static InputStream inputStreamFtps(final Ftps ftps,
                                               final String arguments,
                                               final FtpMethod ftpMethod) throws FtpException {
        InputStream is;

        final FtpListener listener = new FtpListener();
        ftps.addFtpListener( listener );
        switch (ftpMethod.getFtpMethodEnum()) {
            case FTP_MLSD:
            case FTP_LIST:
                List<FtpFile> fileList = new ArrayList<>();
                Enumeration<FtpFile> fileEnum = ftps.getDirListing();
                while (fileEnum.hasMoreElements())  {
                    FtpFile file = fileEnum.nextElement();
                    fileList.add(file);
                }
                //Build XML as a String
                Document doc = createXml(fileList);
                String files = getStringFromDoc(doc);
                is = new ByteArrayInputStream(files.getBytes());
                break;
            case FTP_MDTM:
                Date lastModifiedTime = ftps.getFileTimestamp(arguments);
                long time = lastModifiedTime.getTime();
                is =  new ByteArrayInputStream(String.valueOf(time).getBytes());
                break;
            case FTP_NLST:
                StringBuilder sb = new StringBuilder();
                Enumeration<String> fileNames = ftps.getNameListing();
                while (fileNames.hasMoreElements())  {
                    String file = fileNames.nextElement();
                    sb.append(file);
                    sb.append("\r\n");
                }
                is = new ByteArrayInputStream(sb.toString().getBytes());
                break;
            case FTP_SIZE:
                long fileSize = ftps.getFilesize(arguments);
                is =  new ByteArrayInputStream(Long.toString(fileSize).getBytes());
                break;
            case FTP_USER:
                String userName = ftps.getUsername();
                is =  new ByteArrayInputStream(userName.getBytes());
                break;
            case FTP_PASS:
                String password = ftps.getPassword();
                is =  new ByteArrayInputStream(password.getBytes());
                break;
            default:
                String response = ftps.issueCommand(ftpMethod.getWspName());
                listener.responseReceived(new FtpResponseEvent(ftps, response));
                is = new ByteArrayInputStream(response.getBytes());
        }

        if (listener.isError()) {
            throw new FtpException(listener.getError());
        }

        return is;
    }

    private void upload(final Ftps ftps,
                        final InputStream is,
                        final long count,
                        final String filename,
                        final FtpMethod.FtpMethodEnum ftpMethod) throws FtpException {


        final FtpUtils.FtpUploadSizeListener listener = new FtpUtils.FtpUploadSizeListener();
        ftps.addFtpListener(listener);

        switch (ftpMethod){
            case FTP_PUT:
                ftps.upload(is, filename);
                break;
            case FTP_APPE:
                ftps.upload(is, filename, true);
                break;
            case FTP_STOU:
                ftps.uploadUnique(is, filename);
                break;
            default:
                ftps.upload(is, filename);
        }

        if (listener.isError()) {
            throw new FtpException(listener.getError());
        } else if (listener.getSize() < count) {
            throw new FtpException("File '" + filename + "' upload truncated to " + listener.getSize() + " bytes.");
        }
    }

    private void upload(final Ftp ftp,
                        final InputStream is,
                        final long count,
                        final FtpMethod.FtpMethodEnum ftpMethod,
                        final String filename)
            throws FtpException {

        final FtpUtils.FtpUploadSizeListener listener = new FtpUtils.FtpUploadSizeListener();

        ftp.addFtpListener(listener);
        switch (ftpMethod){
            case FTP_PUT:
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

        if (listener.isError()) {
            throw new FtpException(listener.getError());
        } else if (listener.getSize() < count) {
            throw new FtpException("File '" + filename + "' upload truncated to " + listener.getSize() + " bytes.");
        }
    }

    private String expandVariables(PolicyEnforcementContext context, String pattern) {
        final String[] variablesUsed = Syntax.getReferencedNames(pattern);
        final Map<String, Object> vars = context.getVariableMap(variablesUsed, getAudit());
        return ExpandVariables.process(pattern, vars, getAudit());
    }

    /*
    * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
    * that would otherwise keep our instances from getting collected.
    */
    public static void onModuleUnloaded() {
        logger.log(Level.INFO, "ServerConcurrentAllAssertion is preparing itself to be unloaded; shutting down assertion executor");
        assertionExecutor.shutdownNow();
    }



    public static String getStringFromDoc(Document doc) {
        DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
        LSSerializer lsSerializer = domImplementation.createLSSerializer();
        return lsSerializer.writeToString(doc);
    }

    /**
     * A function to convert a list of Files to a Document
     * @param fileList
     * @return a Document with the file list structure
     */
    public static Document createXml(List<FtpFile> fileList) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder parser;

        try {
            parser = factory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            return null;
        }

        Document doc = parser.newDocument();
        Element fileListElement = doc.createElement("fileList");
        doc.appendChild(fileListElement);

        for (FtpFile f : fileList) {
            Element name = doc.createElement("name");
            Element size = doc.createElement("size");
            Element owner = doc.createElement("owner");
            Element permission = doc.createElement("permission");
            Element date = doc.createElement("date");
            Element time = doc.createElement("time");
            Element raw = doc.createElement("raw");

            if (f.isDirectory()){
                Element dir = doc.createElement("dir");
                fileListElement.appendChild(dir);
                dir.appendChild(name);
                name.setTextContent(f.getFilename());
                dir.appendChild(size);
                size.setTextContent(Long.valueOf(f.getFilesize()).toString());
                dir.appendChild(owner);
                owner.setTextContent(f.getOwner());
                dir.appendChild(permission);
                permission.setTextContent(f.getPermission());
                dir.appendChild(date);
                date.setTextContent(f.getDate());
                dir.appendChild(time);
                time.setTextContent(f.getTime());
                dir.appendChild(raw);
                raw.setTextContent(f.toString());
            } else {
                Element file = doc.createElement("file");
                fileListElement.appendChild(file);
                file.appendChild(name);
                name.setTextContent(f.getFilename());
                file.appendChild(size);
                size.setTextContent(Long.valueOf(f.getFilesize()).toString());
                file.appendChild(owner);
                owner.setTextContent(f.getOwner());
                file.appendChild(permission);
                permission.setTextContent(f.getPermission());
                file.appendChild(date);
                date.setTextContent(f.getDate());
                file.appendChild(time);
                time.setTextContent(f.getTime());
                file.appendChild(raw);
                raw.setTextContent(f.toString());
            }
        }

        return doc;
    }

    private FtpConnectionPoolManager getFtpConnectionPoolManager() {
        FtpConnectionPoolManager ftpConnectionPoolManager = this.ftpConnectionPoolManager;

        if (ftpConnectionPoolManager == null) {
            ftpConnectionPoolManager = new FtpConnectionPoolManager();
            ftpConnectionPoolManager.setBindingTimeout(ftpConnectionIdleTime);
        }

        return ftpConnectionPoolManager;
    }

    private FtpsConnectionPoolManager getFtpsConnectionPoolManager() {
        FtpsConnectionPoolManager ftpsConnectionPoolManager = this.ftpsConnectionPoolManager;

        if (ftpsConnectionPoolManager == null) {
            ftpsConnectionPoolManager = new FtpsConnectionPoolManager();
            ftpsConnectionPoolManager.setBindingTimeout(ftpConnectionIdleTime);
        }

        return ftpsConnectionPoolManager;
    }

    private static final class FtpListener extends FtpAdapter {
        private static final String CODE_CONN_OPEN_START_TRANS = "125";
        private static final String CODE_FILE_STATUS_OK_DATA_OPEN = "150";
        private static final String CODE_FILE_ACTION_NOT_TAKEN = "450";
        private static final String CODE_FILE_ACTION_ABORT_ERROR = "451";
        private static final String CODE_FILE_ACTION_NO_SPACE = "452";
        private static final String CODE_FILE_NOT_FOUND = "550";
        private static final String CODE_FILE_SYNTAX_ERROR = "501";
        private static final String CODE_FILE_COMMAND_NOT_IMPLEMENTED = "502";

        private String error;

        public boolean isError() {
            return error != null;
        }

        public String getError() {
            return error;
        }

        @Override
        public void responseReceived(final FtpResponseEvent ftpResponseEvent) {
            final String response = ftpResponseEvent.getResponse();
            if ( response.startsWith(CODE_CONN_OPEN_START_TRANS) ||
                            response.startsWith(CODE_FILE_STATUS_OK_DATA_OPEN)) {
                error = null;
            } else if (response.startsWith(CODE_FILE_ACTION_NOT_TAKEN) ||
                       response.startsWith(CODE_FILE_ACTION_ABORT_ERROR) ||
                       response.startsWith(CODE_FILE_ACTION_NO_SPACE) ||
                       response.startsWith(CODE_FILE_NOT_FOUND) ||
                       response.startsWith(CODE_FILE_SYNTAX_ERROR) ||
                       response.startsWith(CODE_FILE_COMMAND_NOT_IMPLEMENTED)) {
                error = response;
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
}