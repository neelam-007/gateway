package com.l7tech.server.ssh.client;

import com.jcraft.jsch.JSchException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is a file transfer client. It defines 2 method for transferring file. Upload and Download.
 *
 * @author Victor Kazakov
 */
public interface FileTransferClient extends SshClient {

    /**
     * Uploads the input stream to the server. Note this will not close the InputStream after executing, it will be up to the caller to do so.
     *
     * @param in                          The input stream to upload
     * @param remoteDir                   The remote directory the file is located in
     * @param remoteFile                  The Name of the remote file
     * @param fileLength                  The file length. If this is -1 the entire stream will be uploaded until EOF
     * @param fileOffset                  The file offset. Start writing this many bytes into the file. The bytes proceeding the offset will remain unchanged or if append is false, they will be clear and set to 0.
     * @param append                      If this is true the file will be overwritten. If it is false the file will be cleared and truncated first before writing to it.
     * @param fileAttributes              This holds any file attributes to send to add to the file.
     * @param fileTransferProgressMonitor Provide a FileTransferProgressMonitor to monitor the progress of the file transfer. This can be set to null.
     * @throws IOException           This is thrown if there was an exception writing to the file.
     * @throws JSchException         This is thrown if there was an error communicating with the server
     * @throws FileTransferException This is thrown if there was an error performing the file transfer.
     */
    public void upload(@NotNull InputStream in, @NotNull String remoteDir, @NotNull String remoteFile, long fileLength, long fileOffset, boolean append, @Nullable XmlSshFile fileAttributes, @Nullable FileTransferProgressMonitor fileTransferProgressMonitor) throws IOException, JSchException, FileTransferException;

    /**
     * This will write data to the output stream given for the specified remote file. Note this will not close the OutputStream after executing, it will be up to the caller to do so.
     *
     * @param out                         The output stream to write file data to.
     * @param remoteDir                   The remote directory the file is located in
     * @param remoteFile                  The name of the remote file
     * @param fileLength                  The length of the file. If this is -1 the entire file will be written into the output stream.
     * @param fileOffset                  The offset into the file to start reading from.
     * @param fileTransferProgressMonitor Provide a FileTransferProgressMonitor to monitor the progress of the file transfer. This can be set to null.
     * @throws IOException           This is thrown if there was an exception reading the file.
     * @throws JSchException         This is thrown if there was an error communicating with the server
     * @throws FileTransferException This is thrown if there was an error performing the file transfer.
     */
    public void download(@NotNull OutputStream out, @NotNull String remoteDir, @NotNull String remoteFile, long fileLength, long fileOffset, @Nullable FileTransferProgressMonitor fileTransferProgressMonitor) throws IOException, JSchException, FileTransferException;

}
