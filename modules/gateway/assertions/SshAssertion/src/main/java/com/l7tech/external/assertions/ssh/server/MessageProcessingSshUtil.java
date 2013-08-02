package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.NullOutputStream;
import com.l7tech.common.log.HybridDiagnosticContext;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.*;
import com.l7tech.message.SshKnob.FileMetadata;
import com.l7tech.message.SshKnob.PublicKeyAuthentication;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import org.apache.sshd.server.session.ServerSession;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.common.mime.ContentTypeHeader.XML_DEFAULT;
import static com.l7tech.common.mime.ContentTypeHeader.parseValue;
import static com.l7tech.external.assertions.ssh.server.SshServerModule.*;
import static com.l7tech.gateway.common.transport.SsgConnector.PROP_OVERRIDE_CONTENT_TYPE;
import static com.l7tech.gateway.common.transport.SsgConnector.PROP_REQUEST_SIZE_LIMIT;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.server.message.PolicyEnforcementContextFactory.createPolicyEnforcementContext;
import static com.l7tech.util.TextUtils.isNotEmpty;

/**
 * Utility methods for SSH support.
 */
public class MessageProcessingSshUtil {
    private static final Logger logger = Logger.getLogger(MessageProcessingSshUtil.class.getName());

    public static final int DEFAULT_MAX_WRITE_BUFFER_SIZE = 1024 * 8;

