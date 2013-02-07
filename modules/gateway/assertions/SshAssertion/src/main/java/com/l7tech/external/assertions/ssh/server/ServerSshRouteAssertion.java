package com.l7tech.external.assertions.ssh.server;

import com.jscape.inet.scp.Scp;
import com.jscape.inet.scp.ScpException;
import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpConfiguration;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.sftp.SftpFile;
import com.jscape.inet.ssh.util.HostKeyFingerprintVerifier;
import com.jscape.inet.ssh.util.SshHostKeys;
import com.jscape.inet.ssh.util.SshParameters;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.ssh.SshRouteAssertion;
import com.l7tech.external.assertions.ssh.keyprovider.SshKeyUtil;
import com.l7tech.external.assertions.ssh.server.client.ScpClient;
import com.l7tech.external.assertions.ssh.server.client.SftpClient;
import com.l7tech.external.assertions.ssh.server.client.SshClient;
import com.l7tech.external.assertions.ssh.server.client.SshClientConfiguration;
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
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.*;
import com.l7tech.util.ThreadPool.ThreadPoolShutDownException;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import static com.l7tech.external.assertions.ssh.server.SshAssertionMessages.*;
import static com.l7tech.external.assertions.ssh.server.client.SshClientConfiguration.defaultCipherOrder;
import static com.l7tech.gateway.common.audit.AssertionMessages.SSH_ROUTING_ERROR;
import static com.l7tech.gateway.common.audit.AssertionMessages.SSH_ROUTING_PASSTHRU_NO_USERNAME;
import static com.l7tech.message.Message.getMaxBytes;
import static com.l7tech.util.CollectionUtils.list;
import static com.l7tech.util.CollectionUtils.toSet;
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
            try {
                commandType = CommandKnob.CommandType.valueOf(context.getVariable(assertion.getCommandTypeVariableName()).toString());
            } catch (Exception e) {
                logAndAudit(SSH_ROUTING_ERROR, "Invalid command type given: " + ExpandVariables.process(assertion.getCommandTypeVariableName(), variables, getAudit()));
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


        // determine username and password based or if they were pass-through or specified
        String username = null;
        String password = null;
        if (assertion.isCredentialsSourceSpecified()) {
            username = ExpandVariables.process(assertion.getUsername(), variables, getAudit());
            if (assertion.getPasswordOid() != null) {
                try {
                    SecurePassword securePassword = securePasswordManager.findByPrimaryKey(assertion.getPasswordOid());
                    if (securePassword == null) {
                        logAndAudit(SSH_ROUTING_ERROR, "Unable to find stored password for OID: " + assertion.getPasswordOid());
                        return AssertionStatus.FAILED;
                    }
                    password = new String(securePasswordManager.decryptPassword(securePassword.getEncodedPassword()));
                } catch (FindException fe) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[]{getMessage(fe)}, getDebugException(fe));
                    return AssertionStatus.FAILED;
                } catch (ParseException pe) {
                    logAndAudit(SSH_ROUTING_ERROR, new String[]{getMessage(pe)}, getDebugException(pe));
                    return AssertionStatus.FAILED;
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
                return AssertionStatus.FAILED;
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

        SshParameters sshParams = new SshParameters(host, port, username, password);
        sshParams.setConnectionTimeout(TimeUnit.SECONDS.toMillis((long) assertion.getConnectTimeout()));
        sshParams.setReadingTimeout(TimeUnit.SECONDS.toMillis((long) assertion.getReadTimeout()));

        if (assertion.isUsePublicKey() && assertion.getSshPublicKey() != null) {
            final String publicKeyFingerprint = ExpandVariables.process(assertion.getSshPublicKey(), variables, getAudit());

            // validate public key fingerprint
            final Option<String> fingerprintIsValid = SshKeyUtil.validateSshPublicKeyFingerprint(publicKeyFingerprint);
            if (fingerprintIsValid.isSome()) {
                logAndAudit(SSH_ROUTING_ERROR, SSH_INVALID_PUBLIC_KEY_FINGERPRINT_EXCEPTION);
                return AssertionStatus.FAILED;
            }
            SshHostKeys sshHostKeys = new SshHostKeys();
            try {
                sshHostKeys.addKey(InetAddress.getByName(host), publicKeyFingerprint);
            } catch (UnknownHostException e) {
                logAndAudit(SSH_ROUTING_ERROR, new String[]{"Bad Host: " + getMessage(e)}, getDebugException(e));
                return AssertionStatus.FAILED;
            }
            sshParams.setHostKeyVerifier(new HostKeyFingerprintVerifier(sshHostKeys));
        }

        if (assertion.isUsePrivateKey() && assertion.getPrivateKeyOid() != null) {
            String privateKeyText;
            try {
                SecurePassword securePemPrivateKey = securePasswordManager.findByPrimaryKey(assertion.getPrivateKeyOid());
                if (securePemPrivateKey == null) {
                    logAndAudit(SSH_ROUTING_ERROR, "Unable to find stored PEM private key for OID: " + assertion.getPrivateKeyOid());
                    return AssertionStatus.FAILED;
                }
                privateKeyText = new String(securePasswordManager.decryptPassword(securePemPrivateKey.getEncodedPassword()));
            } catch (FindException | ParseException e) {
                logAndAudit(SSH_ROUTING_ERROR, new String[]{getMessage(e)}, getDebugException(e));
                return AssertionStatus.FAILED;
            }

            sshParams.setSshPassword(null);
            sshParams.setPrivateKey(privateKeyText);
        }

        SshClient sshClient;
        final Set<String> ciphers = toSet(grep(map(list(config.getProperty("sshRoutingEnabledCiphers", defaultCipherOrder).split("\\s*,\\s*")), trim()), isNotEmpty()));
        if (assertion.isScpProtocol()) {
            sshClient = new ScpClient(new Scp(sshParams, new SshClientConfiguration(sshParams, ciphers)));
        } else {
            sshClient = new SftpClient(new Sftp(sshParams, new SftpConfiguration(new SshClientConfiguration(sshParams, ciphers))));
        }
        try {
            sshClient.connect();
        } catch (IOException e) {
            logAndAudit(SSH_ROUTING_ERROR, new String[]{"Unable to connect to ssh server: " + getMessage(e)}, getDebugException(e));
            return AssertionStatus.FAILED;
        }

        if (!sshClient.isConnected()) {
            sshClient.disconnect();
            logAndAudit(SSH_ROUTING_ERROR, "Failed to authenticate with the remote server.");
            return AssertionStatus.FAILED;
        }

        // process the command type
        final String fileName = ExpandVariables.process(assertion.getFileName(), variables, getAudit());
        final String directory = ExpandVariables.process(assertion.getDirectory(), variables, getAudit());
        try {
            if (assertion.isScpProtocol()) {
                //Process SCP commands
                final ScpClient scpClient = (ScpClient) sshClient;
                switch (commandType) {
                    case GET: {
                        final PipedInputStream pis = new PipedInputStream();
                        final PipedOutputStream pos = new PipedOutputStream(pis);

                        // Get the response byte limit
                        final long byteLimit = getByteLimit(variables);

                        // start download task. This will run in the background after this assertion completes until the entire file has been received.
                        startSshResponseTask(scpClient, new Functions.NullaryVoidThrows<IOException>() {
                            @Override
                            public void call() throws IOException {
                                try {
                                    scpClient.download(pos, directory, fileName);
                                } finally {
                                    //need to disconnect the client manually for GET requests.
                                    scpClient.disconnect();
                                }
                            }
                        }, pos);

                        //Create the response message
                        final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
                        response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.create(assertion.getDownloadContentType()), pis, byteLimit);
                        break;
                    }
                    case PUT: {
                        // Get the file length
                        long fileLength = getFileLength(variables);
                        if (fileLength == -1) {
                            //This will force the entire message to be read and stashed. It will also get the full file length
                            fileLength = mimeKnob.getContentLength();
                        }
                        // Upload the message. This will block until the entire file has been uploaded.
                        scpClient.upload(mimeKnob.getEntireMessageBodyAsInputStream(), directory, fileName, fileLength);
                        break;
                    }
                    default: {
                        logAndAudit(SSH_ROUTING_ERROR, "Unsupported SCP command type: " + commandType);
                        return AssertionStatus.BAD_REQUEST;
                    }
                }
            } else {
                //Process SFTP commands
                final SftpClient sftpClient = (SftpClient) sshClient;
                switch (commandType) {
                    case GET: {
                        final PipedInputStream pis = new PipedInputStream();
                        final PipedOutputStream pos = new PipedOutputStream(pis);

                        // Get the response byte limit
                        final long byteLimit = getByteLimit(variables);
                        // Get the file length
                        final long fileLength = getFileLength(variables);
                        // Get the file offset
                        final long fileOffset = getFileOffset(variables);

                        //Save the file size to a context variable.
                        if (assertion.isSetFileSizeToContextVariable()) {
                            SftpFile sftpFile = sftpClient.getFileAttributes(directory, fileName);
                            if (sftpFile != null) {
                                context.setVariable(assertion.getSaveFileSizeContextVariable(), sftpFile.getFilesize());
                            }
                        }

                        // Us this to wait till the input stream to the file is properly retrieved.
                        final CountDownLatch gotStream = new CountDownLatch(1);
                        //This will hold any exceptions that may have been thrown attempting to retrieve the input stream to the file
                        final AtomicReference<Exception> gettingStreamException = new AtomicReference<>(null);
                        // start download task. This will run in the background after this assertion completes until the entire file has been received.
                        startSshResponseTask(sftpClient, new Functions.NullaryVoidThrows<IOException>() {
                            @Override
                            public void call() throws IOException {
                                InputStream in;
                                try {
                                    in = sftpClient.getFileInputStream(directory, fileName, fileOffset);
                                } catch (Exception e) {
                                    //sate the exception thrown trying to retreive the file input stream
                                    gettingStreamException.set(e);
                                    throw e;
                                } finally {
                                    gotStream.countDown();
                                    sftpClient.disconnect();
                                }
                                try {
                                    //write the data from the file to the response output stream.
                                    if (fileLength == -1) {
                                        IOUtils.copyStream(in, pos);
                                    } else {
                                        IOUtils.copyStream(new TruncatingInputStream(in, fileLength), pos);
                                    }
                                } finally {
                                    //need to disconnect the client manually for GET requests.
                                    sftpClient.disconnect();
                                }
                            }
                        }, pos);
                        //wait the the input stream to the sftp file is retieved.
                        gotStream.await();
                        //check if there was an error retrieving the input stream.
                        Exception exceptionThrown = gettingStreamException.get();
                        if (exceptionThrown != null) {
                            logAndAudit(SSH_ROUTING_ERROR,
                                    new String[]{"Error opening file stream: " + getMessage(exceptionThrown)}, getDebugException(exceptionThrown));
                            return AssertionStatus.FAILED;
                        }
                        //Create the response message
                        final Message response = context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);
                        response.initialize(stashManagerFactory.createStashManager(), ContentTypeHeader.create(assertion.getDownloadContentType()), pis, fileLength == -1 ? byteLimit : Math.min(fileLength, byteLimit));
                        break;
                    }
                    case PUT: {
                        //get the request ssh knob. This is used to preserve file permissions.
                        final SshKnob sshKnob = request.getKnob(SshKnob.class);

                        // Get the file offset
                        final long fileOffset = getFileOffset(variables);

                        // Upload the message. This will block until the entire file has been uploaded.
                        sftpClient.upload(mimeKnob.getEntireMessageBodyAsInputStream(), directory, fileName,
                                assertion.isPreserveFileMetadata() && sshKnob != null ? sshKnob.getFileMetadata() : null,
                                fileOffset, assertion.isFailIfFileExists(), !assertion.isTruncateExistingFile());
                        break;
                    }
                    case LIST: {
                        final PipedInputStream pis = new PipedInputStream();
                        final PipedOutputStream pos = new PipedOutputStream(pis);

                        // start directory listing task.
                        Future<Void> listTask = startSshResponseTask(sftpClient, new Functions.NullaryVoidThrows<IOException>() {
                            @Override
                            public void call() throws IOException {
                                Enumeration<SftpFile> listing = sftpClient.listDirectory(directory, fileName);
                                List<XmlSshFile> fileList = new ArrayList<>();
                                while (listing.hasMoreElements()) {
                                    SftpFile file = listing.nextElement();
                                    fileList.add(new XmlSshFile(file.getFilename(), !file.isDirectory(), file.getFilesize(), file.getModificationTime()));
                                }
                                XmlVirtualFileList xmlFileList = new XmlVirtualFileList(fileList);
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
                            if (e.getCause() instanceof SftpException) {
                                return handleJscapeException(e, commandType, username, host, directory, fileName);
                            }
                            logAndAudit(SSH_ROUTING_ERROR, new String[]{"SSH2 Route Assertion error: Error getting directory listing: " + getMessage(e)}, getDebugException(e));
                            return AssertionStatus.FAILED;
                        }
                        break;
                    }
                    case STAT: {
                        final PipedInputStream pis = new PipedInputStream();
                        final PipedOutputStream pos = new PipedOutputStream(pis);

                        // start stat task.
                        Future<Void> statTask = startSshResponseTask(sftpClient, new Functions.NullaryVoidThrows<IOException>() {
                            @Override
                            public void call() throws IOException {
                                SftpFile sftpFile = sftpClient.getFileAttributes(directory, fileName);
                                XmlVirtualFileList xmlFileList = sftpFile != null ?
                                        new XmlVirtualFileList(Arrays.asList(new XmlSshFile(sftpFile.getFilename(), !sftpFile.isDirectory(), sftpFile.getFilesize(), sftpFile.getModificationTime()))) :
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
                            if (e.getCause() instanceof SftpException) {
                                return handleJscapeException(e, commandType, username, host, directory, fileName);
                            }
                            logAndAudit(SSH_ROUTING_ERROR, new String[]{"SSH2 Route Assertion error: Error getting file attributes: " + getMessage(e)}, getDebugException(e));
                            return AssertionStatus.FAILED;
                        }
                        break;
                    }
                    case DELETE: {
                        sftpClient.deleteFile(directory, fileName);
                        break;
                    }
                    case MOVE: {
                        final String newFileName = ExpandVariables.process(assertion.getNewFileName(), variables, getAudit());
                        sftpClient.renameFile(directory, fileName, newFileName);
                        break;
                    }
                    case MKDIR: {
                        sftpClient.createDirectory(directory, fileName);
                        break;
                    }
                    case RMDIR: {
                        sftpClient.removeDirectory(directory, fileName);
                        break;
                    }
                    default: {
                        //This shouldn't ever happen
                        logAndAudit(SSH_ROUTING_ERROR, "Unsupported SFTP command type: " + commandType);
                        return AssertionStatus.BAD_REQUEST;
                    }
                }
            }
        } catch (ScpException | SftpException e) {
            return handleJscapeException(e, commandType, username, host, directory, fileName);
        } catch (IOException e) {
            if (getMessage(e).contains("No such file or directory")) {
                return handleJscapeException(e, commandType, username, host, directory, fileName);
            } else {
                logAndAudit(SSH_ROUTING_ERROR,
                        new String[]{SSH_IO_EXCEPTION + " '" + getMessage(e) + "', server: " + host}, getDebugException(e));
            }
        } catch (ThreadPoolShutDownException | InterruptedException | NoSuchVariableException e) {
            logAndAudit(SSH_ROUTING_ERROR, new String[]{"SSH2 Route Assertion error: " + getMessage(e)}, getDebugException(e));
        } catch (NoSuchPartException e) {
            logAndAudit(SSH_ROUTING_ERROR,
                    new String[]{SSH_NO_SUCH_PART_ERROR + ", server: " + host}, getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (Throwable t) {
            logAndAudit(SSH_ROUTING_ERROR,
                    new String[]{getMessage(t)}, getDebugException(t));
            return AssertionStatus.FAILED;
        } finally {
            if (sshClient != null) {
                //on all commands except GET commands the ssh client needs to be disconnected.
                //On GET commands the client should be left connected this way data can still be streamed to the client once policy finished processing.
                //It will be disconnected automatically after the data is sent.
                if (!CommandKnob.CommandType.GET.equals(commandType)) {
                    sshClient.disconnect();
                }
            }
        }

        context.setRoutingStatus(RoutingStatus.ROUTED);
        return AssertionStatus.NONE;
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
     * This will attempts to nicely report jscape exceptions
     */
    private AssertionStatus handleJscapeException(final Exception e, final CommandKnob.CommandType commandType, final String username,
                                                  final String host, final String directory, String filename) {
        if (getMessage(e).contains("No such file or directory") || getMessage(e).contains("File or directory No such file not found")) {
            logAndAudit(SSH_ROUTING_ERROR,
                    new String[]{SSH_DIR_FILE_DOESNT_EXIST_ERROR + " command type: " + commandType + ", directory: " + directory + ", file: " + filename + ", username: " + username},
                    getDebugException(e));
        } else {
            logAndAudit(SSH_ROUTING_ERROR,
                    new String[]{SSH_EXCEPTION_ERROR + " '" + getMessage(e) + "', server: " + host}, getDebugException(e));
        }
        return AssertionStatus.FAILED;
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
                                              final Functions.NullaryVoidThrows<IOException> processingFunction,
                                              final PipedOutputStream pos) throws IOException, ThreadPoolShutDownException {
        final CountDownLatch startedSignal = new CountDownLatch(1);

        final Future<Void> future = threadPool.submitTask(new Callable<Void>() {
            @Override
            public Void call() throws IOException {
                try {
                    startedSignal.countDown();
                    processingFunction.call();
                } finally {
                    ResourceUtils.closeQuietly(pos);
                    startedSignal.countDown();
                    sshClient.disconnect();
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
}

