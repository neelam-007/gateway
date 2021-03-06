package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.common.ftp.FtpCommand;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.transport.ftp.*;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test coverage of FTP Routing Assertion
 *
 * @author jwilliams
 */
public class ServerFtpRoutingAssertionTest {
    // FTP server settings
    private static final int SERVER_CONTROL_PORT = 22221;

    // resources
    private static final String RES_LOG_TXT_FILE_NAME = "log.txt";
    private static final String RES_DATA_XML_FILE_NAME = "acme_products.xml";

    // file system
    private static final String PATH_SEPARATOR = "/";
    private static final String FS_HOME_DIR = "/";
    private static final String FS_LOG_DIR = FS_HOME_DIR + "logs";
    private static final String FS_LOG_TXT_FILE = FS_LOG_DIR + PATH_SEPARATOR + RES_LOG_TXT_FILE_NAME;
    private static final String FS_DATA_DIR = FS_HOME_DIR + "data";
    private static final String FS_DATA_XML_FILE = FS_DATA_DIR + PATH_SEPARATOR + RES_DATA_XML_FILE_NAME;

    // user accounts
    private static final String USER_ACCOUNT_NAME = "jdoe";
    private static final String USER_ACCOUNT_PASSWORD = "password";

    private static final List<Authority> USER_AUTHORITIES =
            Arrays.asList(new ConcurrentLoginPermission(10, 10),
                    new TransferRatePermission(0, 0),
                    new WritePermission("/"));

    // other settings
    private final FtpFileDetails FS_HOME_DIR_CHANGE = new FtpFileDetails("/", FS_LOG_DIR,
            "", Charsets.UTF8);
    private final FtpFileDetails LOG_FILE_DETAILS = new FtpFileDetails(FS_LOG_TXT_FILE, FS_LOG_DIR,
            readResourceAsString(RES_LOG_TXT_FILE_NAME), Charsets.UTF8);
    private final FtpFileDetails XML_FILE_DETAILS = new FtpFileDetails(FS_DATA_XML_FILE, FS_DATA_DIR,
            readResourceAsString(RES_DATA_XML_FILE_NAME), Charsets.UTF8);

    private final InetAddress LOCALHOST = InetAddress.getLocalHost();

    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;
    private FakeFtpServer fakeFtpServer;

