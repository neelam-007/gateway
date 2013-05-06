package com.l7tech.server.ssh.client;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.l7tech.util.BufferPool;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

/**
 * This is the implementation of the SCPClient.
 * See here for a description of how the SCP protocol works: https://blogs.oracle.com/janp/entry/how_the_scp_protocol_works
 * This code is based on examples from: http://www.jcraft.com/jsch/examples/ScpTo.java.html and http://www.jcraft.com/jsch/examples/ScpFrom.java.html
 *
 * @author Victor Kazakov
 */
public class ScpClientImpl implements ScpClient {
    protected static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ScpClientImpl.class.getName());

    private final Session session;

    /**
     * Creates a new SCP client running on the given session. The session must be connected before any of the SCP methods are executed.
     *
     * @param session The session that the scp client will use.
     */
    public ScpClientImpl(final Session session) {
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upload(@NotNull final InputStream fileContentsInputStream, @NotNull String remoteDir, @NotNull final String remoteFile, final long fileLength, final long fileOffset, final boolean append, @Nullable final XmlSshFile fileAttributes, @Nullable final FileTransferProgressMonitor fileTransferProgressMonitor) throws IOException, JSchException, FileTransferException {
        remoteDir = addTrailingSlash(remoteDir);
        //=========================== Validate Inputs ===========================
        //validate the directory and file name
        validateNames(remoteDir, remoteFile);
        //For SCP the file offset must be 0. SCP does not allow writing to a file at an offset.
        if (fileOffset != 0) {
            throw new IllegalArgumentException("The file offset for SCP file uploads must be 0");
        }
        //SCP does not allow overwriting file. Existing files are deleted before creating the new file
        if (append) {
            throw new IllegalArgumentException("SCP protocol does not allow overwriting files.");
        }
        // The SCP protocol requires that a file length be sent.
        if (fileLength < 0) {
            throw new IllegalArgumentException("The SCP upload protocol requires the use of a file length. The file length must be a positive number.");
        }

        ChannelExec channel = null;
        try {
            //open an SCP channel to upload a file
            channel = (ChannelExec) session.openChannel("exec");
            String command = "scp -t " + remoteDir + remoteFile;
            channel.setCommand(command);

            //Get the SCP channel input and output streams
            OutputStream channelOut = channel.getOutputStream();
            InputStream channelIn = channel.getInputStream();

            //Connect the channel
            channel.connect();

            //Acknowledge that the channel is properly connected.
            checkAckThrowException(channelIn);

            // send the file info. The file name should not include a '/'
            String permissions = getPermissions(fileAttributes);
            command = "C" + permissions + " " + fileLength + " " + remoteFile;
            command += "\n";
            channelOut.write(command.getBytes());
            channelOut.flush();

            //Verify the file info from the server
            checkAckThrowException(channelIn);

            logger.log(Level.FINE, "Sending file: " + remoteDir + remoteFile + " to server: " + session.getHost() + " via SCP. Stream length: " + fileLength);
            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.start(FileTransferProgressMonitor.UPLOAD, new XmlSshFile(remoteDir + remoteFile, true, fileLength, 644));
            }

            // send the input stream
            IOUtils.copyStream(fileContentsInputStream, channelOut, fileLength, fileTransferProgressMonitor == null ? null : new Functions.UnaryVoid<Long>() {
                @Override
                public void call(Long length) {
                    fileTransferProgressMonitor.progress(length);
                }
            });
            //Do not close the fileContentsInputStream, the caller may still want to use it.

            //Signal the end of the file transfer
            sendAck(channelOut, 0);

            //get an ack from the server.
            checkAckThrowException(channelIn);

            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.end();
            }
        } finally {
            if (channel != null) {
                //This will close the channel in and out streams.
                channel.disconnect();
            }
        }
    }

    private String getPermissions(XmlSshFile fileAttributes) {
        if (fileAttributes == null || fileAttributes.getPermissions() == null || fileAttributes.getPermissions() < 0) {
            return "0644";
        }
        return StringUtils.leftPad(String.valueOf(fileAttributes.getPermissions()), 4, '0');
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void download(@NotNull final OutputStream out, @NotNull String remoteDir, @NotNull final String remoteFile, long fileLength, long fileOffset, @Nullable final FileTransferProgressMonitor fileTransferProgressMonitor) throws IOException, JSchException, FileTransferException {
        remoteDir = addTrailingSlash(remoteDir);
        //=========================== Validate Inputs ===========================
        //validate the directory and file name
        validateNames(remoteDir, remoteFile);
        //For SCP the file offset must be 0. SCP does not allow reading from a file at an offset.
        if (fileOffset != 0) {
            throw new IllegalArgumentException("The file offset for SCP file downloads must be 0");
        }
        // The SCP protocol needs to read the entire file. The file length must be -1 to read to the EOF
        if (fileLength != -1) {
            throw new IllegalArgumentException("The SCP download protocol does not allow a file length to be specified. Must be -1 to read to the EOF.");
        }

        ChannelExec channel = null;
        try {
            //open an SCP channel to download a file
            channel = (ChannelExec) session.openChannel("exec");
            String command = "scp -f " + remoteDir + remoteFile;
            channel.setCommand(command);

            //Get the SCP channel input and output streams
            OutputStream channelOut = channel.getOutputStream();
            InputStream channelIn = channel.getInputStream();

            //Connect the channel
            channel.connect();

            //lets the server know that we are starting to send data
            sendAck(channelOut, 0);

            byte[] buf = BufferPool.getBuffer(16384);
            int c = checkAck(channelIn);
            //TODO: directory copying?
            if (c != 'C') {
                if (c == 'D')
                    throw new FileTransferException("This SCP client does not support recursively copying directories.");
                else
                    throw new FileTransferException("Received an unexpected SCP command: " + c);
            }

            // read the permission bits, e.g '0644 '
            int permissionBytesRead = channelIn.read(buf, 0, 5);
            if (permissionBytesRead != 5) {
                throw new FileTransferException("Read an incorrect number of permissions bytes expected 5 got " + permissionBytesRead);
            }
            String permissionsString = new String(java.util.Arrays.copyOf(buf, 5)).trim();
            final int permissions;
            try {
                permissions = Integer.parseInt(permissionsString);
            } catch (NumberFormatException e) {
                throw new FileTransferException("Permissions bytes retrieved were invalid: Retrieved " + permissionsString);
            }

            //Get the file size
            long fileSize;
            for (int i = 0; ; i++) {
                if (channelIn.read(buf, i, 1) < 0) {
                    throw new FileTransferException("EOF encountered reading the file size.");
                }
                if (buf[i] == ' ') {
                    String fileSizeString = new String(buf, 0, i);
                    try {
                        fileSize = Long.parseLong(fileSizeString);
                    } catch (NumberFormatException e) {
                        throw new FileTransferException("File size retrieved is invalid: Retrieved " + fileSizeString);
                    }
                    break;
                }
                //sanity check
                if (i > 25) {
                    throw new FileTransferException("File size retrieved is too long.");
                }
            }

            //Read the file name. This is discarded as it is not used.
            @SuppressWarnings("UnusedDeclaration")
            String file = null;
            for (int i = 0; ; i++) {
                if (channelIn.read(buf, i, 1) < 0) {
                    throw new FileTransferException("EOF encountered reading the file name.");
                }
                if (buf[i] == (byte) 0x0a) {
                    //noinspection UnusedAssignment
                    file = new String(buf, 0, i);
                    break;
                }
                //sanity check
                if (i > 2048) {
                    throw new FileTransferException("File name retrieved is too long.");
                }
            }

            sendAck(channelOut, 0);
            logger.log(Level.FINE, "Retrieving file: " + remoteDir + remoteFile + " from server: " + session.getHost() + " via SCP. Stream length: " + fileSize);

            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.start(FileTransferProgressMonitor.DOWNLOAD, new XmlSshFile(remoteDir + remoteFile, true, fileSize, 0, permissions));
            }

            // read the contents of the file into the output stream.
            long lengthRetrieved = IOUtils.copyStream(channelIn, out, fileSize, fileTransferProgressMonitor == null ? null : new Functions.UnaryVoid<Long>() {
                @Override
                public void call(Long length) {
                    fileTransferProgressMonitor.progress(length);
                }
            });

            //Do not close the output Stream. It is up to the caller to do so.

            //sanity check
            if (lengthRetrieved != fileSize) {
                throw new FileTransferException("File of incorrect length returned. Expected: " + fileSize + " got " + lengthRetrieved);
            }

            checkAckThrowException(channelIn);

            sendAck(channelOut, 0);

            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.end();
            }
        } finally {
            channel.disconnect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws JSchException {
        //does nothing SCP channels are created when performing the upload or download actions.
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return session.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        //does nothing SCP channels are created when performing the upload or download actions.
    }


    /**
     * Sends a single byte to the remote server.
     *
     * @param channelOut The channel to write the byte to.
     * @param b          The byte to write
     * @throws IOException This is thrown if there was an error writing to the output stream.
     */
    private static void sendAck(OutputStream channelOut, int b) throws IOException {
        channelOut.write(b);
        channelOut.flush();
    }

    /**
     * Reads a single byte from the input stream. If the byte read is not 0 an exception is thrown.
     *
     * @param in The input stream to read the ack from
     * @throws IOException           This is thrown if there was an error reading from the input stream.
     * @throws FileTransferException This is thrown if the byte read is not equal to '0' or if an error message is received
     */
    private static void checkAckThrowException(InputStream in) throws IOException, FileTransferException {
        int ack = checkAck(in);
        if (ack != 0) {
            throw new FileTransferException("Received unexpected ack: " + ack);
        }
    }

    /**
     * Reads an Ack from the server. If an error is received an exception will be thrown.
     *
     * @param in The SCP channel input stream.
     * @return The byte read.
     * @throws IOException           This is thrown if there was an error reading from the stream
     * @throws FileTransferException This is thrown if an error is returned by the server. In this case the error message will also be read.
     */
    private static int checkAck(InputStream in) throws IOException, FileTransferException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1
        if (b == 0) return b;
        if (b == -1) return b;
        if (b == 1 || b == 2) {
            byte[] buf = BufferPool.getBuffer(4096);
            final String errorMessage;
            for (int i = 0; ; i++) {
                if (in.read(buf, i, 1) < 0) {
                    throw new FileTransferException("EOF encountered reading error message. Retrieved: " + (b == 1 ? "Error: " : "Fatal error") + new String(buf, 0, i));
                }
                if (buf[i] == '\n') {
                    errorMessage = new String(buf, 0, i);
                    break;
                }
                //sanity check
                if (i > 2048) {
                    throw new FileTransferException("Error message to long. Retrieved: " + (b == 1 ? "Error: " : "Fatal error") + new String(buf, 0, i));
                }
            }
            throw new FileTransferException((b == 1 ? "Error: " : "Fatal error: ") + errorMessage);
        }
        return b;
    }

    /**
     * Validate the remote directory and the remote file Strings.
     *
     * @param remoteDir  The remote directory string to validate
     * @param remoteFile The remote file string to validate.
     */
    private static void validateNames(String remoteDir, String remoteFile) {
        if (!remoteDir.isEmpty() && remoteDir.charAt(remoteDir.length() - 1) != '/') {
            throw new IllegalArgumentException("A remote directory was specified but it must end with a '/'. Given: " + remoteDir);
        }
        if (remoteFile.isEmpty()) {
            throw new IllegalArgumentException("The remote file name cannot be empty.");
        }
    }

    private static String addTrailingSlash(String path){
        if (path != null && !path.isEmpty() && path.charAt(path.length() - 1) != '/') {
            return path + '/';
        }
        return path;
    }
}
