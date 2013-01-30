package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.CommandKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.command.ScpCommand;
import org.apache.sshd.server.session.ServerSession;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing for SCP support (heavily borrowed from org.apache.sshd.server.command.ScpCommand).
 */
class MessageProcessingScpCommand extends ScpCommand implements SessionAware {
    protected static final Logger logger = Logger.getLogger(MessageProcessingScpCommand.class.getName());

    public static final long DEFAULT_MAX_MESSAGE_PROCESSING_WAIT_TIME = 60L;

    private ServerSession session;
    private final SsgConnector connector;
    @Inject
    private MessageProcessor messageProcessor;
    @Inject
    private EventChannel messageProcessingEventChannel;
    @Inject
    private SoapFaultManager soapFaultManager;
    @Inject
    private StashManagerFactory stashManagerFactory;
    @Inject
    @Named("sftpMessageProcessingThreadPool")
    private ThreadPoolBean threadPool;

    MessageProcessingScpCommand(final String[] args, final SsgConnector connector) {
        super(args);
        this.connector = connector;
    }

    @Override
    public void setSession(ServerSession session) {
        this.session = session;
    }

    @Override
    protected void writeDir(String header, SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Recursively writing dir {0} unsupported", path);
        }
        throw new IOException("Unsupported mode");
    }

    @Override
    protected void writeFile(String header, SshFile path) throws IOException {
        logger.log(Level.FINER, "SCP Writing file {0}", path);

        //Parse the header and extract values.
        if (!header.startsWith("C")) {
            throw new IOException("Expected a C message but got '" + header + "'");
        }
        final long length;
        try {
            length = Long.parseLong(header.substring(6, header.indexOf(' ', 6)));
        } catch (NumberFormatException nfe) {
            throw new CausedIOException("Error parsing header.", nfe);
        }

        //find the correct name to name the file.
        String name = header.substring(header.indexOf(' ', 6) + 1);
        final VirtualSshFile sshFile;
        if (path.isDirectory()) {
            sshFile = (VirtualSshFile) root.getFile(path, name);
        } else if (path.isFile()) {
            sshFile = (VirtualSshFile) path;
        } else {
            //For sanities sake. This should never happen.
            throw new IOException("Can not write to " + path);
        }

        final InputStream fileInput = length <= 0L ?
                new EmptyInputStream() :
                new TruncatingInputStream(in, length);
        ack();
        try {
            submitMessageProcessingTask(sshFile, CommandKnob.CommandType.PUT);
        } catch (MessageProcessingException e) {
            sshFile.handleClose();
            throw new CausedIOException("Message processing failed writing a file via SCP", e);
        }
        IOUtils.copyStream(fileInput, sshFile.getOutputStream());
        sshFile.getOutputStream().close();
        fileInput.close();

        try {
            if(!sshFile.getMessageProcessingStatus().waitForMessageProcessingFinished(connector.getLongProperty(SshServerModule.LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS, DEFAULT_MAX_MESSAGE_PROCESSING_WAIT_TIME), TimeUnit.SECONDS)){
                sshFile.handleClose();
                //this means that message processing failed to finish before the time ran out.
                logger.log(Level.WARNING, "Message processing failed to finish in the allowed amount of time.");
                throw new CausedIOException("Error processing PUT command for: " + sshFile.getAbsolutePath());
            }
        } catch (InterruptedException e) {
            sshFile.handleClose();
            logger.log(Level.WARNING, "Interrupted waiting for message processing to finish: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new CausedIOException("Error processing PUT command for: " + sshFile.getAbsolutePath(), e);
        }
        AssertionStatus status = sshFile.getMessageProcessingStatus().getMessageProcessStatus();
        if (status != AssertionStatus.NONE) {
            sshFile.handleClose();
            logger.log(Level.WARNING, "There was an error attempting to process an SCP write request: " + ((status == null) ? "null status" : status.getMessage()));
            throw new CausedIOException("Message processing failed: " + ((status == null) ? "null status" : status.getMessage()));
        }
        sshFile.handleClose();
        ack();
        readAck(false);
    }

    @Override
    protected void readFile(final SshFile file) throws IOException {
        logger.log(Level.FINER, "SCP reading file {0}", path);

        final VirtualSshFile sshFile = (VirtualSshFile) file;

        try {
            submitMessageProcessingTask(sshFile, CommandKnob.CommandType.GET);
        } catch (MessageProcessingException e) {
            sshFile.handleClose();
            throw new CausedIOException("Message processing failed reading a file via SCP", e);
        }
        //wait till the message processor finished processing, this is when the input stream will be set on the file.
        try {
            if(!sshFile.getMessageProcessingStatus().waitForMessageProcessingFinished(connector.getLongProperty(SshServerModule.LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS, DEFAULT_MAX_MESSAGE_PROCESSING_WAIT_TIME), TimeUnit.SECONDS)){
                sshFile.handleClose();
                //this means that message processing failed to finish before the time ran out.
                logger.log(Level.WARNING, "Message processing failed to finish in the allowed amount of time.");
                throw new CausedIOException("Error processing GET command for: " + file.getAbsolutePath());
            }
        } catch (InterruptedException e) {
            sshFile.handleClose();
            logger.log(Level.WARNING, "Interrupted waiting for message processing to finish: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new CausedIOException("Error processing GET command for: " + file.getAbsolutePath(), e);
        }

        AssertionStatus status = sshFile.getMessageProcessingStatus().getMessageProcessStatus();
        if (status != AssertionStatus.NONE) {
            sshFile.handleClose();
            logger.log(Level.WARNING, "There was an error attempting to process an SCP read request: " + ((status == null) ? "null status" : status.getMessage()));
            throw new CausedIOException("Message processing failed: " + ((status == null) ? "null status" : status.getMessage()));
        }

        StringBuffer buf = new StringBuffer();
        buf.append("C");
        buf.append("0644"); // what about perms
        buf.append(" ");

        long contentLength = sshFile.getMessageProcessingStatus().getResultContentLength();
        logger.log(Level.FINE, "SCP Read: Content length returned: " + contentLength);
        buf.append(contentLength); // length
        buf.append(" ");
        buf.append(sshFile.getName());
        buf.append("\n");
        out.write(buf.toString().getBytes());
        out.flush();
        readAck(false);

        IOUtils.copyStream(sshFile.getInputStream(), out);
        out.flush();
        sshFile.handleClose();
        ack();
        readAck(false);
    }

    @Override
    protected void readDir(SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Recursively reading directory {0} unsupported", path);
        }
        throw new IOException("Unsupported mode");
    }

    /**
     * Submit a virtual file for message processing. This will start policy processing. The output stream will be closed if this is a GET, LIST, STAT, DELETE, MKDIR, RMDIR, or MOVE command.
     *
     * @param file        The file to process
     * @param commandType The Type on ssh command being sent.
     */
    private void submitMessageProcessingTask(final VirtualSshFile file, final CommandKnob.CommandType commandType) throws MessageProcessingException {
        try {
            MessageProcessingSshUtil.submitMessageProcessingTask(connector, file, commandType, Collections.<String, String>emptyMap(), session, stashManagerFactory, threadPool, messageProcessor, soapFaultManager, messageProcessingEventChannel);
        } catch (ThreadPool.ThreadPoolShutDownException e) {
            logger.log(Level.WARNING, "Thread pool shutdown: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new MessageProcessingException("Error processing " + commandType.name() + " command for: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "IO exception processing message: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new MessageProcessingException("Error processing " + commandType.name() + " command for: " + file.getAbsolutePath(), e);
        } finally {
            switch (commandType) {
                case GET:
                case LIST:
                case STAT:
                case DELETE:
                case MKDIR:
                case RMDIR:
                case MOVE: {
                    //Close the File output stream since we don't write to it for these commands
                    ResourceUtils.closeQuietly(file.getOutputStream());
                    break;
                }
            }
        }
    }
}
