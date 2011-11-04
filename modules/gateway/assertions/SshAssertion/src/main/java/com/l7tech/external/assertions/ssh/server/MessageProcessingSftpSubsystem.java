package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.log.HybridDiagnosticContext;
import static com.l7tech.external.assertions.ssh.server.MessageProcessingSshUtil.buildPolicyExecutionContext;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.sftp.SftpSubsystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing SFTP subsystem (heavily borrowed from org.apache.sshd.server.sftp.SftpSubsystem).
 */
public class MessageProcessingSftpSubsystem extends SftpSubsystem {
    private static final Logger logger = Logger.getLogger(MessageProcessingSftpSubsystem.class.getName());

    private SsgConnector connector;
    private MessageProcessor messageProcessor;
    private EventChannel messageProcessingEventChannel;
    private SoapFaultManager soapFaultManager;
    private StashManagerFactory stashManagerFactory;

    public MessageProcessingSftpSubsystem(SsgConnector c, MessageProcessor mp, StashManagerFactory smf,
                                          SoapFaultManager sfm, EventChannel mpec) {
        connector = c;
        messageProcessor = mp;
        messageProcessingEventChannel = mpec;
        soapFaultManager = sfm;
        stashManagerFactory = smf;
    }

    @Override
    protected void process(Buffer buffer) throws IOException {
        int length = buffer.getInt();
        int type = (int) buffer.getByte();
        int id = buffer.getInt();

        // customize support for selected SFTP functions on the Gateway
        switch (type) {
            case SSH_FXP_INIT: {
                if (length != 5) {
                    throw new IllegalArgumentException();
                }
                version = id;
                if (version >= LOWER_SFTP_IMPL) {
                    version = Math.min(version, HIGHER_SFTP_IMPL);
                    buffer.clear();
                    buffer.putByte((byte) SSH_FXP_VERSION);
                    buffer.putInt((long) version);
                    send(buffer);
                } else {
                    // We only support version 3 (Version 1 and 2 are not common)
                    sendStatus(id, SSH_FX_OP_UNSUPPORTED, "SFTP server only support versions " + ALL_SFTP_IMPL);
                }
                break;
            }
            case SSH_FXP_OPEN: {
                sshFxpOpen(buffer, id);
                break;
            }
            case SSH_FXP_CLOSE: {
                sshFxpClose(buffer, id);
                break;
            }
            case SSH_FXP_WRITE: {
                sshFxpWrite(buffer, id);
                break;
            }
            case SSH_FXP_LSTAT:
            case SSH_FXP_STAT: {
                String path = buffer.getString();
                try {
                    SshFile p = resolveFile(path);
                    sendAttrs(id, p);
                } catch (FileNotFoundException e) {
                    sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } catch (IOException e) {
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                break;
            }
            case SSH_FXP_REALPATH: {
                String path = buffer.getString();
                if (path.trim().length() == 0) {
                    path = ".";
                }
                try {
                    SshFile p = resolveFile(path);
                    sendPath(id, p);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    sendStatus(id, SSH_FX_NO_SUCH_FILE, e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                }
                break;
            }
            default:
                logger.log(Level.WARNING, "Received unsupported type: {" + type + "}");
                sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Command " + type + " is unsupported on the Gateway");
                break;
        }
    }

    protected void sshFxpOpen(Buffer buffer, int id) throws IOException {
        final int maxHandleCount;
        if ( (maxHandleCount = session.getIntProperty( MAX_OPEN_HANDLES_PER_SESSION, 0 )) > 0 ) {
            if ( handles.size() >= maxHandleCount ) {
                sendStatus(id, SSH_FX_FAILURE, "Too many open handles");
                return;
            }
        }

        if (version <= 4) {
            String path = buffer.getString();
            int pflags = buffer.getInt();
            SshFile file = null;
            // attrs
            try {
                file = resolveFile(path);
                if (file.doesExist()) {
                    if (((pflags & SSH_FXF_CREAT) != 0) && ((pflags & SSH_FXF_EXCL) != 0)) {
                        sendStatus(id, SSH_FX_FILE_ALREADY_EXISTS, path);
                        return;
                    }
                } else {
                    if (((pflags & SSH_FXF_CREAT) != 0)) {
                        if (!file.isWritable()) {
                            sendStatus(id, SSH_FX_FAILURE, "Can not create " + path);
                            return;
                        }
                        file.create();
                    }
                }
                if ((pflags & SSH_FXF_TRUNC) != 0) {
                    file.truncate();
                }
                String handle = UUID.randomUUID().toString();

                // start thread to process Gateway request message
                startGatewayMessageProcessThread(connector, file);

                handles.put(handle, new FileHandle(file, pflags)); // handle flags conversion
                sendHandle(id, handle);
            } catch (IOException e) {
                file.handleClose();
                sendStatus(id, SSH_FX_FAILURE, e.getMessage());
            }
        } else {
            String path = buffer.getString();
            int acc = buffer.getInt();
            int flags = buffer.getInt();
            SshFile file = null;
            // attrs
            try {
                file = resolveFile(path);
                switch (flags & SSH_FXF_ACCESS_DISPOSITION) {
                    case SSH_FXF_CREATE_TRUNCATE:
                    case SSH_FXF_CREATE_NEW:
                    case SSH_FXF_OPEN_EXISTING:
                    case SSH_FXF_TRUNCATE_EXISTING:
                    case SSH_FXF_OPEN_OR_CREATE:
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported open mode: " + flags);
                }
                String handle = UUID.randomUUID().toString();

                // start thread to process Gateway request message
                startGatewayMessageProcessThread(connector, file);

                handles.put(handle, new FileHandle(file, flags));
                sendHandle(id, handle);
            } catch (IOException e) {
                file.handleClose();
                sendStatus(id, SSH_FX_FAILURE, e.getMessage());
            }
        }
    }

    protected void sshFxpWrite(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        long offset = buffer.getLong();
        byte[] data = buffer.getBytes();
        SshFile sshFile = null;
        try {
            Handle p = handles.get(handle);
            if (!(p instanceof FileHandle)) {
                sendStatus(id, SSH_FX_INVALID_HANDLE, handle);
            } else {
                sshFile = p.getFile();

                // write data
                pipeDataToGatewayMessageProcessor(sshFile, data, 0);

                sendStatus(id, SSH_FX_OK, "");
            }
        } catch (IOException e) {
            if ( sshFile != null ) sshFile.handleClose();
            sendStatus(id, SSH_FX_FAILURE, ExceptionUtils.getMessage( e ));
        }
    }

    protected void sshFxpClose(Buffer buffer, int id) throws IOException {
        String handle = buffer.getString();
        try {
            Handle h = handles.get(handle);
            if (h == null) {
                sendStatus(id, SSH_FX_INVALID_HANDLE, handle, "");
            } else {
                handles.remove(handle);
                h.close();

                AssertionStatus status = getStatusFromGatewayMessageProcess(h.getFile(),
                        connector.getLongProperty( SshServerModule.LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS, 60L ));
                if (status == null || status == AssertionStatus.UNDEFINED) {
                    sendStatus(id, SSH_FX_FAILURE, "No status returned from Gateway message processing.");
                } else if (status == AssertionStatus.NONE) {
                    sendStatus(id, SSH_FX_OK, "", "");
                } else if (status == AssertionStatus.AUTH_FAILED) {
                    sendStatus(id, SSH_FX_PERMISSION_DENIED, status.toString());
                } else if (status == AssertionStatus.FAILED) {
                    sendStatus(id, SSH_FX_FAILURE, status.toString());
                } else {
                    sendStatus(id, SSH_FX_BAD_MESSAGE, status.toString());
                }
            }
        } catch (IOException e) {
            sendStatus(id, SSH_FX_FAILURE, ExceptionUtils.getMessage( e ));
        } catch (InterruptedException e) {
            sendStatus(id, SSH_FX_FAILURE, ExceptionUtils.getMessage( e ));
        } catch (ExecutionException e) {
            sendStatus(id, SSH_FX_FAILURE, ExceptionUtils.getMessage( e ));
        } catch (TimeoutException e) {
            sendStatus(id, SSH_FX_FAILURE, ExceptionUtils.getMessage( e ));
        }
    }

    /*
     * Start Gateway Message Process thread.  Thread will finish when there nothing left in the InputStream (e.g. when it has been closed).
     */
    private void startGatewayMessageProcessThread( final SsgConnector connector, final SshFile file ) throws IOException {
        if (file instanceof VirtualSshFile) try {
            final VirtualSshFile virtualSshFile = (VirtualSshFile) file;

            HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.LISTEN_PORT_ID, Long.toString( connector.getOid() ) );
            HybridDiagnosticContext.put( GatewayDiagnosticContextKeys.CLIENT_IP, MessageProcessingSshUtil.getRemoteAddress(session) );

            final PipedInputStream pis = new PipedInputStream();
            final PipedOutputStream pos = new PipedOutputStream(pis);
            virtualSshFile.setPipedOutputStream(pos);

            final String fileName = virtualSshFile.getName();
            String path = virtualSshFile.getAbsolutePath();
            int pathLastIndex = path.lastIndexOf('/');
            if (pathLastIndex > 0) {
                path = path.substring(0, pathLastIndex);
            } else if (pathLastIndex == 0) {
                path = "/";
            }

            final PolicyEnforcementContext context =
                    buildPolicyExecutionContext( connector, session, stashManagerFactory, pis, fileName, path );

            final CountDownLatch startedSignal = new CountDownLatch(1);
            //TODO [steve] SFTP file transfer should use a thread pool
            final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "GatewayMessageProcessThread-" + System.currentTimeMillis());
                    thread.setDaemon(true);
                    return thread;
                }
            });

            Future<AssertionStatus> future = executorService.submit(new Callable<AssertionStatus>()
            {
                @Override
                public AssertionStatus call() throws Exception {
                    AssertionStatus status = AssertionStatus.UNDEFINED;
                    String faultXml = null;
                    try {
                        try {
                            startedSignal.countDown();
                            status = messageProcessor.processMessage(context);

                            if (logger.isLoggable(Level.FINER)) {
                                logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);
                            }
                        } catch ( PolicyVersionException pve ) {
                            logger.log( Level.INFO, "Request referred to an outdated version of policy" );
                            faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
                        } catch ( Throwable t ) {
                            logger.log( Level.WARNING, "Exception while processing SFTP message. " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
                            faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                        }

                        if ( status != AssertionStatus.NONE ) {
                            faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                        }
                        if (faultXml != null) {
                            messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                        }
                    } finally {
                        startedSignal.countDown();
                        ResourceUtils.closeQuietly(context);
                        ResourceUtils.closeQuietly(pis);
                    }
                    return status;
                }
            });
            virtualSshFile.setMessageProcessStatus(future);

            try {
                startedSignal.await();
            }
            catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new CausedIOException("Interrupted waiting for data.", ie);
            }
        } finally {
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.LISTEN_PORT_ID );
            HybridDiagnosticContext.remove( GatewayDiagnosticContextKeys.CLIENT_IP );
        }
    }

    /*
     * Pipe data to Message Processor.
     */
    private void pipeDataToGatewayMessageProcessor(final SshFile sshFile, final byte[] data, final int offset) throws IOException {
        if (sshFile instanceof VirtualSshFile) {
            final PipedOutputStream pos = ((VirtualSshFile) sshFile).getPipedOutputStream();
            pos.write(data, offset, data.length);
        }
    }

    /*
     * Get status set by Gateway Message Processing
     */
    private AssertionStatus getStatusFromGatewayMessageProcess(SshFile file, long waitSeconds)
            throws InterruptedException, ExecutionException, TimeoutException {
        AssertionStatus status = null;
        if (file instanceof VirtualSshFile) {
            VirtualSshFile virtualSshFile = (VirtualSshFile) file;
            Future<AssertionStatus> future = virtualSshFile.getMessageProcessStatus();
            if (future != null) {
                status = future.get(waitSeconds, TimeUnit.SECONDS);
            }
        }
        return status;
    }
}
