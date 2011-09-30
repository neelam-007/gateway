package com.l7tech.external.assertions.ssh.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.HasServiceOid;
import com.l7tech.message.HasServiceOidImpl;
import com.l7tech.message.Message;
import com.l7tech.message.SshKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.FaultProcessed;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyVersionException;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sshd.common.util.DirectoryScanner;
import org.apache.sshd.server.*;
import org.apache.sshd.server.session.ServerSession;

import java.io.*;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing for SCP support (heavily borrowed from org.apache.sshd.server.command.ScpCommand).
 */
public class MessageProcessingScpCommand implements Command, Runnable, SessionAware, FileSystemAware {
    protected static final Logger logger = Logger.getLogger(MessageProcessingScpCommand.class.getName());

    protected static final int OK = 0;
    protected static final int WARNING = 1;
    protected static final int ERROR = 2;

    protected String name;
    protected boolean optR;
    protected boolean optT;
    protected boolean optF;
    protected boolean optV;
    protected boolean optD;
    protected boolean optP;

    protected FileSystemView root;
    protected String path;
    
    protected InputStream in;
    protected OutputStream out;
    protected OutputStream err;
    protected ExitCallback callback;
    protected IOException error;

    // customized for Gateway
    private ServerSession session;
    private SsgConnector connector;
    private MessageProcessor messageProcessor;
    private EventChannel messageProcessingEventChannel;
    private SoapFaultManager soapFaultManager;
    private StashManagerFactory stashManagerFactory;

