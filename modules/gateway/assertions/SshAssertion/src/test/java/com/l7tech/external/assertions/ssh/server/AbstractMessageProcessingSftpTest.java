package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.net.SocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static com.l7tech.external.assertions.ssh.server.SshServerModule.*;

/**
 * This was created: 1/22/13 as 11:04 AM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
public abstract class AbstractMessageProcessingSftpTest {

    @Mock
    ServerSession session;
    @Mock
    IoSession ioSession;

    @Mock
    StashManagerFactory stashManagerFactory;
    @Mock
    StashManager stashManager;
    @Mock
    MessageProcessor messageProcessor;
    @Mock
    SoapFaultManager soapFaultManager;
    @Mock
    EventChannel messageProcessingEventChannel;

    SsgConnector connector;

    @InjectMocks
    MessageProcessingSftpSubsystem sftpSubsystem;

    @Mock
    ThreadPoolBean threadPoolMocked;

    protected ApplicationContext appCtx;
    protected Config config;

    ByteArrayOutputStream out;

    @Before
    public void setUp() throws Exception {
        appCtx = ApplicationContexts.getTestApplicationContext();
        config = appCtx.getBean("serverConfig", Config.class);

        connector = new SsgConnector();
        sftpSubsystem = new MessageProcessingSftpSubsystem(connector);
        out = new ByteArrayOutputStream();
        sftpSubsystem.setOutputStream(out);
        sftpSubsystem.setFileSystemView(new VirtualFileSystemView(false));
        MockitoAnnotations.initMocks(this);
        setupMocks();
    }

    @After
    public void tearDown() throws Exception {
        sftpSubsystem.destroy();
    }

    private void setupMocks() throws ThreadPool.ThreadPoolShutDownException {
        Mockito.when(ioSession.getRemoteAddress()).thenReturn(new SocketAddress() {
            @Override
            public String toString() {
                return "10.7.48.67:8022";
            }
        });
        Mockito.when(ioSession.getLocalAddress()).thenReturn(new SocketAddress() {
            @Override
            public String toString() {
                return "127.0.0.1:57864";
            }
        });
        Mockito.when(session.getIoSession()).thenReturn(ioSession);

        Mockito.when(stashManagerFactory.createStashManager()).thenReturn(stashManager);

        Mockito.when(session.getAttribute(MINA_SESSION_ATTR_CRED_USERNAME)).thenReturn(new Option<String>(null));
        Mockito.when(session.getAttribute(MINA_SESSION_ATTR_CRED_PUBLIC_KEY)).thenReturn(new Option<String>(null));
        Mockito.when(session.getAttribute(MINA_SESSION_ATTR_CRED_PASSWORD)).thenReturn(new Option<String>(null));

        final ThreadPoolBean threadPool = new ThreadPoolBean(config, "Sftp Thread Pool", "sftpListenerThreadLimit",
                "sftp.listenerThreadLimit", 25);
        threadPool.start();
        Mockito.when(threadPoolMocked.submitTask(Matchers.<Callable<Void>>any())).then(new Answer<Future<Void>>() {
            @Override
            public Future<Void> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return threadPool.submitTask((Callable<Void>) invocationOnMock.getArguments()[0]);
            }
        });
    }

    protected static InputStream createInputStream(String contents) throws IOException {
        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(contents.getBytes());
        pout.flush();
        pout.close();
        return pin;
    }

    protected void mockMessageProcessing(final AssertionStatus responseStatus, final InputStream responseInputStream) throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException {
        mockMessageProcessing(new MessageProcessingProperties(responseStatus, responseInputStream, new NullOutputStream()));
    }

    protected void mockMessageProcessing(final AssertionStatus responseStatus, final OutputStream requestOutputStream) throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException {
        mockMessageProcessing(new MessageProcessingProperties(responseStatus, new EmptyInputStream(), requestOutputStream));
    }

    protected void mockMessageProcessing(final MessageProcessingProperties ... messageProcessingProperties) throws IOException, PolicyAssertionException, PolicyVersionException, LicenseException, MethodNotAllowedException, MessageProcessingSuspendedException {
        Mockito.when(messageProcessor.processMessage(Matchers.<PolicyEnforcementContext>any())).then(new Answer<AssertionStatus>() {
            final AtomicInteger invocationCount = new AtomicInteger(0);

            @Override
            public AssertionStatus answer(InvocationOnMock invocationOnMock) throws Throwable {
                PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                //allow out of bounds exceptions here.
                MessageProcessingProperties messageProcessingProperty = messageProcessingProperties[invocationCount.getAndIncrement()];
                messageProcessingProperty.contextValidator.call(context);
                context.getResponse().initialize(stashManager, ContentTypeHeader.XML_DEFAULT, messageProcessingProperty.responseInputStream, 0);
                //should wait for the request input stream to close.
                IOUtils.copyStream(context.getRequest().getMimeKnob().getEntireMessageBodyAsInputStream(true), messageProcessingProperty.requestOutputString);
                messageProcessingProperty.finishedProcessingLatch.countDown();
                return messageProcessingProperty.response;
            }
        });
    }

    protected class MessageProcessingProperties{
        AssertionStatus response;
        InputStream responseInputStream;
        OutputStream requestOutputString;
        Functions.UnaryVoid<PolicyEnforcementContext> contextValidator;
        CountDownLatch finishedProcessingLatch = new CountDownLatch(1);

        public MessageProcessingProperties(AssertionStatus response, InputStream responseInputStream, OutputStream requestOutputString) {
            this(response, responseInputStream, requestOutputString, new Functions.UnaryVoid<PolicyEnforcementContext>() {
                @Override
                public void call(PolicyEnforcementContext policyEnforcementContext) {
                    //do nothing
                }
            });
        }

        public MessageProcessingProperties(AssertionStatus response, InputStream responseInputStream, OutputStream requestOutputString, Functions.UnaryVoid<PolicyEnforcementContext> contextValidator) {
            this.response = response;
            this.responseInputStream = responseInputStream;
            this.requestOutputString = requestOutputString;
            this.contextValidator = contextValidator;
        }
    }

    protected void closeHandle(int id, String rtnHandle) throws IOException {
        int rtnLength;
        byte rtnType;
        int rtnId;
        Buffer buff = new Buffer();
        buff.putInt(0L);
        buff.putByte((byte) SftpSubsystem.SSH_FXP_CLOSE);
        buff.putInt((long) id);
        buff.putString(rtnHandle);

        out.reset();
        sftpSubsystem.process(buff);

        Buffer rtn = new Buffer(out.toByteArray());
        rtnLength = rtn.getInt();
        rtnType = rtn.getByte();
        rtnId = rtn.getInt();
        int substatus = rtn.getInt();

        Assert.assertEquals(SftpSubsystem.SSH_FXP_STATUS, rtnType);
        Assert.assertEquals(id, rtnId);
        Assert.assertEquals(SftpSubsystem.SSH_FX_OK, substatus);
    }

    /**
     * *************************** END TEST SSH_FXP_OPEN ***********************
     */

    private <R extends Reply> R getReply(R reply, Buffer buff, int id) {
        reply.length = buff.getInt();
        reply.type = buff.getByte();
        reply.id = buff.getInt();
        Assert.assertEquals("The returned ID is incorrect", id, reply.id);
        return reply;
    }

    protected StatusReply getStatusReply(int id) {
        Buffer rtn = new Buffer(out.toByteArray());
        StatusReply status = getReply(new StatusReply(), rtn, id);
        Assert.assertEquals("The response was expected to be a status response but was not.", SftpSubsystem.SSH_FXP_STATUS, status.type);
        status.status = rtn.getInt();
        status.message = rtn.getString();
        status.lang = rtn.getString();
        return status;
    }

    protected HandleReply getHandleReply(int id) {
        Buffer rtn = new Buffer(out.toByteArray());
        HandleReply handle = getReply(new HandleReply(), rtn, id);
        Assert.assertEquals("The response was expected to be a handle response but was not.", SftpSubsystem.SSH_FXP_HANDLE, handle.type);
        handle.handle = rtn.getString();
        Assert.assertNotNull(handle.handle);
        Assert.assertFalse(handle.handle.isEmpty());
        return handle;
    }

    protected AttrsReply getAttrsReply(int id) {
        Buffer rtn = new Buffer(out.toByteArray());
        AttrsReply attrs = getReply(new AttrsReply(), rtn, id);
        Assert.assertEquals("The response was expected to be a attrs response but was not.", SftpSubsystem.SSH_FXP_ATTRS, attrs.type);
        attrs.attrs = getAttrs(rtn);
        return attrs;
    }

    protected NameReply getNameReply(int id) {
        Buffer rtn = new Buffer(out.toByteArray());
        NameReply names = getReply(new NameReply(), rtn, id);
        Assert.assertEquals("The response was expected to be a name response but was not.", SftpSubsystem.SSH_FXP_NAME, names.type);
        names.count = rtn.getInt();
        names.names = new Name[names.count];
        for(int i=0;i<names.names.length;i++){
            names.names[i] = getName(rtn);
        }
        return names;
    }

    protected DataReply getDataReply(int id) {
        Buffer rtn = new Buffer(out.toByteArray());
        DataReply data = getReply(new DataReply(), rtn, id);
        Assert.assertEquals("The response was expected to be a attrs response but was not.", SftpSubsystem.SSH_FXP_DATA, data.type);
        data.data = rtn.getBytes();
        return data;
    }

    private Name getName(Buffer buff) {
        Name name = new Name();
        name.filename = buff.getString();
        name.longname = buff.getString();
        name.attrs = getAttrs(buff);
        return name;
    }

    private Attrs getAttrs(Buffer buff) {
        Attrs attrs = new Attrs();
        attrs.flags = buff.getInt();
        if((attrs.flags & SftpSubsystem.SSH_FILEXFER_ATTR_SIZE) == SftpSubsystem.SSH_FILEXFER_ATTR_SIZE){
            attrs.size = buff.getLong();
        }
        if((attrs.flags & SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS) == SftpSubsystem.SSH_FILEXFER_ATTR_PERMISSIONS){
            attrs.permissions = buff.getInt();
        }
        if((attrs.flags & SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME) == SftpSubsystem.SSH_FILEXFER_ATTR_ACMODTIME){
            attrs.atime = buff.getInt();
            attrs.mtime = buff.getInt();
        }
        if((attrs.flags & SftpSubsystem.SSH_FILEXFER_ATTR_EXTENDED) == SftpSubsystem.SSH_FILEXFER_ATTR_EXTENDED){
            attrs.extended_count = buff.getInt();
            Assert.assertEquals("extended count is non zero", 0, attrs.extended_count);
        }
        return attrs;
    }

    private abstract class Reply {
        int length;
        byte type;
        int id;
    }

    protected class StatusReply extends Reply {
        int status;
        String message;
        String lang;
    }

    protected class HandleReply extends Reply {
        String handle;
    }

    protected class AttrsReply extends Reply {
        Attrs attrs;
    }

    protected class NameReply extends Reply {
        int count;
        Name[] names;
    }

    protected class DataReply extends Reply {
        byte[] data;
    }

    protected class Name {
        String filename;
        String longname;
        Attrs attrs;
    }

    protected class Attrs {
        int flags;
        long size;
        int permissions;
        int atime;
        int mtime;
        int extended_count;
    }
}
