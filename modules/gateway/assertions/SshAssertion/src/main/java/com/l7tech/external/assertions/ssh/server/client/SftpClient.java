package com.l7tech.external.assertions.ssh.server.client;

import com.jscape.inet.sftp.Sftp;
import com.jscape.inet.sftp.SftpException;
import com.jscape.inet.sftp.SftpFile;
import com.l7tech.message.SshKnob;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

/**
 * Currently wraps JSCAPE's SFTP client, exposing a subset of the client methods for the Gateway.
 */
public class SftpClient implements SshClient {
    Sftp sftpClient;

    public SftpClient(@NotNull Sftp sftpClient) {
        this.sftpClient = sftpClient;
        this.sftpClient.disableFileAccessLogging();
    }

    @Override
    public void connect() throws SftpException {
        sftpClient.connect();
    }

    @Override
    public boolean isConnected() {
        return sftpClient.isConnected();
    }

    @Override
    public void disconnect() {
        sftpClient.disconnect();
    }

    /**
     * This will return an input stream from the file specified. It will start reading from the given offset into the file.
     *
     * @param remoteDir  The remote directory the file is located in
     * @param remoteFile The name of the file
     * @param fileOffset The offset into the file to start reading from
     * @return The input stream to read file data from.
     * @throws SftpException This is thrown if there was an error communicating with the sftp server.
     */
    public InputStream getFileInputStream(String remoteDir, String remoteFile, long fileOffset) throws SftpException {
        setDir(remoteDir);
        return sftpClient.getInputStream(remoteFile, fileOffset);
    }

    /**
     * This will write the data in the given input stream to the remote file specified. It will write to the given offset.
     * If append is false the file will be truncated! It should only be set to true if processing partial uploads
     *
     * @param in               The inputstream to read the data from
     * @param remoteDir        The remote directory the file is located in.
     * @param remoteFile       The name of the remote file
     * @param fileMetadata     The file metedata to use to set the attribute of the file. If this is null file attribute will not be changed from the default
     * @param fileOffset       The file offset to start writing the data to
     * @param failIfFileExists This will fail is the file already exists.
     * @param append           This will append data to the file. If this is false the file will be truncated.
     * @throws IOException
     */
    public void upload(InputStream in, String remoteDir, String remoteFile, SshKnob.FileMetadata fileMetadata, long fileOffset, boolean failIfFileExists, boolean append) throws IOException {
        setDir(remoteDir);

        //checks if the file exists
        if (failIfFileExists && sftpClient.isValidPath(remoteDir + '/' + remoteFile)) {
            throw new CausedIOException("The file already exists");
        }

        //gets the output stream and writes to it.
        OutputStream out = sftpClient.getOutputStream(remoteFile, fileOffset, append);
        IOUtils.copyStream(in, out);

        //if the file metadata is specified it is set on the file.
        if (fileMetadata != null && fileMetadata.getPermission() >= 0) {
            sftpClient.setFilePermissions(remoteFile, fileMetadata.getPermission());
        }
    }

    /**
     * This will return the directory listing for the given directory
     *
     * @param remoteDir  The directory that this directory is located in.
     * @param remoteFile The name of the directory to list
     * @return An enumeration for SftpFile. these are the files in the directory. If the enumeration is empty then the directory is empty
     * @throws SftpException
     */
    public Enumeration<SftpFile> listDirectory(String remoteDir, String remoteFile) throws SftpException {
        setDir(remoteDir + '/' + remoteFile);
        return sftpClient.getDirListing();
    }

    /**
     * This will return the file attributes for the specified file.
     *
     * @param remoteDir  The directory that the file is located in.
     * @param remoteFile The name of the file to return the file attributes for
     * @return The SftpFile set with the correct file attributes. Null if no such file was found.
     * @throws SftpException
     */
    public SftpFile getFileAttributes(String remoteDir, String remoteFile) throws SftpException {
        setDir(remoteDir);
        Enumeration<SftpFile> listing = sftpClient.getDirListing(remoteFile);
        return listing.hasMoreElements() ? listing.nextElement() : null;
    }

    /**
     * Deletes the specified remote file.
     *
     * @param remoteDir  The remote directory that the file is located in
     * @param remoteFile The name of the file
     * @throws SftpException
     */
    public void deleteFile(String remoteDir, String remoteFile) throws SftpException {
        setDir(remoteDir);
        sftpClient.deleteFile(remoteFile);
    }

    /**
     * This will move a file to a different location or just rename the file.
     *
     * @param remoteDir   The remote directory that the file is located in
     * @param remoteFile  The name of the file
     * @param newFileName The new file to name and path.
     * @throws SftpException
     */
    public void renameFile(String remoteDir, String remoteFile, String newFileName) throws SftpException {
        setDir(remoteDir);
        sftpClient.renameFile(remoteFile, newFileName);
    }

    /**
     * This will create a directory on the sftp server.
     *
     * @param remoteDir  The directory to create this directory in.
     * @param remoteFile The name of the directory to create
     * @throws SftpException
     */
    public void createDirectory(String remoteDir, String remoteFile) throws SftpException {
        setDir(remoteDir);
        sftpClient.makeDir(remoteFile);
    }

    /**
     * This will delete a directory of the remote sftp server.
     *
     * @param remoteDir  The directory that the directory to delete is located in.
     * @param remoteFile The name of the directory to delete.
     * @throws SftpException
     */
    public void removeDirectory(String remoteDir, String remoteFile) throws SftpException {
        setDir(remoteDir);
        sftpClient.deleteDir(remoteFile);
    }

    /**
     * This will set the remote working directory to the one specified.
     *
     * @param remoteDir The directory to set the sftp working directory to.
     * @throws SftpException
     */
    private void setDir(String remoteDir) throws SftpException {
        if (!StringUtils.isEmpty(remoteDir)) {
            sftpClient.setDir(remoteDir);
        }
    }
}
