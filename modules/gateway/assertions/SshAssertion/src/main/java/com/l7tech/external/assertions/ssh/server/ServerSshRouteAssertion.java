package com.l7tech.external.assertions.ssh.server;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.CommandKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.SshKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerRoutingAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.ssh.SshSession;
import com.l7tech.server.ssh.SshSessionKey;
import com.l7tech.server.ssh.SshSessionPool;
import com.l7tech.server.ssh.client.*;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import com.l7tech.util.ThreadPool.ThreadPoolShutDownException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static com.l7tech.external.assertions.ssh.server.SshAssertionMessages.*;
import static com.l7tech.gateway.common.audit.AssertionMessages.*;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.ExceptionUtils.getDebugException;
import static com.l7tech.util.ExceptionUtils.getMessage;
import static com.l7tech.util.Functions.grep;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.TextUtils.isNotEmpty;
import static com.l7tech.util.TextUtils.trim;

/**
 * Server side implementation of the SshRouteAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshRouteAssertion
 */
public class ServerSshRouteAssertion extends ServerRoutingAssertion<SshRouteAssertion> {

    @Inject
    private Config config;
    @Inject
    private SecurePasswordManager securePasswordManager;
    @Inject
    private StashManagerFactory stashManagerFactory;
    @Inject
    @Named("sshResponseDownloadThreadPool")
    private ThreadPoolBean threadPool;
    private final String[] variablesUsed;

    @Inject
    private SshSessionPool sshSessionPool;

    public static final String defaultCipherOrder = "aes128-ctr, aes128-cbc, 3des-cbc, blowfish-cbc, aes192-ctr, aes192-cbc, aes256-ctr, aes256-cbc";
    public static final String defaultMacOrder = "hmac-md5,hmac-sha1,hmac-sha2-256,hmac-sha1-96,hmac-md5-96";
    public static final String defaultCompressionOrder = "none";

