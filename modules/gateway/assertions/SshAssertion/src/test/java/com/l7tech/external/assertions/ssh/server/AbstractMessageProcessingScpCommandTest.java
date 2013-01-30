package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.mime.ByteArrayStashManager;
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
import org.apache.sshd.server.session.ServerSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
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
 * This was created: 12/20/12 as 3:36 PM
 *
 * @author Victor Kazakov
 */
@Ignore
public class AbstractMessageProcessingScpCommandTest {

    @Mock
    ServerSession session;
    @Mock
    IoSession ioSession;

    @Mock
    StashManagerFactory stashManagerFactory;

    StashManager stashManager;
    @Mock
    MessageProcessor messageProcessor;
    @Mock
    SoapFaultManager soapFaultManager;
    @Mock
    EventChannel messageProcessingEventChannel;

    SsgConnector connector;

    @InjectMocks
    MessageProcessingScpCommand messageProcessingScpCommand;

    @Mock
    ThreadPoolBean threadPoolMocked;

    protected ApplicationContext appCtx;
    protected Config config;

    ByteArrayOutputStream out;

    @Before
    public void setUp() throws Exception {
        appCtx = ApplicationContexts.getTestApplicationContext();
        config = appCtx.getBean("serverConfig", Config.class);

        stashManager = new ByteArrayStashManager();
        connector = new SsgConnector();
        messageProcessingScpCommand = new MessageProcessingScpCommand(new String[]{"scp", "-t"}, connector);
        out = new ByteArrayOutputStream();
        messageProcessingScpCommand.setOutputStream(out);
        messageProcessingScpCommand.setFileSystemView(new VirtualFileSystemView(false));
        MockitoAnnotations.initMocks(this);
        setupMocks();
    }

    @After
    public void tearDown() throws Exception {
        messageProcessingScpCommand.destroy();
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

    protected static InputStream createInputStream(ByteArrayOutputStream contents) throws IOException {
        final PipedInputStream pin = new PipedInputStream();
        PipedOutputStream pout = new PipedOutputStream(pin);
        pout.write(contents.toByteArray());
        pout.flush();
        pout.close();
        return pin;
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
}