    public MessageProcessingScpCommand(String[] args, SsgConnector c, MessageProcessor mp, StashManagerFactory smf,
                                       SoapFaultManager sfm, EventChannel mpec) {
        connector = c;
        messageProcessor = mp;
        messageProcessingEventChannel = mpec;
        soapFaultManager = sfm;
        stashManagerFactory = smf;

        name = Arrays.asList(args).toString();
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Executing command {0}", name);
        }
        path = ".";
        for (int i = 1; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                for (int j = 1; j < args[i].length(); j++) {
                    switch (args[i].charAt(j)) {
                        case 'f':
                            optF = true;
                            break;
                        case 'p':
                            optP = true;
                            break;
                        case 'r':
                            optR = true;
                            break;
                        case 't':
                            optT = true;
                            break;
                        case 'v':
                            optV = true;
                            break;
                        case 'd':
                            optD = true;
                            break;
//                          default:
//                            error = new IOException("Unsupported option: " + args[i].charAt(j));
//                            return;
                    }
                }
            } else if (i == args.length - 1) {
                path = args[args.length - 1];
            }
        }
        if (!optF && !optT) {
            error = new IOException("Either -f or -t option should be set");
        }
    }

    public void setSession(ServerSession session) {
        this.session = session;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

    public void start(Environment env) throws IOException {
        if (error != null) {
            throw error;
        }
        new Thread(this, "MessageProcessingScpCommand: " + name).start();
    }

    public void destroy() {
    }

    public void run() {
        int exitValue = OK;
        String exitMessage = null;
        
        try {
            if (optT)
            {
                ack();
                for (; ;)
                {
                    String line;
                    boolean isDir = false;
                    int c = readAck(true);
                    switch (c)
                    {
                        case -1:
                            return;
                        case 'D':
                            isDir = true;
                        case 'C':
                            line = ((char) c) + readLine();
                            break;
                        case 'E':
                            line = ((char) c) + readLine();
                            break;
                        default:
                            //a real ack that has been acted upon already
                            continue;
                    }

                    if (optR && isDir)
                    {
                        writeDir(line, root.getFile(path));
                    }
                    else
                    {
                        writeFile(line, root.getFile(path));
                    }
                }
            } else if (optF) {
                String pattern = path;
                int idx = pattern.indexOf('*');
                if (idx >= 0) {
                    String basedir = "";
                    int lastSep = pattern.substring(0, idx).lastIndexOf('/');
                    if (lastSep >= 0) {
                        basedir = pattern.substring(0, lastSep);
                        pattern = pattern.substring(lastSep + 1);
                    }
                    String[] included = new DirectoryScanner(basedir, pattern).scan();
                    for (String path : included) {
                        SshFile file = root.getFile(basedir + "/" + path);
                        if (file.isFile()) {
                            readFile(file);
                        } else if (file.isDirectory()) {
                            if (!optR) {
                                out.write(WARNING);
                                out.write((path + " not a regular file\n").getBytes());
                            } else {
                                readDir(file);
                            }
                        } else {
                            out.write(WARNING);
                            out.write((path + " unknown file type\n").getBytes());
                        }
                    }
                } else {
                    String basedir = "";
                    int lastSep = pattern.lastIndexOf('/');
                    if (lastSep >= 0) {
                        basedir = pattern.substring(0, lastSep);
                        pattern = pattern.substring(lastSep + 1);
                    }
                    SshFile file = root.getFile(basedir + "/" + pattern);
                    if (!file.doesExist()) {
                        throw new IOException(file + ": no such file or directory");
                    }

                    // customized for Gateway
                    if (!optR) {
                        readFile(file);
                    } else {
                        readDir(file);
                    }
                }
            } else {
                throw new IOException("Unsupported mode");
            }
        } catch (IOException e) {
            try {
                exitValue = ERROR;
                exitMessage = e.getMessage();
                out.write(exitValue);
                out.write(exitMessage.getBytes());
                out.write('\n');
                out.flush();
            } catch (IOException e2) {
                // Ignore
            }
            logger.log(Level.INFO, "Error in scp command", e);
        } finally {
            if (callback != null) {
                callback.onExit(exitValue, exitMessage);
            }
        }
    }

    protected void writeDir(String header, SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Recursively writing dir {0} unsupported", path);
        }
        throw new IOException("Recursive directory write unsupported.");
    }

    protected void writeFile(String header, SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Writing file {0}", path);
        }
        if (!header.startsWith("C")) {
            throw new IOException("Expected a C message but got '" + header + "'");
        }

        String perms = header.substring(1, 5);
        long length = Long.parseLong(header.substring(6, header.indexOf(' ', 6)));
        String name = header.substring(header.indexOf(' ', 6) + 1);

        // if required, remove filename from the absolute path (e.g. jscape scp client appends the file name to the path)
        String absolutePath = path.getAbsolutePath();
        if (!StringUtils.isEmpty(absolutePath)) {
            int index = absolutePath.lastIndexOf("/" + name);
            if (index >= 0) {
                absolutePath = absolutePath.substring(0, index);
            }
        }

        pipeInputStreamToGatewayRequestMessage(connector, absolutePath, name, in, length);

        readAck(false);
    }

    protected String readLine() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;) {
            int c = in.read();
            if (c == '\n') {
                return baos.toString();
            } else if (c == -1) {
                throw new IOException("End of stream");
            } else {
                baos.write(c);
            }
        }
    }

    protected void readFile(SshFile path) throws IOException {
        throw new IOException("Copy from server currently unsupported.");
    }

    protected void readDir(SshFile path) throws IOException {
        if (logger.isLoggable(Level.FINER)) {
            logger.log(Level.FINER, "Recursively reading directory {0} unsupported", path);
        }
        throw new IOException("Recursive directory read unsupported.");
    }

    protected void ack() throws IOException {
        out.write(0);
        out.flush();
    }

    protected int readAck(boolean canEof) throws IOException {
        int c = in.read();
        switch (c) {
            case -1:
                if (!canEof) {
                    throw new EOFException();
                }
                break;
            case OK:
                break;
            case WARNING:
                logger.log(Level.WARNING, "Received warning: " + readLine());
                break;
            case ERROR:
                throw new IOException("Received nack: " + readLine());
            default:
                break;
        }
        return c;
    }

	public void setFileSystemView(FileSystemView view) {
		this.root = view;	
	}

    private boolean pipeInputStreamToGatewayRequestMessage(SsgConnector connector, String path, String file,
                                                           InputStream inputStream, long length) throws IOException {
        boolean success = false;
        Message request = new Message();

        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, null, true);
        final long requestSizeLimit = connector.getLongProperty(SsgConnector.PROP_REQUEST_SIZE_LIMIT, Message.getMaxBytes());
        final InputStream pis = getDataInputStream(inputStream, path, length);

        String ctypeStr = connector.getProperty(SsgConnector.PROP_OVERRIDE_CONTENT_TYPE);
        ContentTypeHeader ctype = ctypeStr == null ? ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.create(ctypeStr);

        request.initialize(stashManagerFactory.createStashManager(), ctype, pis, requestSizeLimit);

        // attach ssh knob
        String userName = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_USERNAME);
        String userPublicKey = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_PUBLIC_KEY);
        if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPublicKey)) {
            request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                    session.getIoSession().getRemoteAddress(), file, path, new SshKnob.PublicKeyAuthentication(userName, userPublicKey)));
        } else {
            userName = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_USERNAME);
            String userPassword = (String) session.getIoSession().getAttribute(SshServerModule.MINA_SESSION_ATTR_CRED_PASSWORD);
            if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPassword)) {
                request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file, path, new PasswordAuthentication(userName, userPassword.toCharArray())));
            } else {
                request.attachKnob(SshKnob.class, MessageProcessingSshUtil.buildSshKnob(session.getIoSession().getLocalAddress(),
                        session.getIoSession().getRemoteAddress(), file, path, null, null));
            }
        }

        long hardwiredServiceOid = connector.getLongProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, -1);
        if (hardwiredServiceOid != -1) {
            request.attachKnob(HasServiceOid.class, new HasServiceOidImpl(hardwiredServiceOid));
        }

            AssertionStatus status = AssertionStatus.UNDEFINED;
            String faultXml = null;
            try {
                try {
                    status = messageProcessor.processMessage(context);

                    if (logger.isLoggable(Level.FINER))
                        logger.log(Level.FINER, "Policy resulted in status ''{0}''.", status);

                } catch ( PolicyVersionException pve ) {
                    logger.log( Level.INFO, "Request referred to an outdated version of policy" );
                    faultXml = soapFaultManager.constructExceptionFault(pve, context.getFaultlevel(), context).getContent();
                } catch ( Throwable t ) {
                    logger.log( Level.WARNING, "Exception while processing SCP message", t );
                    faultXml = soapFaultManager.constructExceptionFault(t, context.getFaultlevel(), context).getContent();
                }

                if ( status != AssertionStatus.NONE ) {
                    faultXml = soapFaultManager.constructReturningFault(context.getFaultlevel(), context).getContent();
                } else {
                    success = true;
                }

                if (faultXml != null) {
                    messageProcessingEventChannel.publishEvent(new FaultProcessed(context, faultXml, messageProcessor));
                }
            } finally {
                ResourceUtils.closeQuietly(context);
                ResourceUtils.closeQuietly(pis);
            }
        return success;
    }

    /*
     * Convert OutputStream to InputStream.
     */
    private InputStream getDataInputStream(final InputStream inputStream,
                                           final String fullPath, final long length) throws IOException {
        final PipedInputStream pis = new PipedInputStream();

        final CountDownLatch startedSignal = new CountDownLatch(1);
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                PipedOutputStream pos = null;
                try {
                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Starting data transfer for ''{0}''.", fullPath);

                    pos = new PipedOutputStream(pis);
                    startedSignal.countDown();
                    transferFromClient(inputStream, pos, length);

                    if (logger.isLoggable(Level.FINE))
                        logger.log(Level.FINE, "Completed data transfer for ''{0}''.", fullPath);
                }
                catch (IOException ioe) {
                    logger.log(Level.WARNING, "Data transfer error for '"+fullPath+"'.", ioe);
                }
                finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                }
            }
        }, "ScpServer-DataTransferThread-" + System.currentTimeMillis());

        thread.setDaemon(true);
        thread.start();

        try {
            startedSignal.await();
        }
        catch(InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for data.", ie);
        }

        return pis;
    }

    private void transferFromClient(InputStream inputStream, OutputStream outputStream, long length) throws IOException {
        try {
            ack();

            byte[] buffer = new byte[8192];
            while (length > 0) {
                int len = (int) Math.min(length, buffer.length);
                len = inputStream.read(buffer, 0, len);
                if (len <= 0) {
                    throw new IOException("End of stream reached");
                }
                outputStream.write(buffer, 0, len);
                length -= len;
            }
        } finally {
            outputStream.close();
        }

        ack();
    }
}