    public ServerSshRouteAssertion(SshRouteAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion, context);
        this.variablesUsed = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        final Message request;
        try {
            request = context.getTargetMessage(assertion.getRequestTarget());
        } catch (NoSuchVariableException e) {
            throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, e.getMessage(), e);
        }

        final MimeKnob mimeKnob = request.getKnob(MimeKnob.class);
        if (mimeKnob == null) {
            // Uninitialized request
            logAndAudit(SSH_ROUTING_ERROR, "Request is not initialized; nothing to route");
            return AssertionStatus.BAD_REQUEST;
        }

        //This will find the command type that should be processed.
        final CommandKnob.CommandType commandType;
        final Map<String, ?> variables = context.getVariableMap(variablesUsed, getAudit());
        if (assertion.isRetrieveCommandTypeFromVariable()) {
            final String commandTypeString;
            try {
                commandTypeString = context.getVariable(assertion.getCommandTypeVariableName()).toString();
            } catch (NoSuchVariableException e) {
                logAndAudit(SSH_ROUTING_ERROR, "Command type variable not found: " + assertion.getCommandTypeVariableName());
                return AssertionStatus.BAD_REQUEST;
            }
            try {
                commandType = CommandKnob.CommandType.valueOf(commandTypeString);
            } catch (Exception e) {
                logAndAudit(SSH_ROUTING_ERROR, "Invalid command type given: " + commandTypeString);
                return AssertionStatus.BAD_REQUEST;
            }
        } else {
            commandType = assertion.getCommandType();
        }

        // if uploading from the Gateway, delete current security header if necessary
        if (CommandKnob.CommandType.PUT.equals(commandType)) {
            try {
                handleProcessedSecurityHeader(request);
            } catch (SAXException se) {
                logger.log(Level.INFO, "Error processing security header, request XML invalid ''{0}''", se.getMessage());
            }
        }

        //create the session key
        final SshSessionKey sshSessionKey = buildSshSessionKey(context, variables);

        //Create a new session
        SshSession session = null;
        try {
            try {
                //noinspection unchecked
                session = (SshSession) sshSessionPool.borrowObject(sshSessionKey);
            } catch (Exception e) {
                logAndAudit(SSH_ROUTING_ERROR, new String[]{"Failed to retrieve ssh session: " + getMessage(e)}, getDebugException(e));
                return AssertionStatus.FAILED;
            }

            //create the ssh client
            FileTransferClient sshClientToUse = null;
            try {
                if (assertion.isScpProtocol()) {
                    sshClientToUse = session.getScpClient();
                } else {
                    sshClientToUse = session.getSftpClient();
                }
            } catch (JSchException e) {
                if (sshClientToUse != null) {
                    sshClientToUse.close();
                }
                logAndAudit(SSH_ROUTING_ERROR, new String[]{"Failed to get an sshClient: " + getMessage(e)}, getDebugException(e));
                return AssertionStatus.FAILED;
            }

            final FileTransferClient sshClient = sshClientToUse;
            try {
                try {
                    sshClient.connect();
                } catch (JSchException e) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[]{"Unable to connect the ssh client: " + getMessage(e)}, getDebugException(e));
                    return AssertionStatus.FAILED;
                }

                // process the command type
                final String fileName = ExpandVariables.process(assertion.getFileName(), variables, getAudit());
                final String directory = ExpandVariables.process(assertion.getDirectory(), variables, getAudit());
                logAndAudit(SSH_ROUTING_INFO,
                        new String[]{"Running route via " + (assertion.isScpProtocol() ? "scp" : "sftp") + " command: " + commandType + ", directory: " + directory + ", file: " + fileName + ", key: " + sshSessionKey});
                if (CommandKnob.CommandType.GET.equals(commandType)) {
                    if (!performGetCommand(session, sshClient, fileName, directory, context, variables))
                        return AssertionStatus.FAILED;
                } else if (CommandKnob.CommandType.PUT.equals(commandType)) {
                    //get the request ssh knob. This is used to preserve file permissions.
                    final SshKnob sshKnob = request.getKnob(SshKnob.class);
                    if (!performPutCommand(sshClient, fileName, directory, mimeKnob, variables, assertion.isPreserveFileMetadata() && sshKnob != null ? sshKnob.getFileMetadata() : null))
                        return AssertionStatus.FAILED;
                    logAndAudit(SSH_ROUTING_INFO, new String[]{"Finished sending file: " + fileName + " in Session: " + session.getKey().toString()});
                } else if (assertion.isScpProtocol()) {
                    logAndAudit(SSH_ROUTING_ERROR, "Unsupported SCP command type: " + commandType);
                    return AssertionStatus.BAD_REQUEST;
                } else {
                    switch (commandType) {
                        case LIST: {
                            if (!performListCommand((SftpClient) sshClient, fileName, directory, context))
                                return AssertionStatus.FAILED;
                            break;
                        }
                        case STAT: {
                            if (!performStatCommand((SftpClient) sshClient, fileName, directory, context))
                                return AssertionStatus.FAILED;
                            break;
                        }
                        case DELETE: {
                            boolean explicitCheck = config.getBooleanProperty("sftpRoutingExplicitlyValidateDeleteFile", true);
                            performDeleteCommand((SftpClient) sshClient, fileName, directory, explicitCheck);
                            break;
                        }
                        case MOVE: {
                            final String newFileName = ExpandVariables.process(assertion.getNewFileName(), variables, getAudit());
                            performMoveCommand((SftpClient) sshClient, fileName, directory, newFileName);
                            break;
                        }
                        case MKDIR: {
                            boolean explicitCheck = config.getBooleanProperty("sftpRoutingExplicitlyValidateMkdir", true);
                            performMkdirCommand((SftpClient) sshClient, fileName, directory, explicitCheck);
                            break;
                        }
                        case RMDIR: {
                            boolean explicitCheck = config.getBooleanProperty("sftpRoutingExplicitlyValidateDeleteDir", true);
                            performRmdirCommand((SftpClient) sshClient, fileName, directory, explicitCheck);
                            break;
                        }
                        default: {
                            //This shouldn't ever happen
                            logAndAudit(SSH_ROUTING_ERROR, "Unsupported command type: " + commandType);
                            return AssertionStatus.BAD_REQUEST;
                        }
                    }
                }
            } catch (SftpException | FileTransferException | JSchException e) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{SSH_EXCEPTION_ERROR + " '" + getMessage(e) + "', server: " + sshSessionKey.getHost() + ", command: " + commandType}, getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (IOException e) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{SSH_IO_EXCEPTION + " '" + getMessage(e) + "', server: " + sshSessionKey.getHost() + ", command: " + commandType}, getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (NoSuchPartException e) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{SSH_NO_SUCH_PART_ERROR + ", server: " + sshSessionKey.getHost() + ", command: " + commandType}, getDebugException(e));
                return AssertionStatus.FAILED;
            } catch (Throwable t) {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{getMessage(t)}, getDebugException(t));
                return AssertionStatus.FAILED;
            } finally {
                //on all commands except GET commands the ssh client needs to be disconnected.
                //On GET commands the client should be left connected this way data can still be streamed to the client once policy finished processing.
                //It will be disconnected automatically after the data is sent.
                if (sshClient != null && !CommandKnob.CommandType.GET.equals(commandType)) {
                    sshClient.close();
                }
            }
        } finally

        {
            //on all commands except GET commands the ssh client needs to be disconnected.
            //On GET commands the client should be left connected this way data can still be streamed to the client once policy finished processing.
            //It will be disconnected automatically after the data is sent.
            if (session != null && !CommandKnob.CommandType.GET.equals(commandType)) {
                try {
                    sshSessionPool.returnObject(sshSessionKey, session);
                } catch (Exception e) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[]{"Failed to return ssh session to pool: " + getMessage(e)}, getDebugException(e));
                }
            }
        }

        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
    }

    private void performRmdirCommand(SftpClient sftpClient, String fileName, String directory, boolean explicitCheck) throws SftpException {
        try {
            sftpClient.removeDirectory(directory, fileName);
        } catch (SftpException e) {
            if (explicitCheck) {
                throw e;
            }
        }
    }

    private void performMkdirCommand(SftpClient sftpClient, String fileName, String directory, boolean explicitCheck) throws SftpException {
        try {
            sftpClient.createDirectory(directory, fileName);
        } catch (SftpException e) {
            if (explicitCheck) {
                throw e;
            }
        }
    }

    private void performMoveCommand(SftpClient sftpClient, String fileName, String directory, String newFileName) throws SftpException {
        sftpClient.renameFile(directory, fileName, newFileName);
    }

    private void performDeleteCommand(SftpClient sftpClient, String fileName, String directory, boolean explicitCheck) throws SftpException {
        try {
            sftpClient.deleteFile(directory, fileName);
        } catch (SftpException e) {
            if (explicitCheck) {
                throw e;
            }
        }
    }

    private boolean performStatCommand(final SftpClient sftpClient, final String fileName, final String directory, final PolicyEnforcementContext context) throws IOException, ThreadPoolShutDownException, NoSuchVariableException, InterruptedException {
        final PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);

        // start stat task.
        Future<Void> statTask = startSshResponseTask(sftpClient, new Functions.NullaryVoidThrows<Exception>() {
            @Override
            public void call() throws Exception {
                XmlSshFile sftpFile = sftpClient.getFileAttributes(directory, fileName);
                //Save the file size to a context variable.
                if (assertion.isSetFileSizeToContextVariable() && sftpFile != null) {
                    context.setVariable(assertion.getSaveFileSizeContextVariable(), sftpFile.getSize());
                }
                //create the xml virtual file list.
                XmlVirtualFileList xmlFileList = sftpFile != null ?
                        new XmlVirtualFileList(Arrays.asList(sftpFile)) :
                        new XmlVirtualFileList();
                //exceptions thrown here will be handled by the below catch
                JAXB.marshal(xmlFileList, pos);
                ResourceUtils.flushAndCloseQuietly(pos);
            }
        }, pos);

        // Create the response message
        final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
        response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.XML_DEFAULT, pis);
        //waits for the file stat to complete
        try {
            //need to call getContentLength so that the entire input stream is read and stashed. If this is not called JAXB.marshal(xmlFileList, pos); can hang
            response.getMimeKnob().getContentLength();
            statTask.get();
        } catch (ExecutionException e) {
            logAndAudit(SSH_ROUTING_ERROR, new String[]{"SSH2 Route Assertion error: Error getting file attributes: " + getMessage(e)}, getDebugException(e));
            return false;
        }
        return true;
    }

    private boolean performListCommand(final SftpClient sftpClient, final String fileName, final String directory, PolicyEnforcementContext context) throws IOException, ThreadPoolShutDownException, NoSuchVariableException, InterruptedException {
        final PipedInputStream pis = new PipedInputStream();
        final PipedOutputStream pos = new PipedOutputStream(pis);

        // start directory listing task.
        Future<Void> listTask = startSshResponseTask(sftpClient, new Functions.NullaryVoidThrows<Exception>() {
            @Override
            public void call() throws Exception {
                XmlVirtualFileList xmlFileList = sftpClient.listDirectory(directory, fileName);
                //exceptions thrown here will be handled by the below catch
                JAXB.marshal(xmlFileList, pos);
                ResourceUtils.flushAndCloseQuietly(pos);
            }
        }, pos);

        //Create the response message
        final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
        response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.XML_DEFAULT, pis);
        //waits for the directory listing to complete
        try {
            //need to call getContentLength so that the entire input stream is read and stashed. If this is not called JAXB.marshal(xmlFileList, pos); will hang forever when listing large directories
            response.getMimeKnob().getContentLength();
            listTask.get();
        } catch (ExecutionException e) {
            logAndAudit(SSH_ROUTING_ERROR, new String[]{"SSH2 Route Assertion error: Error getting directory listing: " + getMessage(e)}, getDebugException(e));
            return false;
        }
        return true;
    }

    private boolean performPutCommand(FileTransferClient sshClient, String fileName, String directory, MimeKnob mimeKnob, Map<String, ?> variables, SshKnob.FileMetadata fileMetadata) throws IOException, JSchException, FileTransferException, NoSuchPartException {
        // Get the file length
        long fileLength = getFileLength(variables);
        if (assertion.isScpProtocol() && fileLength == -1) {
            //This will force the entire message to be read and stashed. It will also get the full file length
            fileLength = mimeKnob.getContentLength();
        }
        // Get the file offset
        final long fileOffset = getFileOffset(variables);

        //create xmlfile so that permissions can be set.
        final XmlSshFile xmlSshFile;
        if (fileMetadata == null) {
            xmlSshFile = null;
        } else {
            xmlSshFile = new XmlSshFile();
            xmlSshFile.setPermissions(fileMetadata.getPermission());
        }
        // Upload the message. This will block until the entire file has been uploaded.
        // Appending when the offset is > 0 is how the jscape client worked by default. This needs to be here for backwards compatibility with Goatfish.
        // Todo: introduce a cluster property to implement appending in a better way.
        sshClient.upload(mimeKnob.getEntireMessageBodyAsInputStream(), directory, fileName,
                fileLength, fileOffset, (sshClient instanceof SftpClient) && fileOffset > 0, xmlSshFile, null);
        return true;
    }

    private boolean performGetCommand(final SshSession session, final FileTransferClient sshClient, final String fileName, final String directory, PolicyEnforcementContext context, Map<String, ?> variables) throws IOException, ThreadPoolShutDownException, InterruptedException, NoSuchVariableException {
        // Use this to wait till the input stream to the file is properly retrieved and any additional data is retrieved (file size)
        final CountDownLatch gotData = new CountDownLatch(1);
        //This will hold any exceptions that may have been thrown attempting to retrieve the input stream to the file
        final AtomicReference<Throwable> gettingStreamException = new AtomicReference<>(null);
        // Get the response byte limit
        final long byteLimit = getByteLimit(variables);
        final AtomicLong fileSize = new AtomicLong(-1);
        final PipedInputStream pis = new PipedInputStream();
        try {
            final PipedOutputStream pos = new PipedOutputStream(pis);
            // Get the file length
            final long fileLength = getFileLength(variables);
            // Get the file offset
            final long fileOffset = getFileOffset(variables);

            //Save the file size to a context variable.
            final FileTransferProgressMonitor progressMonitor;
            progressMonitor = new FileTransferProgressMonitor() {
                @Override
                public void start(int op, XmlSshFile file) {
                    fileSize.set(file.getSize());
                    gotData.countDown();
                }

                @Override
                public void progress(long count) {
                    //do nothing
                }

                @Override
                public void end() {
                    logger.log(Level.FINE, "SSH routing: Finished retrieving file: " + fileName + " in Session: " + session.getKey().toString());
                }
            };

            // start download task. This will run in the background after this assertion completes until the entire file has been received.
            startSshResponseTask(sshClient, new Functions.NullaryVoidThrows<Exception>() {
                @Override
                public void call() throws Exception {
                    try {
                        sshClient.download(pos, directory, fileName, fileLength, fileOffset, progressMonitor);
                    } catch (Throwable t) {
                        //save the exception thrown trying to download the file
                        gettingStreamException.set(t);
                        throw t;
                    } finally {
                        //need to countdown in case this was not called already.
                        gotData.countDown();
                        //need to disconnect the client manually for GET requests.
                        sshClient.close();
                        //need to return the session to the pool.
                        sshSessionPool.returnObject(session.getKey(), session);
                    }
                }
            }, pos);
        } catch (Throwable t) {
            //if there was an exception thrown we have to make sure that the ssh client is closed and the session is returned to the pool to avoid session leaks.
            //need to countdown in case this was not called already.
            gotData.countDown();
            //need to disconnect the client manually for GET requests.
            sshClient.close();
            //need to return the session to the pool.
            try {
                sshSessionPool.returnObject(session.getKey(), session);
            } catch (Exception e) {
                //log the exception but do nothing and throw the above exception
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{"Error returning ssh session to pool: " + getMessage(e)}, getDebugException(e));
            }
            throw t;
        }
        //wait the the input stream to the sftp file is retrieved and data is ready to be sent.
        gotData.await();
        //check if there was an error retrieving the input stream.
        Throwable exceptionThrown = gettingStreamException.get();
        if (exceptionThrown != null) {
            logAndAudit(SSH_ROUTING_ERROR,
                    new String[]{"Error opening file stream: " + getMessage(exceptionThrown)}, getDebugException(exceptionThrown));
            return false;
        }
        if (assertion.isSetFileSizeToContextVariable() && fileSize.get() >= 0) {
            context.setVariable(assertion.getSaveFileSizeContextVariable(), fileSize.get());
        }
        //Create the response message
        final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
        //need to do file size + 1 because of how the ByteLimitInput stream works. It starts throwing exceptions when the given amount of bytes are read (used >= in comparison). So add one to avoid exceptions thrown.
        //if the file size was never set (==-1) then return the byte limit. If the file size is set and the byte limit is unlimited (0) return the file size. if the file size is set and the byte limit is set return the minimum of the two.
        response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.create(assertion.getDownloadContentType()), pis, fileSize.get() >= 0 ? byteLimit > 0 ? Math.min(fileSize.get() + 1, byteLimit) : fileSize.get() + 1 : byteLimit);
        return true;
    }

    /**
     * This will attempt to retrieve the file offset. If it fails the default file offset is used.
     *
     * @param variables The available variables in the policy context
     * @return The file offset to use.
     */
    private long getFileOffset(Map<String, ?> variables) {
        long fileOffset = SshRouteAssertion.DEFAULT_FILE_OFFSET;
        String fileOffsetStr = ExpandVariables.process(assertion.getFileOffset(), variables, getAudit());
        try {
            fileOffset = Long.parseLong(fileOffsetStr);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Used default file offset: " + fileOffset + ".  " + getMessage(e), getDebugException(e));
        }
        logger.log(Level.FINE, "File Offset: {0}.", fileOffset);
        return fileOffset;
    }

    /**
     * This will attempt to retrieve the file length. If it fails the default file length is used.
     *
     * @param variables The available variables in the policy context
     * @return The file length to use.
     */
    private long getFileLength(Map<String, ?> variables) {
        long fileLength = SshRouteAssertion.DEFAULT_FILE_LENGTH;
        String fileLengthStr = ExpandVariables.process(assertion.getFileLength(), variables, getAudit());
        try {
            fileLength = Long.parseLong(fileLengthStr);
        } catch (NumberFormatException e) {
            logger.log(Level.WARNING, "Used default file length: " + fileLength + ".  " + getMessage(e), getDebugException(e));
        }
        logger.log(Level.FINE, "File Length: {0}.", fileLength);
        return fileLength;
    }

    /**
     * This will attempt to retrieve the byte limit. If it fails the default byte limit is used.
     *
     * @param variables The available variables in the policy context
     * @return The byte limit to use.
     */
    private long getByteLimit(Map<String, ?> variables) {
        // response byte limit
        long byteLimit = getMaxBytes();
        if (assertion.getResponseByteLimit() != null) {
            String byteLimitStr = ExpandVariables.process(assertion.getResponseByteLimit(), variables, getAudit());
            try {
                byteLimit = Long.parseLong(byteLimitStr);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "Used default response byte limit: " + byteLimit + ".  " + getMessage(e), getDebugException(e));
            }
        }
        logger.log(Level.FINE, "Response byte limit: {0}.", byteLimit);
        return byteLimit;
    }

    /**
     * This will start a separate task in order to process commands that write to the response.
     *
     * @param sshClient          The ssh client that is being used to process the command
     * @param processingFunction The Funcation that will do the actual processing.
     * @param pos                The output stream that will write to the rresponse message
     * @return The future that is processing this command
     * @throws IOException
     * @throws ThreadPoolShutDownException
     */
    private Future<Void> startSshResponseTask(final SshClient sshClient,
                                              final Functions.NullaryVoidThrows<Exception> processingFunction,
                                              final PipedOutputStream pos) throws IOException, ThreadPoolShutDownException {
        final CountDownLatch startedSignal = new CountDownLatch(1);

        final Future<Void> future = threadPool.submitTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    startedSignal.countDown();
                    processingFunction.call();
                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                    sshClient.close();
                }
                return null;
            }
        });

        // Wait till processing has started before continuing
        try {
            startedSignal.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new CausedIOException("Interrupted waiting for download.", ie);
        }

        return future;
    }

    private SshSessionKey buildSshSessionKey(final PolicyEnforcementContext context, final Map<String, ?> variables) {

        // determine username and password based or if they were pass-through or specified
        String username = null;
        String password = null;
        if (assertion.isCredentialsSourceSpecified()) {
            username = ExpandVariables.process(assertion.getUsername(), variables, getAudit());
            if (assertion.getPasswordGoid() != null) {
                try {
                    SecurePassword securePassword = securePasswordManager.findByPrimaryKey(assertion.getPasswordGoid());
                    if (securePassword == null) {
                        logAndAudit(SSH_ROUTING_ERROR, "Unable to find stored password for GOID: " + assertion.getPasswordGoid());
                        throw new AssertionStatusException(AssertionStatus.FAILED);
                    }
                    password = new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
                } catch (FindException | ParseException e) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[]{getMessage(e)}, getDebugException(e));
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
            }
        } else {
            final LoginCredentials credentials = context.getDefaultAuthenticationContext().getLastCredentials();
            if (credentials != null && credentials.getFormat() == CredentialFormat.CLEARTEXT) {
                username = credentials.getName();
                password = new String(credentials.getCredentials());
            }
            if (username == null) {
                logAndAudit(SSH_ROUTING_PASSTHRU_NO_USERNAME);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }

        final String host = ExpandVariables.process(assertion.getHost(), variables, getAudit());
        int port;
        try {
            port = Integer.parseInt(ExpandVariables.process(assertion.getPort(), variables, getAudit()));
        } catch (Exception e) {
            //use default port
            port = 22;
            logger.log(Level.INFO, "Unable to parse given port number, using default port 22.", getDebugException(e));
        }

        String publicKeyFingerprint = null;
        if (assertion.isUsePublicKey() && assertion.getSshPublicKey() != null) {
            publicKeyFingerprint = ExpandVariables.process(assertion.getSshPublicKey(), variables, getAudit());

            // validate public key fingerprint
            final Option<String> fingerprintIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
            if (fingerprintIsValid.isSome()) {
                logAndAudit(SSH_ROUTING_ERROR, SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }

        String privateKeyText = null;
        if (assertion.isUsePrivateKey() && assertion.getPrivateKeyGoid() != null) {
            try {
                SecurePassword securePemPrivateKey = securePasswordManager.findByPrimaryKey(assertion.getPrivateKeyGoid());
                if (securePemPrivateKey == null) {
                    logAndAudit(SSH_ROUTING_ERROR, "Unable to find stored PEM private key for GOID: " + assertion.getPrivateKeyGoid());
                    throw new AssertionStatusException(AssertionStatus.FAILED);
                }
                privateKeyText = new String(securePasswordManager.decryptPassword(securePemPrivateKey.getEncodedPassword()));
            } catch (FindException | ParseException e) {
                logAndAudit(SSH_ROUTING_ERROR, new String[]{getMessage(e)}, getDebugException(e));
                throw new AssertionStatusException(AssertionStatus.FAILED);
            }
        }

        boolean enableMacNone = ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.server.enableMacNone", false);
        boolean enableMacMd5 = ConfigFactory.getBooleanProperty("com.l7tech.external.assertions.ssh.server.enableMacMd5", false);

        //get the cipher algorithms to use
        final List<String> ciphers = grep(map(list(config.getProperty("sshRoutingEnabledCiphers", defaultCipherOrder).split("\\s*,\\s*")), trim()), isNotEmpty());

        //get the mac algorithms to use.
        List<String> macs = grep(map(list(config.getProperty("sshRoutingEnabledMacs", defaultMacOrder).split("\\s*,\\s*")), trim()), isNotEmpty());
        //Note the below mac changes were previously existing. They referenced bugs: SSG-5440, and SSG-5563. It is unknown if they are still needed.
        if (!enableMacNone) {
            macs.remove("none");
        }
        // Remove MD5 hash by default, always prefer SHA-1
        if (!enableMacMd5) {
            macs.remove("hmac-md5");
        }
        int sha1Index = macs.indexOf("hmac-sha1");
        if (sha1Index > 0) {
            macs.remove("hmac-sha1");
            macs = new LinkedList<>(macs);
            ((LinkedList<String>) macs).push("hmac-sha1");
        }

        //get the compression algorithms to use
        final List<String> compressions = grep(map(list(config.getProperty("sshRoutingEnabledCompressions", defaultCompressionOrder).split("\\s*,\\s*")), trim()), isNotEmpty());

        return new SshSessionKey(
                username,
                host,
                port,
                assertion.isUsePrivateKey() ? Either.<String, String>right(privateKeyText) : Either.<String, String>left(password),
                (int) TimeUnit.SECONDS.toMillis((long) assertion.getConnectTimeout()),
                (int) TimeUnit.SECONDS.toMillis((long) assertion.getReadTimeout()),
                assertion.isUsePublicKey() ? publicKeyFingerprint : null, ciphers, macs, compressions);
    }
}