    /**
     * Read any remaining input after policy evaluation to ensure input can close without error.
     *
     * @param inputStream The input to read
     * @param logger      Log any IOException
     */
    public static void prepareInputStreamForClosing(InputStream inputStream, Logger logger) {
        try {
            IOUtils.copyStream(inputStream, new NullOutputStream());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Exception while preparing to close input stream.  " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * Build a policy execution context for processing a message with an SSH knob
     *
     * @param connector           The connector in use
     * @param session             The current SSH session
     * @param stashManagerFactory The stash manager to use for messages
     * @param messageInputStream  The stream for reading message content
     * @param file                The file being transfered
     * @param path                The path for the file transfer
     * @param fileMetadata        The file metadata.
     * @return The PolicyEnforcementContext
     * @throws IOException If the content type for the connector is invalid or there is an error reading the message
     */
    public static PolicyEnforcementContext buildPolicyExecutionContext(final SsgConnector connector,
                                                                       final ServerSession session,
                                                                       final StashManagerFactory stashManagerFactory,
                                                                       final InputStream messageInputStream,
                                                                       final String file,
                                                                       final String path,
                                                                       final CommandKnob.CommandType commandType,
                                                                       final FileMetadata fileMetadata, final Map<String, String> parameters) throws IOException {
        final Message request = new Message();
        final PolicyEnforcementContext context = createPolicyEnforcementContext(request, null);
        final long requestSizeLimit = connector.getLongProperty(PROP_REQUEST_SIZE_LIMIT, getMaxBytes());
        final String ctypeStr = connector.getProperty(PROP_OVERRIDE_CONTENT_TYPE);
        final ContentTypeHeader ctype = ctypeStr == null ? XML_DEFAULT : parseValue(ctypeStr);

        request.initialize(
                stashManagerFactory.createStashManager(),
                ctype,
                messageInputStream,
                requestSizeLimit);

        // attach ssh knob
        final PublicKeyAuthentication publicKeyAuthentication;
        final PasswordAuthentication passwordAuthentication;
        final Option<String> userName = session.getAttribute(MINA_SESSION_ATTR_CRED_USERNAME);
        final Option<String> userPublicKey = session.getAttribute(MINA_SESSION_ATTR_CRED_PUBLIC_KEY);
        final Option<String> userPassword = session.getAttribute(MINA_SESSION_ATTR_CRED_PASSWORD);
        if (userName.exists(isNotEmpty()) && userPublicKey.isSome()) {
            publicKeyAuthentication = new PublicKeyAuthentication(userName.some(), userPublicKey.some());
            passwordAuthentication = null;
        } else if (userName.exists(isNotEmpty()) && userPassword.exists(isNotEmpty())) {
            publicKeyAuthentication = null;
            passwordAuthentication = new PasswordAuthentication(userName.some(), userPassword.some().toCharArray());
        } else {
            publicKeyAuthentication = null;
            passwordAuthentication = null;
        }

        final SshKnob knob = buildSshKnob(
                session.getIoSession().getLocalAddress(),
                session.getIoSession().getRemoteAddress(),
                file,
                path,
                commandType,
                publicKeyAuthentication,
                passwordAuthentication, fileMetadata, parameters);
        request.attachKnob(knob, SshKnob.class, UriKnob.class, TcpKnob.class, CommandKnob.class);

        final Goid hardwiredServiceGoid = connector.getGoidProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, GoidEntity.DEFAULT_GOID);
        if (!Goid.isDefault(hardwiredServiceGoid)) {
            request.attachKnob(HasServiceGoid.class, new HasServiceGoidImpl(hardwiredServiceGoid));
        }

        return context;
    }

    /*
     * Create an SshKnob for SCP and SFTP.
     */
    static SshKnob buildSshKnob(final SocketAddress localSocketAddress,
                                final SocketAddress remoteSocketAddress,
                                final String file,
                                final String path,
                                final CommandKnob.CommandType commandType,
                                final PublicKeyAuthentication publicKeyCredential,
                                final PasswordAuthentication passwordCredential,
                                @Nullable final FileMetadata fileMetadata, final Map<String, String> parameters) {

        // SocketAddress requires us to parse for host and port (e.g. /127.0.0.1:22)
        final Pair<String, String> localHostPortPair = getHostAndPort(localSocketAddress.toString());
        final Pair<String, String> remoteHostPortPair = getHostAndPort(remoteSocketAddress.toString());

        final String localHostFinal = localHostPortPair.getKey();
        final int localPortFinal = Integer.parseInt(localHostPortPair.getValue());
        final String remoteHostFinal = remoteHostPortPair.getKey();
        final int remotePortFinal = Integer.parseInt(remoteHostPortPair.getValue());

        return buildSshKnob(localHostFinal, localPortFinal, remoteHostFinal, remotePortFinal, file, path, commandType,
                publicKeyCredential, passwordCredential, fileMetadata, parameters);
    }

    /*
     * Create an SshKnob for SCP and SFTP.
     */
    public static SshKnob buildSshKnob(@Nullable final String localHost,
                                       final int localPort,
                                       @Nullable final String remoteHost,
                                       final int remotePort,
                                       final String file,
                                       final String path,
                                       final CommandKnob.CommandType commandType,
                                       @Nullable final PublicKeyAuthentication publicKeyCredential,
                                       @Nullable final PasswordAuthentication passwordCredential,
                                       @Nullable final FileMetadata fileMetadata,
                                       final Map<String, String> parameters) {

        return new SshKnob() {
            @Override
            public String getLocalAddress() {
                return localHost;
            }

            @Override
            public String getLocalHost() {
                return localHost;
            }

            @Override
            public int getLocalPort() {
                return getLocalListenerPort();
            }

            @Override
            public int getLocalListenerPort() {
                return localPort;
            }

            @Override
            public String getRemoteAddress() {
                return remoteHost;
            }

            @Override
            public String getRemoteHost() {
                return remoteHost;
            }

            @Override
            public int getRemotePort() {
                return remotePort;
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
            public PasswordAuthentication getPasswordAuthentication() {
                return passwordCredential;
            }

            @Override
            public PublicKeyAuthentication getPublicKeyAuthentication() {
                return publicKeyCredential;
            }

            @Override
            public FileMetadata getFileMetadata() {
                return fileMetadata;
            }

            @Override
            public CommandType getCommandType() {
                return commandType;
            }

            @Override
            public String getParameter(String name) {
                return parameters.get(name);
            }
        };
    }

    /**
     * Get the remote IP address for the given session (the client IP)
     *
     * @param session The server session (required)
     * @return The address or an empty string if not available (never null)
     */
    static String getRemoteAddress(final ServerSession session) {
        String address = "";
        final SocketAddress socketAddress = session.getIoSession().getRemoteAddress();

        if (socketAddress instanceof InetSocketAddress) {
            final InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            if (!inetSocketAddress.isUnresolved()) {
                address = inetSocketAddress.getAddress().getHostAddress();
            }
        }

        return address;
    }

    /**
     * Parses the host and port from a "host[:port]" string.
     * <p/>
     * Similar to InetAddressUtil.getHostAndPort(String hostAndPossiblyPort, String defaultPort),
     * but does not return unwanted square bracket around the host name (e.g. [hostname]) like InetAddressUtil
     *
     * @param hostAndPossiblyPort string containing a host and optionally a port (delimited from the host part with ":")
     * @return the host and port determined as described above
     */
    private static Pair<String, String> getHostAndPort(String hostAndPossiblyPort) {
        boolean startsWithForwardSlash = (int) hostAndPossiblyPort.charAt(0) == (int) '/';
        int colonIndex = hostAndPossiblyPort.indexOf(':');
        String host;
        if (startsWithForwardSlash && colonIndex > 1) {
            host = hostAndPossiblyPort.substring(1, colonIndex);
        } else if (startsWithForwardSlash) {
            host = hostAndPossiblyPort.substring(1);
        } else if (colonIndex > -1) {
            host = hostAndPossiblyPort.substring(0, colonIndex);
        } else {
            host = hostAndPossiblyPort;
        }
        String port = null;
        if (colonIndex > -1) {
            port = hostAndPossiblyPort.substring(colonIndex + 1);
        }

        return new Pair<String, String>(host, port);
    }

    /**
     * Start Gateway Policy Process task.
     * The task will run in a separate thread and finish when there nothing left in the InputStream (e.g. when it has been closed) and the file handle is closed.
     * This is used to process both scp and sftp messages.
     * <p/>
     * The output string of the virtual ssh file will be set so that writing to it will write to the input stream of the message processor.
     * Message processing stages can be followed using the VirtualSshFile.getMessageProcessingStatus
     *
     * @param connector The SSG connector associated with this transport
     * @param file The virtual ssh file to submit for processing
     * @param commandType The command type
     * @param parameters The parameters associated with this command
     * @param session The server session for this connection
     * @param stashManagerFactory The stashManagerFactory to give to the policy enforcement context creator.
     * @param threadPool The thread pool to run policy processing in.
     * @param messageProcessor The message processor to use to process the message
     * @param soapFaultManager The fault manager to use to register soap faults
     * @param messageProcessingEventChannel The message processing even channel.
     * @throws IOException
     * @throws ThreadPool.ThreadPoolShutDownException
     *
     */
    public static void submitMessageProcessingTask(final SsgConnector connector, final VirtualSshFile file, final CommandKnob.CommandType commandType,
                                                   final Map<String, String> parameters, ServerSession session, StashManagerFactory stashManagerFactory, ThreadPoolBean threadPool, final MessageProcessor messageProcessor,
                                                   final SoapFaultManager soapFaultManager, final EventChannel messageProcessingEventChannel) throws IOException, ThreadPool.ThreadPoolShutDownException {
        //Validate that message processing has not already been started. And start it if it has not.
        if (!file.getMessageProcessingStatus().setProcessingStarted()) {
            throw new IllegalStateException("Message processing has already been started.");
        }

        try {
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, connector.getGoid().toString());
            HybridDiagnosticContext.put(GatewayDiagnosticContextKeys.CLIENT_IP, MessageProcessingSshUtil.getRemoteAddress(session));

            //create the output stream for the virtual ssh file. Writing to the output stream of the virtual ssh file will pipe the data to the input stream of the request message
            final PipedInputStream requestInputStream = new PipedInputStream(connector.getIntProperty(SshServerModule.LISTEN_PROP_SFTP_MAX_WRITE_BUFFER_SIZE, DEFAULT_MAX_WRITE_BUFFER_SIZE));
            final PipedOutputStream requestOutputStream = new PipedOutputStream(requestInputStream);
            file.setOutputStream(requestOutputStream);

            //Get the file name and the file path.
            final String fileName = file.getName();
            final String path = file.getPath();

            //create the policy enforcement context
            SshKnob.FileMetadata metadata = new SshKnob.FileMetadata(file.getAccessTime(), file.getLastModified(), file.getPermission());
            final PolicyEnforcementContext context =
                    buildPolicyExecutionContext(connector, session, stashManagerFactory, requestInputStream, fileName, path, commandType, metadata, parameters);

            //Create the countdown latches. These are used to monitor and wait for states during message processing.
            final CountDownLatch startedSignal = new CountDownLatch(1);
            final CountDownLatch finishedSignal = new CountDownLatch(1);
            file.getMessageProcessingStatus().setMessageProcessingFinishedLatch(finishedSignal);
            //The message processing task.
            threadPool.submitTask(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    AssertionStatus status = AssertionStatus.UNDEFINED;
                    String faultXml = null;
                    try {
                        try {
                            // signal message processing started.
                            startedSignal.countDown();
                            // process the message
                            // Note that message processing will not return until the request input stream is closed.
                            status = messageProcessor.processMessage(context);

                            //sets the message processing status and the response output stream
                            file.getMessageProcessingStatus().setMessageProcessStatus(status);

                            //Set the input stream getter. Do it this way because calling getEntireMessageBodyAsInputStream(true) will destroy the input stream but in
                            // some cases we will need to read and stash it first to get the length.
                            file.setInputStreamGetter(new Functions.NullaryThrows<InputStream, IOException>() {

                                @Override
                                public InputStream call() throws IOException {
                                    try {
                                        return context.getResponse().getMimeKnob().getEntireMessageBodyAsInputStream(true);
                                    } catch (NoSuchPartException e) {
                                        //to make exception handling easier wrap this with an IOException
                                        throw new IOException(e);
                                    }
                                }
                            });

                            // Sets the content length getter. Using a getter allows for the input stream to not be stashed in many cases where the content length is never used.
                            file.getMessageProcessingStatus().setResultContentLengthGetter(new Functions.Nullary<Long>() {

                                @Override
                                public Long call() {
                                    try {
                                        if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SCP_RETRIEVE_FILE_SIZE_FROM_VARIABLE)) {
                                            try {
                                                return Long.parseLong(context.getVariable(connector.getProperty(SshCredentialAssertion.LISTEN_PROP_SIZE_CONTEXT_VARIABLE_NAME)).toString());
                                            } catch(Throwable e){
                                                logger.log(Level.WARNING, "Error parsing file size from context variable. " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                                return context.getResponse().getMimeKnob().getContentLength();
                                            }
                                        } else {
                                            return context.getResponse().getMimeKnob().getContentLength();
                                        }
                                    } catch (Throwable e) {
                                        return -1L;
                                    }
                                }
                            });

                            //signal message processing finished. This needs to be signalled after the processing status, the file input stream and the content length have be set!
                            finishedSignal.countDown();

                            if (logger.isLoggable(Level.FINER)) {
                                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
                            }
                        } catch (PolicyVersionException pve) {
                            logger.log(Level.INFO, "Request referred to an outdated version of policy");
                            faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
                        } catch (Throwable t) {
                            logger.log(Level.WARNING, "Exception while processing SFTP message. " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                            faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                        }

                        if (status != AssertionStatus.NONE) {
                            faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                        } else {
                            //Wait for the file handle to be closed. If the context is closed before message processing is stopped then the response input stream will no longer be able to be read.
                            //Do not timeout around here. If uploading a large file the file handle may be open for a long time, especially on a slow connection.
                            file.waitForHandleClosed();
                            prepareInputStreamForClosing(requestInputStream, logger);
                        }
                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } finally {
                        startedSignal.countDown();
                        finishedSignal.countDown();
                        ResourceUtils.closeQuietly(context);
                        ResourceUtils.closeQuietly(requestInputStream);
                    }
                    return null;
                }
            });

            //Wait for message processing to start.
            try {
                startedSignal.await();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new CausedIOException("Interrupted waiting for data.", ie);
            }
        } finally {
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.LISTEN_PORT_ID);
            HybridDiagnosticContext.remove(GatewayDiagnosticContextKeys.CLIENT_IP);
        }
    }

    /**
     * This class is used to keep track of the message processing status and the result.
     */
    public static class MessageProcessingStatus {

        private final AtomicReference<AssertionStatus> messageProcessStatus = new AtomicReference<AssertionStatus>();

        /**
         * This latch keeps track of when message processing is finished.
         */
        private CountDownLatch messageProcessingFinishedLatch;

        private AtomicBoolean processingStarted = new AtomicBoolean(false);
        private Functions.Nullary<Long> resultContentLength;

        /**
         * This returns the status of the message processing
         *
         * @return The message processing future result
         */
        public AssertionStatus getMessageProcessStatus() {
            return messageProcessStatus.get();
        }

        /**
         * Sets the message processing status. If it is already set an IllegalStateException is thrown
         * @param messageProcessStatus The message processing status
         */
        private void setMessageProcessStatus(AssertionStatus messageProcessStatus) {
            if (!this.messageProcessStatus.compareAndSet(null, messageProcessStatus)) {
                throw new IllegalStateException("The message processing status for this virtual ssh file has already been set.");
            }
        }

        /**
         * Set the message processing latch to alert when message processing has finished. This will throw an @IllegalStateException if the latch has already been set.
         *
         * @param messageProcessingFinishedLatch The message processing finished latch
         */
        private void setMessageProcessingFinishedLatch(CountDownLatch messageProcessingFinishedLatch) {
            if (this.messageProcessingFinishedLatch != null) {
                throw new IllegalStateException("The message processing latch has already been set.");
            }
            this.messageProcessingFinishedLatch = messageProcessingFinishedLatch;
        }

        /**
         * Waits for processing finished for the given amount of time.
         *
         * @param timeout The max time to wait for
         * @param unit The unit of the max time
         * @return Returns true if message processing successfully finished. False if the time elapsed before message processing finished.
         * @throws InterruptedException
         */
        public boolean waitForMessageProcessingFinished(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException {
            if (messageProcessingFinishedLatch == null) {
                throw new IllegalStateException("The message processing finished latch is null. This is most likely because message processing has not started yet.");
            }
            return messageProcessingFinishedLatch.await(timeout, unit);
        }

        /**
         * Checks to see if message processing has started.
         *
         * @return true if message processing has started. False otherwise.
         */
        public boolean isProcessingStarted() {
            return processingStarted.get();
        }

        /**
         * Sets processing started to true.
         *
         * @return true if processing processing started was successfully set to true. False if it was already true.
         */
        private boolean setProcessingStarted() {
            return processingStarted.compareAndSet(false, true);
        }

        /**
         * Sets the result content length getter to retrieve the resulting file length.
         * @param resultContentLength The function to get the result content length
         */
        private void setResultContentLengthGetter(Functions.Nullary<Long> resultContentLength) {
            this.resultContentLength = resultContentLength;
        }

        /**
         * Get the resulting file length.
         * @return The file length.
         */
        public long getResultContentLength() {
            return resultContentLength.call();
        }
    }
}
