package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.CommandKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.util.EventChannel;
import com.l7tech.server.util.SoapFaultManager;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ThreadPool.ThreadPoolShutDownException;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.SshFile;
import org.apache.sshd.server.sftp.SftpSubsystem;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXB;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Message processing SFTP subsystem (heavily borrowed from org.apache.sshd.server.sftp.SftpSubsystem).
 * https://wiki.l7tech.com/mediawiki/index.php/SSH_Listener_Add_Support_for_Additional_Keywords
 */
class MessageProcessingSftpSubsystem extends SftpSubsystem {
    private static final Logger logger = Logger.getLogger(MessageProcessingSftpSubsystem.class.getName());

    public static final long DEFAULT_MAX_MESSAGE_PROCESSING_WAIT_TIME = 60L;
    public static final int DEFAULT_MAX_READ_BUFFER_SIZE = 1024 * 32;
    public static final int DEFAULT_MAX_READ_TIME = 10 * 1000;
    public static final int DEFAULT_MAX_WRITE_TIME = 10 * 1000;

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

    //Need an executerService to put timeouts around reading and writing to the file streams.
    private ExecutorService readWriteExecutorService = Executors.newCachedThreadPool();


    /**
     * One MessageProcessingSftpSubsystem is created for every connection. Every sftp client has its own instance of an MessageProcessingSftpSubsystem
     */
    MessageProcessingSftpSubsystem(final SsgConnector connector) {
        this.connector = connector;
    }