    private final LoginCredentials defaultLoginCredentials =
            LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
                SecurityTokenType.FTP_CREDENTIAL,
                USER_ACCOUNT_NAME,
                USER_ACCOUNT_PASSWORD.toCharArray()),
            FtpRoutingAssertion.class);

    public ServerFtpRoutingAssertionTest() throws IOException {
    }

    @Before
    public void setUp() {
        testAudit = new TestAudit();
        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());

        fakeFtpServer = makeFakeFtpServer();
        fakeFtpServer.start();
    }

    @After
    public void tearDown() {
        if(fakeFtpServer.isStarted()) {
            fakeFtpServer.stop();
        }

        System.setSecurityManager(originalSecurityManager);
    }

    @Test
    public void testCWD_Success() throws Exception {

        FtpFileDetails testDirectory = FS_HOME_DIR_CHANGE;
        FtpCommand testCommand = FtpCommand.CWD;

        // set up file system
        addDirectory(fakeFtpServer.getFileSystem(), testDirectory);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand( testCommand );
        assertion.setArguments( testDirectory.getDirectory());
        assertion.setDirectory( FS_HOME_DIR );
        assertion.setSecurity( FtpSecurity.FTP_UNSECURED );
        assertion.setCredentialsSource( FtpCredentialsSource.PASS_THRU );

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer( assertion );

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext( testDirectory );

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest( context );

        assertEquals( AssertionStatus.NONE, status );
        assertTrue( testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED) );

        validateFtpResponseKnob(context, 250, "CWD completed. New directory is "+testDirectory.getDirectory()+".");
    }

    @Test
    public void testCDUP_Success() throws Exception {

        FtpFileDetails testDirectory = FS_HOME_DIR_CHANGE;
        FtpCommand testCommand = FtpCommand.CDUP;

        // set up file system
        addDirectory(fakeFtpServer.getFileSystem(), testDirectory);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand( testCommand );
        assertion.setDirectory( testDirectory.getDirectory() );
        assertion.setSecurity( FtpSecurity.FTP_UNSECURED );
        assertion.setCredentialsSource( FtpCredentialsSource.PASS_THRU );

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer( assertion );

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext( testDirectory );

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest( context );

        assertEquals( AssertionStatus.NONE, status );
        assertTrue( testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED) );

        validateFtpResponseKnob(context, 200, "CDUP completed. New directory is "+FS_HOME_DIR+".");
    }

    @Test
    public void testPWD_Success() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;
        FtpCommand testCommand = FtpCommand.PWD;

        // set up file system
        addFile(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(testCommand);
        assertion.setArguments(testFile.getName());
        assertion.setDirectory(testFile.getDirectory());
        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));

        validateFtpResponseKnob(context, 257, "\"" + testFile.getDirectory() + "\"");
    }

    @Test
    public void testACCT_FailOnUnrecognizedCommand() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;
        String testCommand = "ACCT";

        // set up file system
        addFile(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setCommandFromVariable(true);
        assertion.setFtpCommandVariable(testCommand);
        assertion.setDirectory(testFile.getDirectory());
        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = null;

        try {
            serverAssertion.checkRequest(context);
        } catch (AssertionStatusException e) {
            status = e.getAssertionStatus();
        }

        assertEquals(AssertionStatus.SERVER_ERROR, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.NO_SUCH_VARIABLE_WARNING));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));
        assertNull(context.getResponse().getKnob(FtpResponseKnob.class));
    }

    @Test
    public void testNullFtpCommand_Fail() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;

        // set up file system
        addFile(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(null);
        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status;

        try {
            status = serverAssertion.checkRequest(context);
        } catch (AssertionStatusException e) {
            status = e.getAssertionStatus();
        }

        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_NO_COMMAND));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));
        assertNull(context.getResponse().getKnob(FtpResponseKnob.class));
    }

    /**
     * This test corresponds to handling an upload request with a pre-Icefish policy (i.e. no command set, but
     * argument provided). The default command "STOR" should be assumed and the assertion should succeed.
     */
    @Test
    public void testNoCommand_ArgumentProvided_Success() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;

        // set up file system
        addDirectory(fakeFtpServer.getFileSystem(), testFile);

        // create assertion, don't set an FTP command (use default of STOR)
        FtpRoutingAssertion assertion = createAssertion();

        fillFileSettings(assertion, testFile);

        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));
        assertEquals(getFileSystemEntryContent(testFile), testFile.getContent());

        validateFtpResponseKnob(context, 226, "Created file " + testFile.getName() + ".");
    }

    @Test
    public void testLIST_Success() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;
        FtpCommand testCommand = FtpCommand.LIST;

        // set up file system
        FileEntry testFileEntry = addFile(fakeFtpServer.getFileSystem(), testFile);
        String listItem = new UnixDirectoryListingFormatter().format(testFileEntry) + "\r\n";

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(testCommand);
        assertion.setDirectory(testFile.getDirectory());
        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));

        String responseContent =
                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));

        assertEquals(listItem, responseContent);

        validateFtpResponseKnob(context, 226, "Closing data connection. Requested file action successful.");
    }

    @Test
    public void testRETR_LogFile_Success() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;
        FtpCommand testCommand = FtpCommand.RETR;

        // set up file system
        addFile(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(testCommand);

        fillFileSettings(assertion, testFile);

        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));

        String responseContent =
                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));

        assertEquals(testFile.getContent(), responseContent);

        validateFtpResponseKnob(context, 150, null);
    }

    @Test
    public void testRETR_XmlFile_Success() throws Exception {
        FtpFileDetails testFile = XML_FILE_DETAILS;
        FtpCommand testCommand = FtpCommand.RETR;

        // set up file system
        addFile(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(testCommand);

        fillFileSettings(assertion, testFile);

        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));

        String responseContent =
                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));

        assertEquals(testFile.getContent(), responseContent);

        validateFtpResponseKnob(context, 150, null);
    }

    @Test
    public void testSTOR_LogFile_Success() throws Exception {
        FtpFileDetails testFile = LOG_FILE_DETAILS;
        FtpCommand testCommand = FtpCommand.STOR;

        // set up file system
        addDirectory(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(testCommand);

        fillFileSettings(assertion, testFile);

        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));
        assertEquals(getFileSystemEntryContent(testFile), testFile.getContent());

        validateFtpResponseKnob(context, 226, "Created file " + testFile.getName() + ".");
    }

    @Test
    public void testSTOR_XmlFile_Success() throws Exception {
        FtpFileDetails testFile = XML_FILE_DETAILS;
        FtpCommand testCommand = FtpCommand.STOR;

        // set up file system
        addDirectory(fakeFtpServer.getFileSystem(), testFile);

        // create assertion
        FtpRoutingAssertion assertion = createAssertion();

        assertion.setFtpCommand(testCommand);

        fillFileSettings(assertion, testFile);

        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);

        // create server assertion
        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);

        // create context
        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);

        // run server assertion
        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.FTP_ROUTING_SUCCEEDED));
        assertEquals(getFileSystemEntryContent(testFile), testFile.getContent());

        validateFtpResponseKnob(context, 226, "Created file " + testFile.getName() + ".");
    }

    //----------- VALIDATION -------

    private void validateFtpResponseKnob(PolicyEnforcementContext context, int code, @Nullable String data) {
        FtpResponseKnob ftpResponseKnob = context.getResponse().getKnob(FtpResponseKnob.class);

        assertNotNull(ftpResponseKnob);
        assertEquals(code, ftpResponseKnob.getReplyCode());

        if (null == ftpResponseKnob.getReplyText()) {   // null is valid and indicates the reply is unset
            assertEquals(data, ftpResponseKnob.getReplyText());
        } else {
            assertEquals(data, ftpResponseKnob.getReplyText().trim());
        }
    }

    //----------- SETUP -----------=

    private String getFileSystemEntryContent(FtpFileDetails fileDetails) throws IOException {
        FileSystem fileSystem = fakeFtpServer.getFileSystem();
        FileEntry fileEntry = (FileEntry) fileSystem.getEntry(fileDetails.getName());
        return new String(IOUtils.slurpStream(fileEntry.createInputStream()), fileDetails.getCharset());
    }

    private void fillFileSettings(FtpRoutingAssertion assertion, FtpFileDetails file) {
        assertion.setArguments(file.getName());
        assertion.setDirectory(file.getDirectory());
    }

    private class FtpFileDetails {
        private final String name;
        private final String directory;
        private final String content;
        private final Charset charset;

        public FtpFileDetails(String fileName, String directory, String content, Charset charset) {
            this.name = fileName;
            this.directory = directory;
            this.content = content;
            this.charset = charset;
        }

        public String getName() {
            return name;
        }

        public String getDirectory() {
            return directory;
        }

        public String getContent() {
            return content;
        }

        public Charset getCharset() {
            return charset;
        }
    }

    private FtpRoutingAssertion createAssertion() {
        FtpRoutingAssertion assertion = new FtpRoutingAssertion();

        assertion.setPort(Integer.toString(SERVER_CONTROL_PORT));
        assertion.setHostName("localhost");

        return assertion;
    }

    private ServerFtpRoutingAssertion createServer(FtpRoutingAssertion assertion) throws PolicyAssertionException {
        ServerFtpRoutingAssertion serverAssertion =
                new ServerFtpRoutingAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .put("stashManagerFactory", TestStashManagerFactory.getInstance())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(FtpFileDetails fileDetails) throws IOException {
        return createPolicyEnforcementContext(fileDetails, defaultLoginCredentials);
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(FtpFileDetails fileDetails, LoginCredentials loginCredentials) throws IOException {
        Message request = createFtpRequest(buildUser(), fileDetails.getName(), "test_endpoint", fileDetails.getContent(), false, false);

        Message response = createResponse();

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        context.getAuthenticationContext(context.getRequest()).addCredentials(loginCredentials);

        return context;
    }

    private Message createFtpRequest(final User user, final String file, final String path, final String fileContents,
                                     final boolean secure, final boolean unique) throws IOException {
        Message request = new Message();

        request.initialize(createStashManager(),
                ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(fileContents.getBytes()), Message.getMaxBytes());

        // attach knob to request representing org.apache.ftpserver.ftplet.FtpSession information
        request.attachFtpRequestKnob(
                new FtpRequestKnob() {
                    @Override
                    public int getLocalPort() {
                        return getLocalListenerPort();
                    }

                    @Override
                    public int getLocalListenerPort() {
                        return SERVER_CONTROL_PORT;
                    }

                    @Override
                    public String getRemoteAddress() {
                        return LOCALHOST.getHostAddress();
                    }

                    @Override
                    public String getRemoteHost() {
                        return LOCALHOST.getHostAddress();
                    }

                    @Override
                    public int getRemotePort() {
                        return 0;
                    }

                    @Override
                    public String getLocalAddress() {
                        return LOCALHOST.getHostAddress();
                    }

                    @Override
                    public String getLocalHost() {
                        return LOCALHOST.getHostAddress();
                    }

                    @Override
                    public String getCommand() {
                        return null;
                    }

                    @Override
                    public String getArgument() {
                        return null;
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
                        urlBuffer.append(InetAddressUtil.getHostForUrl(LOCALHOST.getHostAddress()));
                        urlBuffer.append(":");
                        urlBuffer.append(SERVER_CONTROL_PORT);
                        urlBuffer.append(path);
                        if (!path.endsWith("/"))
                            urlBuffer.append("/");
                        urlBuffer.append(file);

                        return urlBuffer.toString();
                    }

                    @Nullable
                    @Override
                    public X509Certificate[] getClientCertificate() throws IOException {
                        return null;
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
                }
        );

        return request;
    }

    @SuppressWarnings("UnusedDeclaration")
    private Message createHttpRequest(final InetAddress serverAddress,
                                      final int serverPort,
                                      final InetAddress clientAddress,
                                      final User user,
                                      final String file,
                                      final String path,
                                      final boolean secure,
                                      final boolean unique) throws IOException {
        // Create request message
        Message request = new Message();

        request.initialize(createStashManager(),
                ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(new byte[0]), Message.getMaxBytes());

        request.attachFtpRequestKnob(
                new FtpRequestKnob() {
                    @Override
                    public int getLocalPort() {
                        return getLocalListenerPort();
                    }

                    @Override
                    public int getLocalListenerPort() {
                        return serverPort;
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
                        return null;
                    }

                    @Override
                    public String getArgument() {
                        return null;
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
                        urlBuffer.append(serverPort);
                        urlBuffer.append(path);
                        if (!path.endsWith("/"))
                            urlBuffer.append("/");
                        urlBuffer.append(file);

                        return urlBuffer.toString();
                    }

                    @Nullable
                    @Override
                    public X509Certificate[] getClientCertificate() throws IOException {
                        return null;
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
                }
        );

        return request;
    }

    private Message createResponse() {
        return new Message();
    }

    private StashManager createStashManager() {
        return TestStashManagerFactory.getInstance().createStashManager();
    }

    /**
     * Create FakeFtpServer object and configure with user account, file system, and server settings.
     *
     * @return FakeFtpServer
     */
    private FakeFtpServer makeFakeFtpServer() {
        FakeFtpServer server = new FakeFtpServer();
        server.setServerControlPort(SERVER_CONTROL_PORT);
        server.addUserAccount(new UserAccount(USER_ACCOUNT_NAME, USER_ACCOUNT_PASSWORD, FS_HOME_DIR));

        FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry(FS_HOME_DIR));
        server.setFileSystem(fileSystem);

        return server;
    }

    /**
     * Adds a directory using the specified details to the FakeFtpServer FileSystem
     */
    private void addDirectory(FileSystem fileSystem, FtpFileDetails fileDetails) {
        fileSystem.add(new DirectoryEntry(fileDetails.getDirectory()));
    }

    /**
     * Adds a file with the specified details to the FakeFtpServer FileSystem
     */
    private FileEntry addFile(FileSystem fileSystem, FtpFileDetails fileDetails) {
        FileEntry fileEntry = new FileEntry(fileDetails.getName(), fileDetails.getContent());

        fileSystem.add(new DirectoryEntry(fileDetails.getDirectory()));
        fileSystem.add(fileEntry);

        return fileEntry;
    }

    private User buildUser() {
        return buildUser(USER_ACCOUNT_NAME, USER_ACCOUNT_PASSWORD);
    }

    private User buildUser(String userName, String password) {
        BaseUser user = new BaseUser();

        user.setEnabled(true);
        user.setName(userName);
        user.setPassword(password);
        user.setHomeDirectory("/");
        user.setMaxIdleTime(60);
        user.setAuthorities(USER_AUTHORITIES);

        return user;
    }

    /**
     * Fetch the contents of the specified resource, decoded using the UTF-8 Charset
     * @throws IOException
     */
    private String readResourceAsString(String resource) throws IOException {
        return readResourceAsString(resource, Charsets.UTF8);
    }

    /**
     * Fetch the contents of the specified resource, decoded using the specified Charset
     * @param resource filename of resource
     * @param charset charset to use in creating String from byte array
     * @return contents of the specified resource file
     * @throws IOException
     */
    private String readResourceAsString(String resource, Charset charset) throws IOException {
        return new String(IOUtils.slurpStream(getClass().getResourceAsStream(resource)), charset);
    }
}
