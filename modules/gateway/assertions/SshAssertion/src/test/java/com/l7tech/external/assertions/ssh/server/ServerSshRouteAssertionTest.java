package com.l7tech.external.assertions.ssh.server;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.CommandKnob;
import com.l7tech.message.Message;
import com.l7tech.message.SshKnob;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.ssh.SshSession;
import com.l7tech.server.ssh.SshSessionKey;
import com.l7tech.server.ssh.SshSessionPool;
import com.l7tech.server.ssh.client.*;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.Either;
import com.l7tech.util.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.xml.bind.JAXB;
import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

/**
 * Test SshRouteAssertion
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerSshRouteAssertionTest {

    private static final String fileName = "myFile.txt";
    private static final String fileDir = "myDir/";
    private static final long fileLength = 100;
    private static final long fileOffset = 0;
    private static final Goid passwordGoid = new Goid(0, 123L);
    private static final String username = "myUser";
    private static final String host = "myhost";
    private static final int port = 22;
    private static final Either<String, String> password = Either.left("password");
    private static final int connectionTimeout = 10000;
    private static final int socketTimeout = 60000;
    private static final String fingerPrint = null;
    public static final CommandKnob.CommandType commandType = CommandKnob.CommandType.GET;
    public static final String requestBody = "<myrequest/>";
    public static final String responseBody = "<myresponse/>";
    SshSessionKey key = new SshSessionKey(username, host, port, password, connectionTimeout, socketTimeout, fingerPrint, grep(map(list(ServerSshRouteAssertion.defaultCipherOrder.split("\\s*,\\s*")), trim()), isNotEmpty()), grep(map(list("hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96".split("\\s*,\\s*")), trim()), isNotEmpty()), grep(map(list(ServerSshRouteAssertion.defaultCompressionOrder.split("\\s*,\\s*")), trim()), isNotEmpty()));


    //Note Spy is needed here in order for inject mocks to work.
    @Mock
    SshSessionPool sshSessionPool;
    @Inject
    @Spy
    ServerConfigStub config;
    @Inject
    ApplicationContext applicationContext;
    @Spy
    ThreadPoolBean threadPool;
    @Spy
    StashManagerFactory stashManagerFactory = new StashManagerFactory() {
        @Override
        public StashManager createStashManager() {
            return new ByteArrayStashManager();
        }
    };
    @Spy
    private TestAudit testAudit;
    @Mock
    SecurePasswordManager securePasswordManager;

    @InjectMocks
    ServerSshRouteAssertion serverSshRouteAssertion;
    SshRouteAssertion assertion;

    @Mock
    SshSession sshSession;
    @Mock
    ScpClient scpClient;
    @Mock
    SftpClient sftpClient;

    private PolicyEnforcementContext peCtx;

    @Before
    public void setUp() throws Exception {
        threadPool = new ThreadPoolBean(config, "SSH2 Routing Response Download Thread Pool", "sshResponseDownloadThreadLimit", "ssh.responseDownloadThreadLimit", 20);
        threadPool.start();
        assertion = createSshRouteAssertion();
        final AuditFactory auditFactory = new TestAudit().factory();
        testAudit = (TestAudit) auditFactory.newInstance(null, null);
        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("auditFactory", auditFactory);
        serverSshRouteAssertion = new ServerSshRouteAssertion(assertion, new GenericApplicationContext(beanFactory, applicationContext));
        peCtx = makeContext(requestBody, responseBody);

        MockitoAnnotations.initMocks(this);
        Mockito.when(sshSession.getScpClient()).thenReturn(scpClient);
        Mockito.when(sshSession.getSftpClient()).thenReturn(sftpClient);
        Mockito.when(sshSession.getKey()).thenReturn(key);
        SecurePassword password = new SecurePassword();
        password.setEncodedPassword("drowssap");
        Mockito.when(securePasswordManager.findByPrimaryKey(Matchers.eq(passwordGoid))).thenReturn(password);
        Mockito.when(securePasswordManager.decryptPassword(Matchers.eq(password.getEncodedPassword()))).thenReturn("password".toCharArray());

        Mockito.when(sshSessionPool.borrowObject(Matchers.eq(key))).thenReturn(sshSession);
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        return makeContext(req, res, null);
    }

    private PolicyEnforcementContext makeContext(String req, String res, SshKnob sshKnob) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        if (sshKnob != null) request.attachKnob(SshKnob.class, sshKnob);
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private SshRouteAssertion createSshRouteAssertion() {
        SshRouteAssertion assertion = new SshRouteAssertion();
        assertion.setFileName(fileName);
        assertion.setDirectory(fileDir);
        assertion.setCommandType(commandType);
        assertion.setConnectTimeout(10);
        assertion.setFileLength(String.valueOf(fileLength));
        assertion.setHost("myhost");
        assertion.setUsername("myUser");
        assertion.setPasswordGoid(new Goid(0,123L));
        assertion.setCredentialsSourceSpecified(true);
        assertion.setRequestTarget(new MessageTargetableSupport(TargetMessageType.REQUEST, false));
        assertion.setResponseTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, false));
        assertion.setScpProtocol(true);
        assertion.setDownloadContentType(ContentTypeHeader.TEXT_DEFAULT.toString());
        assertion.setResponseByteLimit("1000000");
        return assertion;
    }

    @Test
    public void testScpGet() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, InterruptedException, NoSuchPartException {
        final String returnedResponse = "My response string";
        final CountDownLatch streamWritten = new CountDownLatch(1);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                IOUtils.copyStream(new ByteArrayInputStream(returnedResponse.getBytes()), (OutputStream) invocationOnMock.getArguments()[0]);
                ((FileTransferProgressMonitor) invocationOnMock.getArguments()[5]).start(FileTransferProgressMonitor.DOWNLOAD, new XmlSshFile(fileDir + fileName, true, returnedResponse.length()));
                streamWritten.countDown();
                return null;
            }
        }).when(scpClient).download(Matchers.any(OutputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(scpClient).download(Matchers.any(OutputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.any(FileTransferProgressMonitor.class));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(peCtx.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), bout);

        Assert.assertTrue("The stream was not copied in time.", streamWritten.await(5, TimeUnit.SECONDS));

        Assert.assertEquals(returnedResponse, bout.toString());
    }

    @Test
    public void testScpGetSaveFileSize() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, InterruptedException, NoSuchPartException, NoSuchVariableException {
        final String returnedResponse = "My response string";
        final CountDownLatch streamWritten = new CountDownLatch(1);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                IOUtils.copyStream(new ByteArrayInputStream(returnedResponse.getBytes()), (OutputStream) invocationOnMock.getArguments()[0]);
                ((FileTransferProgressMonitor) invocationOnMock.getArguments()[5]).start(FileTransferProgressMonitor.DOWNLOAD, new XmlSshFile(fileDir + fileName, true, returnedResponse.length()));
                streamWritten.countDown();
                return null;
            }
        }).when(scpClient).download(Matchers.any(OutputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.any(FileTransferProgressMonitor.class));

        assertion.setSetFileSizeToContextVariable(true);
        final String saveFileSizeContextVariable = "file.size.context.variable";
        assertion.setSaveFileSizeContextVariable(saveFileSizeContextVariable);
        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(scpClient).download(Matchers.any(OutputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.any(FileTransferProgressMonitor.class));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(peCtx.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), bout);

        Assert.assertTrue("The stream was not copied in time.", streamWritten.await(5, TimeUnit.SECONDS));

        Assert.assertEquals(returnedResponse, bout.toString());
        Assert.assertEquals((long) returnedResponse.length(), peCtx.getVariable(saveFileSizeContextVariable));
    }

    @Test
    public void testScpPut() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException {
        assertion.setCommandType(CommandKnob.CommandType.PUT);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream((InputStream) invocationOnMock.getArguments()[0], bout);
                Assert.assertEquals(requestBody, bout.toString());
                return null;
            }
        }).when(scpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(scpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.argThat(new BaseMatcher<XmlSshFile>() {
            @Override
            public boolean matches(Object o) {
                if (o == null) {
                    return true;
                } else if (o instanceof XmlSshFile && ((XmlSshFile) o).getPermissions() == null)
                    return true;
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expecting null XmlSshFile or XmlSsh file with null permissions.");
            }
        }), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testSchPutPerversePermissions() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException {
        assertion.setCommandType(CommandKnob.CommandType.PUT);
        assertion.setPreserveFileMetadata(true);
        long accessTime = 123L;
        long modificationTime = 456L;
        final int permissions = 742;
        peCtx = makeContext(requestBody, responseBody, MessageProcessingSshUtil.buildSshKnob("localhost", 123, host, port, fileName, fileDir, commandType,
                null, null, new SshKnob.FileMetadata(accessTime, modificationTime, permissions), Collections.<String, String>emptyMap()));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream((InputStream) invocationOnMock.getArguments()[0], bout);
                Assert.assertEquals(requestBody, bout.toString());
                return null;
            }
        }).when(scpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(scpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.argThat(new BaseMatcher<XmlSshFile>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof XmlSshFile && ((XmlSshFile) o).getPermissions() == permissions;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expecting permissions to be: " + permissions);
            }
        }), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testScpPutNegativeLength() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException {
        assertion.setCommandType(CommandKnob.CommandType.PUT);
        assertion.setFileLength("-1");

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream((InputStream) invocationOnMock.getArguments()[0], bout);
                Assert.assertEquals(requestBody, bout.toString());
                return null;
            }
        }).when(scpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(scpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq((long) requestBody.length()), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.argThat(new BaseMatcher<XmlSshFile>() {
            @Override
            public boolean matches(Object o) {
                if (o == null) {
                    return true;
                } else if (o instanceof XmlSshFile && ((XmlSshFile) o).getPermissions() == null)
                    return true;
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expecting null XmlSshFile or XmlSsh file with null permissions.");
            }
        }), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testSftpGet() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, NoSuchPartException, InterruptedException {
        final String returnedResponse = "My response string";
        final CountDownLatch streamWritten = new CountDownLatch(1);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                IOUtils.copyStream(new ByteArrayInputStream(returnedResponse.getBytes()), (OutputStream) invocationOnMock.getArguments()[0]);
                ((FileTransferProgressMonitor) invocationOnMock.getArguments()[5]).start(FileTransferProgressMonitor.DOWNLOAD, new XmlSshFile(fileDir + fileName, true, returnedResponse.length()));
                streamWritten.countDown();
                return null;
            }
        }).when(sftpClient).download(Matchers.any(OutputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.any(FileTransferProgressMonitor.class));

        assertion.setScpProtocol(false);
        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).download(Matchers.any(OutputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.any(FileTransferProgressMonitor.class));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(peCtx.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), bout);

        Assert.assertTrue("The stream was not copied in time.", streamWritten.await(5, TimeUnit.SECONDS));

        Assert.assertEquals(returnedResponse, bout.toString());
    }

    @Test
    public void testSftpPut() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException {
        assertion.setCommandType(CommandKnob.CommandType.PUT);
        assertion.setScpProtocol(false);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream((InputStream) invocationOnMock.getArguments()[0], bout);
                Assert.assertEquals(requestBody, bout.toString());
                return null;
            }
        }).when(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.argThat(new BaseMatcher<XmlSshFile>() {
            @Override
            public boolean matches(Object o) {
                if (o == null) {
                    return true;
                } else if (o instanceof XmlSshFile && ((XmlSshFile) o).getPermissions() == null)
                    return true;
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expecting null XmlSshFile or XmlSsh file with null permissions.");
            }
        }), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testSftpPutPerversePermissions() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException {
        assertion.setCommandType(CommandKnob.CommandType.PUT);
        assertion.setPreserveFileMetadata(true);
        assertion.setScpProtocol(false);
        long accessTime = 123L;
        long modificationTime = 456L;
        final int permissions = 742;
        peCtx = makeContext(requestBody, responseBody, MessageProcessingSshUtil.buildSshKnob("localhost", 123, host, port, fileName, fileDir, commandType,
                null, null, new SshKnob.FileMetadata(accessTime, modificationTime, permissions), Collections.<String, String>emptyMap()));

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream((InputStream) invocationOnMock.getArguments()[0], bout);
                Assert.assertEquals(requestBody, bout.toString());
                return null;
            }
        }).when(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.argThat(new BaseMatcher<XmlSshFile>() {
            @Override
            public boolean matches(Object o) {
                return o instanceof XmlSshFile && ((XmlSshFile) o).getPermissions() == permissions;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expecting permissions to be: " + permissions);
            }
        }), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testSftpPutNegativeLength() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException {
        assertion.setCommandType(CommandKnob.CommandType.PUT);
        assertion.setFileLength("-1");
        assertion.setScpProtocol(false);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                IOUtils.copyStream((InputStream) invocationOnMock.getArguments()[0], bout);
                Assert.assertEquals(requestBody, bout.toString());
                return null;
            }
        }).when(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(-1L), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.argThat(new BaseMatcher<XmlSshFile>() {
            @Override
            public boolean matches(Object o) {
                if (o == null) {
                    return true;
                } else if (o instanceof XmlSshFile && ((XmlSshFile) o).getPermissions() == null)
                    return true;
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("Expecting null XmlSshFile or XmlSsh file with null permissions.");
            }
        }), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testSftpList() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.LIST);

        final XmlVirtualFileList xmlVirtualFileList = new XmlVirtualFileList(Arrays.asList(new XmlSshFile("File1", true, 222, 333), new XmlSshFile("File2", true, 555, 333, 444), new XmlSshFile("Dir1", false), new XmlSshFile("File3", true)));
        Mockito.when(sftpClient.listDirectory(Matchers.eq(fileDir), Matchers.eq(fileName))).thenReturn(xmlVirtualFileList);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).listDirectory(Matchers.eq(fileDir), Matchers.eq(fileName));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(peCtx.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), bout);

        StringWriter writer = new StringWriter();
        JAXB.marshal(xmlVirtualFileList, writer);

        Assert.assertEquals(writer.toString(), bout.toString());
    }

    @Test
    public void testSftpStat() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.STAT);

        final XmlSshFile xmlSshFile = new XmlSshFile("File1", true, 222, 333, 666);
        Mockito.when(sftpClient.getFileAttributes(Matchers.eq(fileDir), Matchers.eq(fileName))).thenReturn(xmlSshFile);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).getFileAttributes(Matchers.eq(fileDir), Matchers.eq(fileName));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(peCtx.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), bout);

        StringWriter writer = new StringWriter();
        JAXB.marshal(new XmlVirtualFileList(Arrays.asList(xmlSshFile)), writer);

        Assert.assertEquals(writer.toString(), bout.toString());
    }

    @Test
    public void testSftpStatSaveSize() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException, NoSuchVariableException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.STAT);
        assertion.setSetFileSizeToContextVariable(true);
        final String saveFileSizeContextVariable = "file.size.context.variable";
        assertion.setSaveFileSizeContextVariable(saveFileSizeContextVariable);

        final long size = 222L;
        final XmlSshFile xmlSshFile = new XmlSshFile("File1", true, size, 333, 666);
        Mockito.when(sftpClient.getFileAttributes(Matchers.eq(fileDir), Matchers.eq(fileName))).thenReturn(xmlSshFile);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).getFileAttributes(Matchers.eq(fileDir), Matchers.eq(fileName));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copyStream(peCtx.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(), bout);

        StringWriter writer = new StringWriter();
        JAXB.marshal(new XmlVirtualFileList(Arrays.asList(xmlSshFile)), writer);

        Assert.assertEquals(writer.toString(), bout.toString());

        Assert.assertEquals(size, peCtx.getVariable(saveFileSizeContextVariable));
    }

    @Test
    public void testSftpDelete() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.DELETE);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).deleteFile(Matchers.eq(fileDir), Matchers.eq(fileName));
    }

    @Test
    public void testSftpMove() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.MOVE);
        final String newFileName = "newFileName.txt";
        assertion.setNewFileName(newFileName);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).renameFile(Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(newFileName));
    }

    @Test
    public void testSftpMkdir() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.MKDIR);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).createDirectory(Matchers.eq(fileDir), Matchers.eq(fileName));
    }

    @Test
    public void testSftpRmdir() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandType(CommandKnob.CommandType.RMDIR);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).removeDirectory(Matchers.eq(fileDir), Matchers.eq(fileName));
    }

    @Test
    public void testCommandFromContextVariable() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandTypeVariableName("command.type.variable");
        assertion.setRetrieveCommandTypeFromVariable(true);
        peCtx.setVariable("command.type.variable", "DELETE");

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).deleteFile(Matchers.eq(fileDir), Matchers.eq(fileName));

        peCtx.setVariable("command.type.variable", "LIST");
        final XmlVirtualFileList xmlVirtualFileList = new XmlVirtualFileList(Arrays.asList(new XmlSshFile("File1", true, 222, 333), new XmlSshFile("File2", true, 555, 333, 444), new XmlSshFile("Dir1", false), new XmlSshFile("File3", true)));
        Mockito.when(sftpClient.listDirectory(Matchers.eq(fileDir), Matchers.eq(fileName))).thenReturn(xmlVirtualFileList);
        status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).listDirectory(Matchers.eq(fileDir), Matchers.eq(fileName));

        peCtx.setVariable("command.type.variable", "PUT");

        status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals("Assertion Failed: Messages:\n" + getAuditMessages(), AssertionStatus.NONE, status);
        Mockito.verify(sftpClient).upload(Matchers.any(InputStream.class), Matchers.eq(fileDir), Matchers.eq(fileName), Matchers.eq(fileLength), Matchers.eq(fileOffset), Matchers.eq(false), Matchers.any(XmlSshFile.class), Matchers.any(FileTransferProgressMonitor.class));
    }

    @Test
    public void testBadCommandFromContextVariable() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandTypeVariableName("command.type.variable");
        assertion.setRetrieveCommandTypeFromVariable(true);
        peCtx.setVariable("command.type.variable", "BADCOMMAND");

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.BAD_REQUEST, status);

        Assert.assertTrue("Did not find expected audit message.", testAudit.isAuditPresentContaining("Invalid command type given: BADCOMMAND"));
    }

    @Test
    public void testBadContextVariable() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(false);
        assertion.setCommandTypeVariableName("command.type.badvariable");
        assertion.setRetrieveCommandTypeFromVariable(true);
        peCtx.setVariable("command.type.variable", "BADCOMMAND");

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.BAD_REQUEST, status);

        Assert.assertTrue("Did not find expected audit message.", testAudit.isAuditPresentContaining("Command type variable not found: command.type.badvariable"));
    }

    @Test
    public void testScpListFail() throws IOException, PolicyAssertionException, LicenseException, JSchException, FileTransferException, SftpException, NoSuchPartException {
        assertion.setScpProtocol(true);
        assertion.setCommandType(CommandKnob.CommandType.LIST);

        AssertionStatus status = serverSshRouteAssertion.checkRequest(peCtx);

        Assert.assertEquals(AssertionStatus.BAD_REQUEST, status);

        Assert.assertTrue("Did not find expected audit message.", testAudit.isAuditPresentContaining("Unsupported SCP command type: LIST"));
    }

    private String getAuditMessages() {
        if (!testAudit.iterator().hasNext()) {
            return "none";
        }
        StringWriter audits = new StringWriter();
        for (String message : testAudit) {
            audits.append(message).append('\n');
        }
        return audits.toString();
    }
}