    /**
     * This is borrowed from {@link SftpSubsystem}. It processes the different types of sftp commands available.
     *
     * @param buffer This is the inbound data buffer from the sftp client containing the command and data to execute.
     * @throws IOException This is only thrown if there was an error attempting to send a message to the client
     */
    @Override
    protected void process(final Buffer buffer) throws IOException {
        final int length = buffer.getInt();
        final int type = (int) buffer.getByte();
        final int id = buffer.getInt();

        // customize support for selected SFTP functions on the Gateway
        switch (type) {
            // Same as SftpSubsystem only version 3 of the protocol is supported
            case SSH_FXP_INIT: {
                if (length != 5) {
                    throw new IllegalArgumentException();
                }
                version = id;
                //only version 3 of the protocol is supported.
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
                //From Docs: Files are opened and created using the SSH_FXP_OPEN message. In our implementation files will never be created with an fxp open command. They will be created when they are first written to.
                sshFxpOpen(buffer, id);
                break;
            }
            case SSH_FXP_CLOSE: {
                //From Docs: A file is closed by using the SSH_FXP_CLOSE request
                sshFxpClose(buffer, id);
                break;
            }
            case SSH_FXP_READ: {
                //From Docs: Once a file has been opened, it can be read using the SSH_FXP_READ message
                sshFxpRead(buffer, id);
                break;
            }
            case SSH_FXP_WRITE: {
                //From Docs: Writing to a file is achieved using the SSH_FXP_WRITE message
                sshFxpWrite(buffer, id);
                break;
            }
            case SSH_FXP_LSTAT:
            case SSH_FXP_STAT: {
                /*
                From Docs:
                    Very often, file attributes are automatically returned by
                   SSH_FXP_READDIR.  However, sometimes there is need to specifically
                   retrieve the attributes for a named file.  This can be done using the
                   SSH_FXP_STAT, SSH_FXP_LSTAT and SSH_FXP_FSTAT requests.

                    SSH_FXP_STAT and SSH_FXP_LSTAT only differ in that SSH_FXP_STAT
                   follows symbolic links on the server, whereas SSH_FXP_LSTAT does not
                   follow symbolic links.
                 */
                // We ignore symbolic links and treat both commands in the same way.
                sshFxpStat(buffer, id);
                break;
            }
            case SSH_FXP_FSTAT: {
                //From Docs: SSH_FXP_FSTAT differs from the others in that it returns status information for an open file (identified by the file handle).
                String handleID = buffer.getString();
                Handle handle = handles.get(handleID);
                if (handle == null) {
                    logger.log(Level.INFO, "Error retrieving file attributes: handle not found.");
                    sendStatus(id, SSH_FX_INVALID_HANDLE, handleID);
                } else {
                    sendAttrs(id, handle.getFile());
                }
                break;
            }
            case SSH_FXP_OPENDIR: {
                /*
                From Docs:
                    The files in a directory can be listed using the SSH_FXP_OPENDIR and
                   SSH_FXP_READDIR requests.  Each SSH_FXP_READDIR request returns one
                   or more file names with full file attributes for each file.  The
                   client should call SSH_FXP_READDIR repeatedly until it has found the
                   file it is looking for or until the server responds with a
                   SSH_FXP_STATUS message indicating an error (normally SSH_FX_EOF if
                   there are no more files in the directory).  The client should then
                   close the handle using the SSH_FXP_CLOSE request.
                 */
                sshFxpOpenDir(buffer, id);
                break;
            }
            case SSH_FXP_READDIR: {
                sshFxpReadDir(buffer, id);
                break;
            }
            // Same as SftpSubsystem.
            // From Docs: The SSH_FXP_REALPATH request can be used to have the server
            //      canonicalize any given path name to an absolute path.  This is useful
            //      for converting path names containing ".." components or relative
            //      pathnames without a leading slash into absolute paths.
            case SSH_FXP_REALPATH: {
                String path = buffer.getString();
                if (path.trim().length() == 0) {
                    path = ".";
                }
                SshFile sshFile = resolveFile(path);
                sendPath(id, sshFile);
                break;
            }
            // Same as SftpSubsystem
            // These are supposed to be used to set the file stats for a file. Stats include file ownership and permissions.
            // This is not supported by the sftp subsystem. We don't support this either but in order to have some sftp clients (winscp) work we blindly return ok here.
            case SSH_FXP_SETSTAT:
            case SSH_FXP_FSETSTAT: {
                sendStatus(id, SSH_FX_OK, "");
                break;
            }
            //This is used to remove a file from. This should not be used to try and remove directories
            case SSH_FXP_REMOVE: {
                if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_DELETE)) {
                    logger.log(Level.INFO, "An sftp client attempted to remove a file but this is not enabled in this connector.");
                    sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support removing files.");
                    break;
                }
                // Given file name
                // Respond with SSH_FXP_STATUS
                String path = buffer.getString();
                VirtualSshFile sshFile;
                try {
                    sshFile = removeFile(path);
                } catch (MessageProcessingException e) {
                    logger.log(Level.WARNING, "There was an error attempting to process an SFTP Delete command.");
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                    break;
                }
                if (respondFromErroredMessageProcessingStatus(id, sshFile)) {
                    sendStatus(id, SSH_FX_OK, "");
                }
                break;
            }
            //From Docs: New directories can be created using the SSH_FXP_MKDIR request.
            case SSH_FXP_MKDIR: {
                if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MKDIR)) {
                    logger.log(Level.INFO, "An sftp client attempted to create a directory but this is not enabled in this connector.");
                    sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support creating directories.");
                    break;
                }
                // Given path, attrs
                // Respond with SSH_FXP_STATUS
                String path = buffer.getString();
                VirtualSshFile sshFile = (VirtualSshFile) resolveFile(path);
                try {
                    processCommand(sshFile, CommandKnob.CommandType.MKDIR, Collections.<String, String>emptyMap());
                } catch (MessageProcessingException e) {
                    logger.log(Level.WARNING, "There was an error attempting to process an SFTP MKDIR command.");
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                    break;
                } finally {
                    sshFile.handleClose();
                }
                if (respondFromErroredMessageProcessingStatus(id, sshFile)) {
                    sendStatus(id, SSH_FX_OK, "");
                }
                break;
            }
            // From Docs: Directories can be removed using the SSH_FXP_RMDIR request
            case SSH_FXP_RMDIR: {
                if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_RMDIR)) {
                    logger.log(Level.INFO, "An sftp client attempted to remove a directory but this is not enabled in this connector.");
                    sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support removing directories.");
                    break;
                }
                // Given path,
                // Respond with SSH_FXP_STATUS
                String path = buffer.getString();
                VirtualSshFile sshFile = (VirtualSshFile) resolveFile(path);
                try {
                    processCommand(sshFile, CommandKnob.CommandType.RMDIR, Collections.<String, String>emptyMap());
                } catch (MessageProcessingException e) {
                    logger.log(Level.WARNING, "There was an error attempting to process an SFTP RMDIR command.");
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                    break;
                } finally {
                    sshFile.handleClose();
                }
                if (respondFromErroredMessageProcessingStatus(id, sshFile)) {
                    sendStatus(id, SSH_FX_OK, "");
                }
                break;
            }
            /*
            From Docs:
                Files (and directories) can be renamed using the SSH_FXP_RENAME message.
             */
            case SSH_FXP_RENAME: {
                if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_MOVE)) {
                    logger.log(Level.INFO, "An sftp client attempted to rename a file but this is not enabled in this connector.");
                    sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support renaming files.");
                    break;
                }
                // Given old path, new path
                // Respond with SSH_FXP_STATUS
                String path = buffer.getString();
                String newPath = buffer.getString();
                VirtualSshFile sshFile = (VirtualSshFile) resolveFile(path);
                VirtualSshFile newSshFile = (VirtualSshFile) resolveFile(newPath);

                Map<String, String> parameters = CollectionUtils.MapBuilder.<String, String>builder().
                        put("newPath", newSshFile.getPath()).
                        put("newFile", newSshFile.getName()).unmodifiableMap();
                try {
                    processCommand(sshFile, CommandKnob.CommandType.MOVE, parameters);
                } catch (MessageProcessingException e) {
                    logger.log(Level.WARNING, "There was an error attempting to process an SFTP MOVE command.");
                    sendStatus(id, SSH_FX_FAILURE, e.getMessage());
                    break;
                } finally {
                    sshFile.handleClose();
                }
                if (respondFromErroredMessageProcessingStatus(id, sshFile)) {
                    sendStatus(id, SSH_FX_OK, "");
                }
                break;
            }
            default:
                logger.log(Level.INFO, "Received unsupported type: {0}", type);
                sendStatus(id, SSH_FX_OP_UNSUPPORTED, "Command " + type + " is unsupported on the Gateway");
                break;
        }
    }


    /**
     * This will open a file for processing. A handle will be added to the file handle list.
     * The response to this message will be either SSH_FXP_HANDLE (if the
     * operation is successful) or SSH_FXP_STATUS (if the operation fails).
     *
     * @throws IOException This is thrown if there is an exception sending messages to the sftp client.
     */
    private void sshFxpOpen(final Buffer buffer, final int id) throws IOException {
        final int maxHandleCount;
        if ((maxHandleCount = session.getIntProperty(MAX_OPEN_HANDLES_PER_SESSION, 0)) > 0) {
            if (handles.size() >= maxHandleCount) {
                logger.log(Level.INFO, "Cannot open a new file connection, too many open handles: {0}", handles.size());
                sendStatus(id, SSH_FX_FAILURE, "Too many open handles");
                return;
            }
        }

        //The path of the file
        final String path = buffer.getString();
        //The file flags. These are used to tell the server what to do with the file.
        final int flags = buffer.getInt();
        //The file attributes.
        final int attrs = buffer.getInt();

        //appending is only enabled if PUT is enabled and partial uploads are enabled.
        if ((flags & SSH_FXF_APPEND) != 0 &&
                !(connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT) &&
                        connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS))) {
            logger.log(Level.INFO, "A client attempted to open a file for appending but SFTP put and SFTP partial uploads are not enabled");
            sendStatus(id, SSH_FX_FAILURE, "Appending to the end of a file is not supported by this sftp server.");
            return;
        }

        final VirtualSshFile file;
        //If SFTP Stat or LIST commands are enabled 'real' file properties will need to be returned, so policy is called to list or stat to get the file properties. Otherwise dummby file stats are returned.
        try {
            file = getFileProperties(path);
        } catch (MessageProcessingException e) {
            logger.log(Level.WARNING, "There was an error attempting to get file statistics when opening the file: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            sendStatus(id, SSH_FX_FAILURE, "Error attempting to open file: " + e.getMessage());
            return;
        }

        //cannot open directories
        if (file.isDirectory()) {
            sendStatus(id, SSH_FX_NO_SUCH_FILE, path);
            return;
        }

        if (file.doesExist()) {
            // according to the SFTP documentation this error should be returned if the file exists and both the SSH_FXF_CREAT and SSH_FXF_EXCL flags are specified.
            if (((flags & SSH_FXF_CREAT) != 0) && ((flags & SSH_FXF_EXCL) != 0)) {
                sendStatus(id, SSH_FX_FILE_ALREADY_EXISTS, path);
                return;
            }
        } else {
            // If the file doesn't exist and create flag is set copy over the specified file attributes. If the file is sent using the SshRouteAssertion then the attributes will be preserved.
            // This is existing legacy workflow. It may need to be rethought. It works by using the sshKnob and is only applicable if the SshRouteAssertion is used
            if (((flags & SSH_FXF_CREAT) != 0)) {
                //attrs copy over file attributes
                getFileAttributes(attrs, file, buffer);
                //does nothing
                file.create();
            }
        }
        if ((flags & SSH_FXF_TRUNC) != 0) {
            if(file.doesExist() &&
                    connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_DELETE) &&
                    connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_DELETE_FILE_ON_TRUNCATE_REQUEST) ) {
                VirtualSshFile removedFile;
                try {
                    removedFile = removeFile(path);
                } catch (MessageProcessingException e) {
                    logger.log(Level.WARNING, "There was an error attempting to truncate file: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    sendStatus(id, SSH_FX_FAILURE, "Error attempting to truncate file: " + e.getMessage());
                    return;
                }
                //Get the message processing status and validate that if completed successfully.
                AssertionStatus status = removedFile.getMessageProcessingStatus().getMessageProcessStatus();
                if (status != AssertionStatus.NONE) {
                    logger.log(Level.WARNING, "There was an error attempting to truncate file: Message processing failed: " + ((status == null) ? "null status" : status.getMessage()));
                    sendStatus(id, SSH_FX_FAILURE, "Error attempting to truncate file: " + ((status == null) ? "null status" : status.getMessage()));
                    return;
                }
            }
            //does nothing
            file.truncate();
        }

        final String handle = UUID.randomUUID().toString();

        handles.put(handle, new FileHandle(file, flags)); // handle flags conversion
        sendHandle(id, handle);
    }

    /**
     * Closes an open file handle
     *
     * @throws IOException This is only thrown if there was an error attempting to send a message to the client
     */
    private void sshFxpClose(final Buffer buffer, final int id) throws IOException {
        final String handleId = buffer.getString();
        final Handle handle = handles.get(handleId);
        if (handle == null) {
            logger.log(Level.INFO, "A client attempted to close a non existing file handle.");
            sendStatus(id, SSH_FX_INVALID_HANDLE, handleId, "");
        } else {
            handles.remove(handleId);
            //This statement should never throw an IOException. This is because all files are VirtualSshFile and don't throw IOExceptions on close.
            handle.close();

            //If this was a file handle look at the assertion status returned by the message processor to see if it was successful
            if (!(handle instanceof FileHandle) ||
                    !((VirtualSshFile) handle.getFile()).getMessageProcessingStatus().isProcessingStarted() ||
                    respondFromErroredMessageProcessingStatus(id, (VirtualSshFile) handle.getFile())) {
                // This is sent if the handle is an instance of a DirectoryHandle OR Policy processing has not yet started OR policy processing was successful. If policy processing was unsuccessful then a response will be sent from: respondFromErroredMessageProcessingStatus
                sendStatus(id, SSH_FX_OK, "", "");
            }
        }
    }


    /**
     * This is used to send file data back to the client. Message processing will be called to get the file stream.
     * If Partial downloads are enabled Policy processing is called once for every read request.
     * If they are disabled policy processing is called only once for the first read request. Subsequent read requests read from the opened stream.
     *
     * @throws IOException This is only thrown if there was an error attempting to send a message to the client
     */
    private void sshFxpRead(final Buffer buffer, final int id) throws IOException {
        // Validate that retrieving files is enabled on this connector.
        if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET)) {
            logger.log(Level.INFO, "An sftp client attempted to read a file but this is not enabled in this connector.");
            sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support downloading files.");
            return;
        }
        // get the request properties.
        final String handleId = buffer.getString();
        final long requestedOffset = buffer.getLong();
        final int requestedLength = buffer.getInt();
        final Handle fileHandle = handles.get(handleId);
        // validate the the handle is not null and that it is a file handle
        if (!(fileHandle instanceof FileHandle)) {
            sendStatus(id, SSH_FX_INVALID_HANDLE, handleId);
        } else {
            // The ssh file returned will always be a VirtualSshFile
            final VirtualSshFile sshFile = (VirtualSshFile) fileHandle.getFile();
            logger.log(Level.FINE, "Read request: Offset: {0} Length: {1} File: {2}", new Object[]{requestedOffset, requestedLength, sshFile.getAbsolutePath()});

            //Retrieve the max buffer size. It is settable as an advanced property on this connector.
            final int maxReadBufferSize = connector.getIntProperty(SshServerModule.LISTEN_PROP_SFTP_MAX_READ_BUFFER_SIZE, DEFAULT_MAX_READ_BUFFER_SIZE);
            try {
                //if partial downloads are enabled need to call policy processing for each read request.
                if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS)) {
                    //add the length and offset parameters.
                    Map<String, String> parameters = CollectionUtils.MapBuilder.<String, String>builder().
                            put("length", String.valueOf(Math.min(requestedLength, maxReadBufferSize))).
                            put("offset", String.valueOf(requestedOffset)).unmodifiableMap();
                    processCommand(sshFile, CommandKnob.CommandType.GET, parameters);
                } else {
                    //If the message processing has not been started yet, then start message processing.
                    if (!sshFile.getMessageProcessingStatus().isProcessingStarted()) {
                        //add the length and offset parameters.
                        Map<String, String> parameters = CollectionUtils.MapBuilder.<String, String>builder().
                                put("length", sshFile.getSize() > 0 ? String.valueOf(sshFile.getSize()) : "-1").
                                put("offset", "0").unmodifiableMap();
                        processCommand(sshFile, CommandKnob.CommandType.GET, parameters);
                    }
                }
            } catch (MessageProcessingException e) {
                logger.log(Level.WARNING, "There was an error attempting to process an SFTP GET command.");
                sshFile.handleClose();
                sendStatus(id, SSH_FX_FAILURE, "Error processing GET command in policy: " + ExceptionUtils.getMessage(e));
                return;
            }

            //check for message processing errors here. This will respond to the client it there were message processing errors.
            if (!respondFromErroredMessageProcessingStatus(id, sshFile)) {
                sshFile.handleClose();
                return;
            }

            // read data from the response stream set on the file after processing the policy
            final byte[] readBytes = new byte[Math.min(requestedLength, maxReadBufferSize)];
            final int lengthRead;
            final InputStream fileInputStream;
            try {
                fileInputStream = sshFile.getInputStream();
            } catch (IOException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Exception attempting to read from the input stream while processing a SFTP GET request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing GET command, could not read from the input stream: " + ExceptionUtils.getMessage(e));
                return;
            }
            Callable<Integer> readTask = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return readFromStream(fileInputStream, readBytes, 0, readBytes.length);
                }
            };

            // Read the data from the stream in a separate thread so that a maximum read time can be set.
            final int maxReadTime = connector.getIntProperty(SshServerModule.LISTEN_PROP_SFTP_MAX_READ_TIME_MILLIS, DEFAULT_MAX_READ_TIME);
            Future<Integer> future = readWriteExecutorService.submit(readTask);
            try {
                lengthRead = future.get(maxReadTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Interrupted attempting to read to the input stream while processing a SFTP GET request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing GET command, could not read from the input stream: " + ExceptionUtils.getMessage(e));
                return;
            } catch (ExecutionException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Exception attempting to read to the input stream while processing a SFTP GET request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing GET command, could not read from the input stream: " + ExceptionUtils.getMessage(e));
                return;
            } catch (TimeoutException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Timed out attempting to read to the input stream while processing a SFTP GET request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing GET command, could not read from the input stream: " + ExceptionUtils.getMessage(e));
                return;
            }

            logger.log(Level.FINE, "Read: Length read from policy: " + lengthRead);
            if (lengthRead >= 0) {
                //When partial downloads are not enabled validate that the requested offset is an expected offset.
                if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS) &&
                        requestedOffset != sshFile.getNextExpectedOffset()) {
                    logger.log(Level.WARNING, "Bad offset given, expected: {0}. This sftp server only supports reading files in order." + ((maxReadBufferSize < requestedLength) ? " This may be caused because the requested length is greater then the max read buffer size." : ""), sshFile.getNextExpectedOffset());
                    sendStatus(id, SSH_FX_FAILURE, "Bad offset given, expected: " + sshFile.getNextExpectedOffset() + ". This sftp server only supports reading files in order.");
                    return;
                }
                // Send the response back to the client
                Buffer buf = new Buffer(lengthRead + 5);
                buf.putByte((byte) SSH_FXP_DATA);
                buf.putInt(id);
                buf.putBytes(readBytes, 0, lengthRead);
                send(buf);
                sshFile.setNextExpectedOffset(requestedOffset + lengthRead);
            } else {
                sendStatus(id, SSH_FX_EOF, "");
            }

            //If partial uploads are enabled need to close the ssh file.
            if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_DOWNLOADS)) {
                sshFile.handleClose();
                sshFile.reset();
            }
        }
    }

    /**
     * This is used to send file data through the ssg. Message processing will be called to get open a stream to the processor.
     * If Partial uploads are enabled Policy processing is called once for every write request.
     * If they are disabled policy processing is called only once for the first write request. Subsequent write requests write to the opened stream.
     *
     * @throws IOException This is only thrown if there was an error attempting to send a message to the client
     */
    private void sshFxpWrite(final Buffer buffer, final int id) throws IOException {
        // Validate that uploading files is enabled on this connector.
        if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT)) {
            logger.log(Level.INFO, "An sftp client attempted to write a file but this is not enabled in this connector.");
            sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support uploading files.");
            return;
        }
        // get the request properties.
        final String handleId = buffer.getString();
        long requestedOffset = buffer.getLong();
        // The data to write to the file stream.
        final byte[] data = buffer.getBytes();
        final Handle fileHandle = handles.get(handleId);
        // validate the the handle is not null and that it is a file handle
        if (!(fileHandle instanceof FileHandle)) {
            sendStatus(id, SSH_FX_INVALID_HANDLE, handleId);
        } else {
            // The ssh file returned will always be a VirtualSshFile
            final VirtualSshFile sshFile = (VirtualSshFile) fileHandle.getFile();
            logger.log(Level.FINE, "Write request: Offset: {0} Length: {1} File: {2}", new Object[]{requestedOffset, data.length, sshFile.getAbsolutePath()});
            // When partial uploads are not enabled validate that the requested offset is an expected offset.
            if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS) &&
                    requestedOffset != sshFile.getNextExpectedOffset()) {
                logger.log(Level.WARNING, "Bad offset given, expected: {0}. This sftp server only supports writing files in order.", sshFile.getNextExpectedOffset());
                sendStatus(id, SSH_FX_FAILURE, "Bad offset given, expected: " + sshFile.getNextExpectedOffset() + ". This sftp server only supports writing files in order.");
                return;
            }

            try {
                //if partial uploads are enabled need to call policy processing for each write request.
                if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS)) {
                    //add the length and offset parameters.
                    Map<String, String> parameters = CollectionUtils.MapBuilder.<String, String>builder().
                            put("length", String.valueOf(data.length)).
                            put("offset", String.valueOf(requestedOffset)).unmodifiableMap();
                    submitMessageProcessingTask(sshFile, CommandKnob.CommandType.PUT, parameters);
                } else {
                    //If the message processing has not been started yet, then start message processing.
                    if (!sshFile.getMessageProcessingStatus().isProcessingStarted()) {
                        //add the length and offset parameters.
                        Map<String, String> parameters = CollectionUtils.MapBuilder.<String, String>builder().
                                put("length", sshFile.getSize() > 0 ? String.valueOf(sshFile.getSize()) : "-1").
                                put("offset", "0").unmodifiableMap();
                        submitMessageProcessingTask(sshFile, CommandKnob.CommandType.PUT, parameters);
                    }
                }
            } catch (MessageProcessingException e) {
                logger.log(Level.WARNING, "There was an error attempting to process an SFTP PUT command.");
                sshFile.handleClose();
                sendStatus(id, SSH_FX_FAILURE, "Error processing PUT command in policy: " + ExceptionUtils.getMessage(e));
                return;
            }

            // write data to the request output stream set on the file
            final OutputStream fileOutputStream = sshFile.getOutputStream();
            Callable<Void> writeTask = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    fileOutputStream.write(data, 0, data.length);
                    return null;
                }
            };

            // Write the data to the stream in a separate thread so that a maximum write time can be set.
            final int maxWriteTime = connector.getIntProperty(SshServerModule.LISTEN_PROP_SFTP_MAX_WRITE_TIME_MILLIS, DEFAULT_MAX_WRITE_TIME);
            Future<Void> future = readWriteExecutorService.submit(writeTask);
            try {
                future.get(maxWriteTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Interrupted attempting to write to the output stream while processing a SFTP PUT request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing PUT command, could not write to the output stream: " + ExceptionUtils.getMessage(e));
                return;
            } catch (ExecutionException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Exception attempting to write to the output stream while processing a SFTP PUT request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing PUT command, could not write to the output stream: " + ExceptionUtils.getMessage(e));
                return;
            } catch (TimeoutException e) {
                sshFile.handleClose();
                logger.log(Level.WARNING, "Timed out attempting to write to the output stream while processing a SFTP PUT request: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                sendStatus(id, SSH_FX_FAILURE, "Error processing PUT command, could not write to the output stream: " + ExceptionUtils.getMessage(e));
                return;
            }

            //If partial downloads are enabled need to close the ssh file.
            if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PARTIAL_UPLOADS)) {
                sshFile.handleClose();
                //check for message processing errors here. This will respond to the client it there were message processing errors.
                //This will wait for message processing to finish before responding.
                if (!respondFromErroredMessageProcessingStatus(id, sshFile)) {
                    return;
                }
                sshFile.reset();
            } else {
                sshFile.setNextExpectedOffset(requestedOffset + data.length);
            }
            sendStatus(id, SSH_FX_OK, "");
        }
    }

    /**
     * Returns the file attribute based on the given path (not an open handle).
     * Policy processing may occur here if LIST or STAT commands are enabled. Otherwise dummy stats will be returned.
     *
     * @param buffer The client buffer input stream
     * @throws IOException This is thrown if there was an error sending a response to the sftp client
     */
    private void sshFxpStat(final Buffer buffer, final int id) throws IOException {
        // The file path of the file to get attributes for.
        final String path = buffer.getString();
        final VirtualSshFile file;
        try {
            //retrieve the file properties.
            file = getFileProperties(path);
        } catch (MessageProcessingException e) {
            logger.log(Level.WARNING, "There was an error attempting to process an SFTP LIST or STAT command.");
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
            return;
        }
        if (file.doesExist()) {
            sendAttrs(id, file);
        } else {
            sendStatus(id, SSH_FX_NO_SUCH_FILE, "File Not Found: " + path);
        }
    }


    /**
     * Processes the open directory command. This will call the message processor to retrieve the list of files that the directory contains.
     *
     * @throws IOException This is thrown if there was an error sending a response to the sftp client
     */
    private void sshFxpOpenDir(Buffer buffer, int id) throws IOException {
        if (!connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST)) {
            logger.log(Level.INFO, "An sftp client attempted to open a directory but this is not enabled in this connector.");
            sendStatus(id, SSH_FX_FAILURE, "This SFTP server does not support scanning directories.");
            return;
        }
        //The path of the directory to read.
        final String path = buffer.getString();
        //Creates the directory virtual file.
        final VirtualSshFile directory = (VirtualSshFile) resolveFile(path);
        directory.setFile(false);

        //get the file list for the directory
        final XmlVirtualFileList xmlVirtualFileList;
        try {
            xmlVirtualFileList = statOrListFile(directory, CommandKnob.CommandType.LIST);
        } catch (MessageProcessingException e) {
            sendStatus(id, SSH_FX_FAILURE, e.getMessage());
            return;
        }

        final List<SshFile> files;
        if (xmlVirtualFileList == null) {
            sendStatus(id, SSH_FX_FAILURE, "Error directory listing for " + directory.getAbsolutePath());
        } else {
            //if xmlVirtualFileList is not null that means that the message was successfully processed. If it wasn't successfully processed a response was already sent.
            if (xmlVirtualFileList.getFileList() != null) {
                //need to get the path of the directory file so that the full path children of the directory can be returned.
                String currentPath = directory.getAbsolutePath();
                if (!currentPath.endsWith("/")) {
                    currentPath += "/";
                }
                //convert the xml file list to virtualSshFile list
                files = new ArrayList<>(xmlVirtualFileList.getFileList().size());
                for (XmlSshFile parsedFile : xmlVirtualFileList.getFileList()) {
                    VirtualSshFile virtualSshFile = new VirtualSshFile(currentPath + parsedFile.getName(), parsedFile.isFile());
                    if (parsedFile.getSize() != null) virtualSshFile.setSize(parsedFile.getSize());
                    if (parsedFile.getLastModified() != null)
                        virtualSshFile.setLastModified(parsedFile.getLastModified());
                    files.add(virtualSshFile);
                }
            } else {
                files = Collections.emptyList();
            }
            directory.setSshFiles(files);
            String handle = UUID.randomUUID().toString();
            handles.put(handle, new DirectoryHandle(directory));
            sendHandle(id, handle);
        }
    }


    /**
     * This will return a list of files in a directory. The list of files should have already been processed when the open dir command was called.
     * Note the majority of this method was copied from SftpSubsystem
     *
     * @param buffer The client buffer input stream
     * @throws IOException This is thrown if there was an error sending a response to the sftp client
     */
    private void sshFxpReadDir(final Buffer buffer, final int id) throws IOException {
        final String handleId = buffer.getString();
        final Handle handle = handles.get(handleId);
        if (!(handle instanceof DirectoryHandle)) {
            sendStatus(id, SSH_FX_INVALID_HANDLE, handleId);
        } else if (((DirectoryHandle) handle).isDone()) {
            sendStatus(id, SSH_FX_EOF, "", "");
        } else if (!handle.getFile().doesExist()) {
            sendStatus(id, SSH_FX_NO_SUCH_FILE, handle.getFile().getAbsolutePath());
        } else if (!handle.getFile().isDirectory()) {
            sendStatus(id, SSH_FX_NOT_A_DIRECTORY, handle.getFile().getAbsolutePath());
        } else if (!handle.getFile().isReadable()) {
            sendStatus(id, SSH_FX_PERMISSION_DENIED, handle.getFile().getAbsolutePath());
        } else {
            DirectoryHandle directoryHandle = (DirectoryHandle) handle;
            if (directoryHandle.hasNext()) {
                // There is at least one file in the directory.
                // Send only a few files at a time to not create packets of a too
                // large size or have a timeout to occur.
                sendName(id, directoryHandle);
                if (!directoryHandle.hasNext()) {
                    // if no more files to send
                    directoryHandle.setDone(true);
                    directoryHandle.clearFileList();
                }
            } else {
                // empty directory
                directoryHandle.setDone(true);
                directoryHandle.clearFileList();
                sendStatus(id, SSH_FX_EOF, "", "");
            }
        }
    }


    /**
     * Finds the given file in the XmlVirtualFileList. A new instance of a virtual file is returned. If the file is not found the File has its exists property set to false.
     *
     * @param file               The file to find
     * @param xmlVirtualFileList The xml file list.
     * @return A new instance of a virtual ssh file. This will never be null. If the file is not found in the list it will have its exists property set to false.
     */
    private VirtualSshFile findFileInList(final VirtualSshFile file, XmlVirtualFileList xmlVirtualFileList) {
        //if xmlVirtualFileList is not null that means that the message was successfully processed. If it wasn't successfully processed a response was already sent.
        if (xmlVirtualFileList.getFileList() != null) {
            for (XmlSshFile parsedFile : xmlVirtualFileList.getFileList()) {
                if (file.getName().equals(parsedFile.getName())) {
                    VirtualSshFile returnedVirtualFile = new VirtualSshFile(file.getAbsolutePath(), parsedFile.isFile());
                    if (parsedFile.getSize() != null) returnedVirtualFile.setSize(parsedFile.getSize());
                    if (parsedFile.getLastModified() != null)
                        returnedVirtualFile.setLastModified(parsedFile.getLastModified());
                    return returnedVirtualFile;
                }
            }
        }

        //If this point gets reached that means the file has not been found.
        VirtualSshFile returnedVirtualFile = new VirtualSshFile(file.getAbsolutePath(), file.isFile());
        returnedVirtualFile.setExists(false);
        return returnedVirtualFile;
    }

    /**
     * Copy over file attributes from the ones given to the virtual file.
     */
    private void getFileAttributes(final int attrs, final VirtualSshFile file, final Buffer buffer) {
        boolean preserve = false;
        long size;
        String perm = null;

        if ((attrs & SSH_FILEXFER_ATTR_SIZE) == SSH_FILEXFER_ATTR_SIZE) {
            size = buffer.getLong();
            file.setSize(size);
        }
        //exist regardless if -p is present or not
        if ((attrs & SSH_FILEXFER_ATTR_PERMISSIONS) == SSH_FILEXFER_ATTR_PERMISSIONS) {
            perm = Integer.toOctalString(buffer.getInt());
        }
        //these are not available if the -p is not set
        if ((attrs & SSH_FILEXFER_ATTR_ACMODTIME) == SSH_FILEXFER_ATTR_ACMODTIME) {
            final long accessTime = buffer.getInt() * 1000L;
            file.setAccessTime(accessTime);
            final long modificationTime = buffer.getInt() * 1000L;
            file.setLastModified(modificationTime);
            preserve = true;
        }
        //exist regardless if -p is present or not
        if (preserve && perm != null) {
            file.setPermission(Integer.valueOf(perm));
        }
    }

    /**
     * Read bytes from an input stream. This implementation guarantees that it will read as many bytes as possible before giving up. This code has been borrowed from Apache commons IO IOUtils.read(InputStream input, byte[] buffer, int offset, int length).
     * It was slightly modified to have it return -1 if the first read from the stream is -1 (EOF)
     *
     * @param input  The input stream to read.
     * @param bytes  The destination buffer
     * @param offset the initial offset into the buffer
     * @param length The length to read. must be > 0
     * @return Returns the actual length read. If EOF was reached before length given this will be a smaller number. If the stream is already at EOF -1 is returned.
     * @throws IOException thrown if an error occurs reading the stream.
     */
    private int readFromStream(final InputStream input, byte[] bytes, final int offset, final int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("Length must not be negative: " + length);
        }
        int remaining = length;
        while (remaining > 0) {
            int location = length - remaining;
            int count = input.read(bytes, offset + location, remaining);
            if (-1 == count) { // EOF
                // has something been read yet?
                if (remaining != length) {
                    break;
                } else {
                    //Stream is already empty so return -1
                    return -1;
                }
            }
            remaining -= count;
        }
        return length - remaining;
    }

    /**
     * This is used to response to the sftp client based on the policy processing status code only if there is an error. This method will first wait for policy processing to finish then respond to the client.
     *
     * @param id   The id to use for the response message
     * @param file The virtual file to check policy processing status for.
     * @return Returns true if message processing was successful. Otherwise it responsed to the sftp client with an appropriate error and return false.
     * @throws IOException This is thrown if there was an error sending a message tot he sftp client.
     */
    private boolean respondFromErroredMessageProcessingStatus(int id, VirtualSshFile file) throws IOException {
        AssertionStatus status = null;
        try {
            //wait for message processing to finish so that the policy processing status is available
            if (file.getMessageProcessingStatus().waitForMessageProcessingFinished(connector.getLongProperty(SshServerModule.LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS, DEFAULT_MAX_MESSAGE_PROCESSING_WAIT_TIME), TimeUnit.SECONDS)) {
                // retrieve the policy processing status
                status = file.getMessageProcessingStatus().getMessageProcessStatus();
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Error occurred retrieving policy processing status: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            sendStatus(id, SSH_FX_FAILURE, ExceptionUtils.getMessage(e));
        }
        if (status == null || status == AssertionStatus.UNDEFINED) {
            sendStatus(id, SSH_FX_FAILURE, "No status returned from Gateway message processing.");
        } else if (status == AssertionStatus.NONE) {
            return true;
        } else if (status == AssertionStatus.AUTH_FAILED) {
            sendStatus(id, SSH_FX_PERMISSION_DENIED, status.toString());
        } else if (status == AssertionStatus.FAILED) {
            sendStatus(id, SSH_FX_FAILURE, status.toString());
        } else {
            sendStatus(id, SSH_FX_BAD_MESSAGE, status.toString());
        }
        return false;
    }

    /**
     * Returns a file properties by either calling policy processing with the LIST or STAT command (If this connector is
     * enabled to do so). Or returns dummy file property. If it is returning dummy file properties the returned file
     * will always have size 0, modified date of now, will be a file (not a directory),
     * if sftp put is enabled the file will not exist, if sftp get is enabled the file will exist
     *
     * @param path The path to the file
     * @return The virtual ssh file with its properties properly set.
     * @throws MessageProcessingException This is thrown if there is an exception thrown during policy processing.
     */
    private VirtualSshFile getFileProperties(String path) throws MessageProcessingException {
        VirtualSshFile file = (VirtualSshFile) resolveFile(path);
        // Handle the special case here the file is the root file.
        if ("/".equals(file.getAbsolutePath())) {
            file.setFile(false);
        } else if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_STAT)) {
            file = findFileInList(file, statOrListFile(file, CommandKnob.CommandType.STAT));
        } else if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_LIST)) {
            file = findFileInList(file, statOrListFile((VirtualSshFile) file.getParentFile(), CommandKnob.CommandType.LIST));
        } else if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_PUT)) {
            file.setExists(false);
        } else if (connector.getBooleanProperty(SshCredentialAssertion.LISTEN_PROP_ENABLE_SFTP_GET)) {
            file.setExists(true);
        }
        return file;
    }

    /**
     * This will call message processing to delete the file specified by the given path.
     *
     * @param path The path of the file to delete
     * @throws MessageProcessingException
     */
    private VirtualSshFile removeFile(String path) throws MessageProcessingException {
        VirtualSshFile sshFile = (VirtualSshFile) resolveFile(path);
        try {
            processCommand(sshFile, CommandKnob.CommandType.DELETE, Collections.<String, String>emptyMap());
        } finally {
            sshFile.handleClose();
        }
        return sshFile;
    }

    /**
     * This calls the message processor to retrieve a file stats or directory listing for the given file.
     * It returns a XmlVirtualFileList containing either 0 or more elements. If it contains 0 elements that means
     * not such file was found or the directory is empty.
     *
     * @param file The file to retrieve the stats for, or the directory to get the file listing for.
     * @return The XmlVirtualFileList The virtual file list of files in the directory. If null is returned that means there was an error processing the message or message response.
     * @throws MessageProcessingException This is thrown if there was an error processing or attempting to process the policy or attempting to parse the returned response.
     */
    private XmlVirtualFileList statOrListFile(final VirtualSshFile file, final CommandKnob.CommandType commandType) throws MessageProcessingException {
        try {
            //Process the policy
            processCommand(file, commandType, Collections.<String, String>emptyMap());
        } catch (MessageProcessingException e) {
            //Close the file handle if this fails and throw the exception.
            file.handleClose();
            throw e;
        }
        //Get the message processing status and validate that if completed successfully.
        AssertionStatus status = file.getMessageProcessingStatus().getMessageProcessStatus();
        if (status != AssertionStatus.NONE) {
            file.handleClose();
            throw new MessageProcessingException("Message processing failed: " + ((status == null) ? "null status" : status.getMessage()));
        }

        // read data from the message processing response.
        final InputStream fileInputStream;
        try {
            fileInputStream = file.getInputStream();
        } catch (IOException e) {
            file.handleClose();
            logger.log(Level.WARNING, "IO exception processing message: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new MessageProcessingException("Error getting file info for: " + file.getAbsolutePath(), e);
        }
        if (fileInputStream == null) {
            file.handleClose();
            logger.log(Level.WARNING, "IO exception processing message: InputStream is null");
            throw new MessageProcessingException("Error getting file info for: " + file.getAbsolutePath());
        }

        final XmlVirtualFileList xmlVirtualFileList;
        try {
            //convert the response string into an xmlfilelist
            xmlVirtualFileList = JAXB.unmarshal(fileInputStream, XmlVirtualFileList.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Policy did not appear to return a file listing in a proper format: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new MessageProcessingException("Error parsing file info for: " + file.getAbsolutePath(), e);
        } finally {
            //close the file handle after reading the input stream.
            file.handleClose();
        }
        return xmlVirtualFileList;
    }

    /**
     * This will start policy processing and wait for it to finish for a given sshfile and command. Exceptions that make occur during are encapsulated with MessageProcessingException to make it easier to handle.
     *
     * @param file        The ssh file to process.
     * @param commandType The command type
     * @param parameters  The associated with this command type
     * @throws MessageProcessingException This is thrown if an error occurs at any time before, during, or after Policy processing.
     */
    private void processCommand(final VirtualSshFile file, final CommandKnob.CommandType commandType, final Map<String, String> parameters) throws MessageProcessingException {
        // Submit the message processing task to process the file with the given command type and parameters.
        submitMessageProcessingTask(file, commandType, parameters);
        try {
            // wait for the message processor to finish
            if (!file.getMessageProcessingStatus().waitForMessageProcessingFinished(connector.getLongProperty(SshServerModule.LISTEN_PROP_MESSAGE_PROCESSOR_THREAD_WAIT_SECONDS, DEFAULT_MAX_MESSAGE_PROCESSING_WAIT_TIME), TimeUnit.SECONDS)) {
                //this means that message processing failed to finish before the time ran out.
                logger.log(Level.WARNING, "Message processing failed to finish in the allowed amount of time.");
                throw new MessageProcessingException("Error processing " + commandType.name() + " command for: " + file.getAbsolutePath());
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted waiting for message processing to finish: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new MessageProcessingException("Error processing " + commandType.name() + " command for: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Submit a virtual file for message processing. This will start policy processing. The output stream will be closed if this is a GET, LIST, STAT, DELETE, MKDIR, RMDIR, or MOVE command.
     *
     * @param file        The file to process
     * @param commandType The Type on ssh command being sent.
     * @param parameters  The parameters associated with this command.
     */
    private void submitMessageProcessingTask(final VirtualSshFile file, final CommandKnob.CommandType commandType, final Map<String, String> parameters) throws MessageProcessingException {
        try {
            MessageProcessingSshUtil.submitMessageProcessingTask(connector, file, commandType, parameters, session, stashManagerFactory, threadPool, messageProcessor, soapFaultManager, messageProcessingEventChannel);
        } catch (ThreadPoolShutDownException e) {
            logger.log(Level.WARNING, "SFTP thread pool shutdown: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
