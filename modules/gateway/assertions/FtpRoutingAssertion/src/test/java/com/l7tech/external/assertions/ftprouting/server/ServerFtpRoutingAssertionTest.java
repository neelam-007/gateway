package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.ftprouting.FtpRoutingAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.transport.ftp.FtpCredentialsSource;
import com.l7tech.gateway.common.transport.ftp.FtpFileNameSource;
import com.l7tech.gateway.common.transport.ftp.FtpSecurity;
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
import com.l7tech.gateway.common.transport.ftp.FtpMethod;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.*;
import org.apache.ftpserver.ftplet.*;
import org.apache.ftpserver.usermanager.BaseUser;
import org.apache.ftpserver.usermanager.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.TransferRatePermission;
import org.apache.ftpserver.usermanager.WritePermission;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.*;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test coverage of FTP Routing Assertion
 *
 * @author jwilliams
 */
public class ServerFtpRoutingAssertionTest {
    // XPath expressions
    private static final String RAW_NODE_XPATH = "//raw";

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

    // other settings?

    // TODO jwilliams: remove temporary test once commit is complete
    @Test
    public void testAcct() throws Exception {
        assertEquals("jdoe", USER_ACCOUNT_NAME);
    }

//    private final FtpFileDetails LOG_FILE_DETAILS = new FtpFileDetails(FS_LOG_TXT_FILE, FS_LOG_DIR,
//            readResourceAsString(RES_LOG_TXT_FILE_NAME), ContentTypeHeader.TEXT_DEFAULT.toString(), Charsets.UTF8);
//    private final FtpFileDetails XML_FILE_DETAILS = new FtpFileDetails(FS_DATA_XML_FILE, FS_DATA_DIR,
//            readResourceAsString(RES_DATA_XML_FILE_NAME), ContentTypeHeader.XML_DEFAULT.toString(), Charsets.UTF8);
//
//    private final InetAddress LOCALHOST = InetAddress.getLocalHost();
//
//    private TestAudit testAudit;
//    private SecurityManager originalSecurityManager;
//    private FakeFtpServer fakeFtpServer;
//
//    private final LoginCredentials defaultLoginCredentials =
//            LoginCredentials.makeLoginCredentials(new UsernamePasswordSecurityToken(
//                SecurityTokenType.FTP_CREDENTIAL,
//                USER_ACCOUNT_NAME,
//                USER_ACCOUNT_PASSWORD.toCharArray()),
//            FtpRoutingAssertion.class);
//
//    public ServerFtpRoutingAssertionTest() throws IOException {
//    }
//
//    @Before
//    public void setUp() {
//        testAudit = new TestAudit();
//        originalSecurityManager = System.getSecurityManager();
//        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
//
//        fakeFtpServer = makeFakeFtpServer();
//        fakeFtpServer.start();
//    }
//
//    @After
//    public void tearDown() {
//        if(fakeFtpServer.isStarted()) {
//            fakeFtpServer.stop();
//        }
//
//        System.setSecurityManager(originalSecurityManager);
//    }
//
//    @Test
//    public void testAcct() throws Exception {
//        FtpFileDetails testFile = LOG_FILE_DETAILS;
//        FtpMethod testMethod = FtpMethod.FTP_ACCT;
//
//        // set up file system
//        addFile(fakeFtpServer.getFileSystem(), testFile);
//
//        // create assertion
//        FtpRoutingAssertion assertion = createAssertion();
//
//        assertion.setFtpMethod(testMethod);
//        assertion.setDirectory(testFile.getDirectory());
//        assertion.setDownloadedContentType(ContentTypeHeader.XML_DEFAULT.toString());
//        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
//        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
//
//        // create server assertion
//        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);
//
//        // create context
//        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);
//
//        // run server assertion
//        AssertionStatus status = serverAssertion.checkRequest(context);
//
//        assertEquals(AssertionStatus.NONE, status);
//
//        String responseContent =
//                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));
//
//        assertEquals("230 ACCT completed for jdoe.", responseContent.trim());
//    }
//
//    @Test
//    public void testList() throws Exception {
//        FtpFileDetails testFile = LOG_FILE_DETAILS;
//        FtpMethod testMethod = FtpMethod.FTP_LIST;
//
//        // set up file system
//        FileEntry testFileEntry = addFile(fakeFtpServer.getFileSystem(), testFile);
//        String listItem = new UnixDirectoryListingFormatter().format(testFileEntry);
//
//        // create assertion
//        FtpRoutingAssertion assertion = createAssertion();
//
//        assertion.setFtpMethod(testMethod);
//        assertion.setDirectory(testFile.getDirectory());
//        assertion.setDownloadedContentType(ContentTypeHeader.XML_DEFAULT.toString());
//        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
//        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
//
//        // create server assertion
//        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);
//
//        // create context
//        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);
//
//        // run server assertion
//        AssertionStatus status = serverAssertion.checkRequest(context);
//
//        assertEquals(AssertionStatus.NONE, status);
//
//        String responseContent =
//                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));
//
//        List<Element> elementList =
//                XpathUtil.findElements(XmlUtil.parse(responseContent).getDocumentElement(), RAW_NODE_XPATH, null);
//
//        assertEquals(1, elementList.size());
//        assertEquals(listItem, elementList.get(0).getTextContent());
//    }
//
//    @Test
//    public void testGetLogFile() throws Exception {
//        FtpFileDetails testFile = LOG_FILE_DETAILS;
//        FtpMethod testMethod = FtpMethod.FTP_GET;
//
//        // set up file system
//        addFile(fakeFtpServer.getFileSystem(), testFile);
//
//        // create assertion
//        FtpRoutingAssertion assertion = createAssertion();
//
//        assertion.setFtpMethod(testMethod);
//
//        setPattern(assertion, testFile);
//
//        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
//        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
//
//        // create server assertion
//        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);
//
//        // create context
//        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);
//
//        // run server assertion
//        AssertionStatus status = serverAssertion.checkRequest(context);
//
//        assertEquals(AssertionStatus.NONE, status);
//
//        String responseContent =
//                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));
//
//        assertEquals(testFile.getContent(), responseContent);
//    }
//
//    @Test
//    public void testGetXmlFile() throws Exception {
//        FtpFileDetails testFile = XML_FILE_DETAILS;
//        FtpMethod testMethod = FtpMethod.FTP_GET;
//
//        // set up file system
//        addFile(fakeFtpServer.getFileSystem(), testFile);
//
//        // create assertion
//        FtpRoutingAssertion assertion = createAssertion();
//
//        assertion.setFtpMethod(testMethod);
//
//        setPattern(assertion, testFile);
//
//        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
//        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
//
//        // create server assertion
//        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);
//
//        // create context
//        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);
//
//        // run server assertion
//        AssertionStatus status = serverAssertion.checkRequest(context);
//
//        assertEquals(AssertionStatus.NONE, status);
//
//        String responseContent =
//                new String(IOUtils.slurpStream(context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream()));
//
//        assertEquals(testFile.getContent(), responseContent);
//    }
//
//    @Test
//    public void testPutLogFile() throws Exception {
//        FtpFileDetails testFile = LOG_FILE_DETAILS;
//        FtpMethod testMethod = FtpMethod.FTP_PUT;
//
//        // set up file system
//        addDirectory(fakeFtpServer.getFileSystem(), testFile);
//
//        // create assertion
//        FtpRoutingAssertion assertion = createAssertion();
//
//        assertion.setFtpMethod(testMethod);
//
//        setPattern(assertion, testFile);
//
//        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
//        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
//
//        // create server assertion
//        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);
//
//        // create context
//        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);
//
//        // run server assertion
//        AssertionStatus status = serverAssertion.checkRequest(context);
//
//        assertEquals(AssertionStatus.NONE, status);
//        assertEquals(getFileSystemEntryContent(testFile), testFile.getContent());
//    }
//
//    @Test
//    public void testPutXmlFile() throws Exception {
//        FtpFileDetails testFile = XML_FILE_DETAILS;
//        FtpMethod testMethod = FtpMethod.FTP_PUT;
//
//        // set up file system
//        addDirectory(fakeFtpServer.getFileSystem(), testFile);
//
//        // create assertion
//        FtpRoutingAssertion assertion = createAssertion();
//
//        assertion.setFtpMethod(testMethod);
//
//        setPattern(assertion, testFile);
//
//        assertion.setSecurity(FtpSecurity.FTP_UNSECURED);
//        assertion.setCredentialsSource(FtpCredentialsSource.PASS_THRU);
//
//        // create server assertion
//        ServerFtpRoutingAssertion serverAssertion = createServer(assertion);
//
//        // create context
//        final PolicyEnforcementContext context = createPolicyEnforcementContext(testFile);
//
//        // run server assertion
//        AssertionStatus status = serverAssertion.checkRequest(context);
//
//        assertEquals(AssertionStatus.NONE, status);
//        assertEquals(getFileSystemEntryContent(testFile), testFile.getContent());
//    }
//
//    private String getFileSystemEntryContent(FtpFileDetails fileDetails) throws IOException {
//        FileSystem fileSystem = fakeFtpServer.getFileSystem();
//        FileEntry fileEntry = (FileEntry) fileSystem.getEntry(fileDetails.getName());
//        return new String(IOUtils.slurpStream(fileEntry.createInputStream()), fileDetails.getCharset());
//    }
//
//    private void setPattern(FtpRoutingAssertion assertion, FtpFileDetails file) {
//        assertion.setFileNameSource(FtpFileNameSource.ARGUMENT);
//        assertion.setArguments(file.getName());
//        assertion.setDirectory(file.getDirectory());
//        assertion.setDownloadedContentType(file.getContentType());
//    }
//
//    private class FtpFileDetails {
//        private final String name;
//        private final String directory;
//        private final String contentType;
//        private final String content;
//        private final Charset charset;
//
//        public FtpFileDetails(String fileName, String directory, String content, String contentType, Charset charset) {
//            this.name = fileName;
//            this.directory = directory;
//            this.content = content;
//            this.contentType = contentType;
//            this.charset = charset;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public String getDirectory() {
//            return directory;
//        }
//
//        public String getContent() {
//            return content;
//        }
//
//        public String getContentType() {
//            return contentType;
//        }
//
//        public Charset getCharset() {
//            return charset;
//        }
//    }
//
//    private FtpRoutingAssertion createAssertion() {
//        FtpRoutingAssertion assertion = new FtpRoutingAssertion();
//
//        assertion.setPort(Integer.toString(SERVER_CONTROL_PORT));
//        assertion.setHostName("localhost");
//
//        return assertion;
//    }
//
//    private ServerFtpRoutingAssertion createServer(FtpRoutingAssertion assertion) throws PolicyAssertionException {
//        ServerFtpRoutingAssertion serverAssertion =
//                new ServerFtpRoutingAssertion(assertion, ApplicationContexts.getTestApplicationContext());
//
//        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
//                .put("auditFactory", testAudit.factory())
//                .put("stashManagerFactory", TestStashManagerFactory.getInstance())
//                .unmodifiableMap()
//        );
//
//        return serverAssertion;
//    }
//
//    private PolicyEnforcementContext createPolicyEnforcementContext(FtpFileDetails fileDetails) throws IOException {
//        return createPolicyEnforcementContext(fileDetails, defaultLoginCredentials);
//    }
//
//    private PolicyEnforcementContext createPolicyEnforcementContext(FtpFileDetails fileDetails, LoginCredentials loginCredentials) throws IOException {
//        Message request = createFtpRequest(buildUser(), fileDetails.getName(), "test_endpoint", fileDetails.getContent(), false, false);
//
//        Message response = createResponse();
//
//        PolicyEnforcementContext context =
//                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
//
//        context.getAuthenticationContext(context.getRequest()).addCredentials(loginCredentials);
//
//        return context;
//    }
//
//    private Message createFtpRequest(FtpFileDetails fileDetails) throws IOException {
//        return createFtpRequest(buildUser(), fileDetails.getName(), fileDetails.getDirectory(), fileDetails.getContent(), false, false);
//    }
//
//    private Message createFtpRequest(final User user, final String file, final String path, final String fileContents,
//                                     final boolean secure, final boolean unique) throws IOException {
//        Message request = new Message();
//
//        request.initialize(createStashManager(),
//                ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(fileContents.getBytes()), Message.getMaxBytes());
//
//        // attach knob to request representing org.apache.ftpserver.ftplet.FtpSession information
//        request.attachFtpKnob(
//                new FtpRequestKnob() {
//                    @Override
//                    public int getLocalPort() {
//                        return getLocalListenerPort();
//                    }
//
//                    @Override
//                    public int getLocalListenerPort() {
//                        return SERVER_CONTROL_PORT;
//                    }
//
//                    @Override
//                    public String getRemoteAddress() {
//                        return LOCALHOST.getHostAddress();
//                    }
//
//                    @Override
//                    public String getRemoteHost() {
//                        return LOCALHOST.getHostAddress();
//                    }
//
//                    @Override
//                    public int getRemotePort() {
//                        return 0;
//                    }
//
//                    @Override
//                    public String getLocalAddress() {
//                        return LOCALHOST.getHostAddress();
//                    }
//
//                    @Override
//                    public String getLocalHost() {
//                        return LOCALHOST.getHostAddress();
//                    }
//
//                    @Override
//                    public String getFile() {
//                        return file;
//                    }
//
//                    @Override
//                    public String getPath() {
//                        return path;
//                    }
//
//                    @Override
//                    public String getRequestUri() {
//                        return path;
//                    }
//
//                    @Override
//                    public String getRequestUrl() {
//                        StringBuilder urlBuffer = new StringBuilder();
//
//                        urlBuffer.append(secure ? "ftps" : "ftp");
//                        urlBuffer.append("://");
//                        urlBuffer.append(InetAddressUtil.getHostForUrl(LOCALHOST.getHostAddress()));
//                        urlBuffer.append(":");
//                        urlBuffer.append(SERVER_CONTROL_PORT);
//                        urlBuffer.append(path);
//                        if (!path.endsWith("/"))
//                            urlBuffer.append("/");
//                        urlBuffer.append(file);
//
//                        return urlBuffer.toString();
//                    }
//
//                    @Override
//                    public boolean isSecure() {
//                        return secure;
//                    }
//
//                    @Override
//                    public boolean isUnique() {
//                        return unique;
//                    }
//
//                    @Override
//                    public PasswordAuthentication getCredentials() {
//                        PasswordAuthentication passwordAuthentication = null;
//                        if (user.getPassword() != null) {
//                            passwordAuthentication =
//                                    new PasswordAuthentication(user.getName(), user.getPassword().toCharArray());
//                        }
//                        return passwordAuthentication;
//                    }
//                }
//        );
//
//        return request;
//    }
//
//    private Message createHttpRequest(final InetAddress serverAddress,
//                                      final int serverPort,
//                                      final InetAddress clientAddress,
//                                      final User user,
//                                      final String file,
//                                      final String path,
//                                      final boolean secure,
//                                      final boolean unique) throws IOException {
//        // Create request message
//        Message request = new Message();
//
//        request.initialize(createStashManager(),
//                ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(new byte[0]), Message.getMaxBytes());
//
//        request.attachFtpKnob(
//                new FtpRequestKnob() {
//                    @Override
//                    public int getLocalPort() {
//                        return getLocalListenerPort();
//                    }
//
//                    @Override
//                    public int getLocalListenerPort() {
//                        return serverPort;
//                    }
//
//                    @Override
//                    public String getRemoteAddress() {
//                        return clientAddress.getHostAddress();
//                    }
//
//                    @Override
//                    public String getRemoteHost() {
//                        return clientAddress.getHostAddress();
//                    }
//
//                    @Override
//                    public int getRemotePort() {
//                        return 0;
//                    }
//
//                    @Override
//                    public String getLocalAddress() {
//                        return serverAddress.getHostAddress();
//                    }
//
//                    @Override
//                    public String getLocalHost() {
//                        return serverAddress.getHostAddress();
//                    }
//
//                    @Override
//                    public String getFile() {
//                        return file;
//                    }
//
//                    @Override
//                    public String getPath() {
//                        return path;
//                    }
//
//                    @Override
//                    public String getRequestUri() {
//                        return path;
//                    }
//
//                    @Override
//                    public String getRequestUrl() {
//                        StringBuilder urlBuffer = new StringBuilder();
//
//                        urlBuffer.append(secure ? "ftps" : "ftp");
//                        urlBuffer.append("://");
//                        urlBuffer.append(InetAddressUtil.getHostForUrl(serverAddress.getHostAddress()));
//                        urlBuffer.append(":");
//                        urlBuffer.append(serverPort);
//                        urlBuffer.append(path);
//                        if (!path.endsWith("/"))
//                            urlBuffer.append("/");
//                        urlBuffer.append(file);
//
//                        return urlBuffer.toString();
//                    }
//
//                    @Override
//                    public boolean isSecure() {
//                        return secure;
//                    }
//
//                    @Override
//                    public boolean isUnique() {
//                        return unique;
//                    }
//
//                    @Override
//                    public PasswordAuthentication getCredentials() {
//                        PasswordAuthentication passwordAuthentication = null;
//                        if (user.getPassword() != null) {
//                            passwordAuthentication =
//                                    new PasswordAuthentication(user.getName(), user.getPassword().toCharArray());
//                        }
//                        return passwordAuthentication;
//                    }
//                }
//        );
//
//        return request;
//    }
//
//    private Message createResponse() {
//        return new Message();
//    }
//
//    private StashManager createStashManager() {
//        return TestStashManagerFactory.getInstance().createStashManager();
//    }
//
//    /**
//     * Create FakeFtpServer object and configure with user account, file system, and server settings.
//     *
//     * @return FakeFtpServer
//     */
//    private FakeFtpServer makeFakeFtpServer() {
//        FakeFtpServer server = new FakeFtpServer();
//        server.setServerControlPort(SERVER_CONTROL_PORT);
//        server.addUserAccount(new UserAccount(USER_ACCOUNT_NAME, USER_ACCOUNT_PASSWORD, FS_HOME_DIR));
//
//        FileSystem fileSystem = new UnixFakeFileSystem();
//        fileSystem.add(new DirectoryEntry(FS_HOME_DIR));
//        server.setFileSystem(fileSystem);
//
//        return server;
//    }
//
//    /**
//     * Adds a directory using the specified details to the FakeFtpServer FileSystem
//     * @param fileSystem
//     * @param fileDetails
//     */
//    private void addDirectory(FileSystem fileSystem, FtpFileDetails fileDetails) {
//        fileSystem.add(new DirectoryEntry(fileDetails.getDirectory()));
//    }
//
//    /**
//     * Adds a file with the specified details to the FakeFtpServer FileSystem
//     * @param fileSystem
//     * @param fileDetails
//     */
//    private FileEntry addFile(FileSystem fileSystem, FtpFileDetails fileDetails) {
//        FileEntry fileEntry = new FileEntry(fileDetails.getName(), fileDetails.getContent());
//
//        fileSystem.add(new DirectoryEntry(fileDetails.getDirectory()));
//        fileSystem.add(fileEntry);
//
//        return fileEntry;
//    }
//
//    private User buildUser() {
//        return buildUser(USER_ACCOUNT_NAME, USER_ACCOUNT_PASSWORD);
//    }
//
//    private User buildUser(String userName, String password) {
//        BaseUser user = new BaseUser();
//
//        user.setEnabled(true);
//        user.setName(userName);
//        user.setPassword(password);
//        user.setHomeDirectory("/");
//        user.setMaxIdleTime(60);
//        user.setAuthorities(new Authority[]{
//                new ConcurrentLoginPermission(10, 10),
//                new TransferRatePermission(0, 0),
//                new WritePermission("/"),
//        });
//
//        return user;
//    }
//
//    /**
//     * Fetch the contents of the specified resource, decoded using the UTF-8 Charset
//     * @param resource
//     * @return
//     * @throws IOException
//     */
//    private String readResourceAsString(String resource) throws IOException {
//        return readResourceAsString(resource, Charsets.UTF8);
//    }
//
//    /**
//     * Fetch the contents of the specified resource, decoded using the specified Charset
//     * @param resource filename of resource
//     * @param charset charset to use in creating String from byte array
//     * @return contents of the specified resource file
//     * @throws IOException
//     */
//    private String readResourceAsString(String resource, Charset charset) throws IOException {
//        return new String(IOUtils.slurpStream(getClass().getResourceAsStream(resource)), charset);
//    }
}
