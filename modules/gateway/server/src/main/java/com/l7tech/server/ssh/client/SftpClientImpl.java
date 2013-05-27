package com.l7tech.server.ssh.client;

import com.jcraft.jsch.*;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is the implementation of the SftpClient.
 * The RFC for SFTP version 3 is here: http://tools.ietf.org/html/draft-ietf-secsh-filexfer-02
 * This code is based on examples from: http://www.jcraft.com/jsch/examples/Sftp.java.html
 *
 * @author Victor Kazakov
 */
public class SftpClientImpl implements SftpClient {
    private final ChannelSftp channel;

    /**
     * Creates a new SFTP client running on the given session. The session must be connected.
     *
     * @param session The session that the sftp client will use.
     */
    public SftpClientImpl(final Session session) throws JSchException {
        channel = (ChannelSftp) session.openChannel("sftp");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upload(@NotNull InputStream in, @NotNull String remoteDir, @NotNull String remoteFile, long fileLength, long fileOffset, boolean append, @Nullable final XmlSshFile fileAttributes, @Nullable final FileTransferProgressMonitor fileTransferProgressMonitor) throws IOException, FileTransferException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);

        OutputStream out = null;
        try {
            try {
                //With this channel.put method. both the append and resume modes have the exact same behaviour. They will not truncate the existing file. Overwrite will truncate it.
                //Giving a mode 3 (a non existing mode) will make is so that writing does not start at the end of the file and the file contents will not be truncated. (This is a bit of a hack :-(
                out = channel.put(remoteDir + remoteFile, null, append ? 3 : ChannelSftp.OVERWRITE, fileOffset);
            } catch (SftpException e) {
                //Need to translate SftpException's to FileTransferException's in upload and download methods.
                throw new FileTransferException(e);
            }
            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.start(FileTransferProgressMonitor.UPLOAD, new XmlSshFile(remoteDir + remoteFile, true, fileLength, 644));
            }
            //Copy the input stream to the sftp output stream.
            IOUtils.copyStream(in, out, fileLength, fileTransferProgressMonitor == null ? null : new Functions.UnaryVoid<Long>() {
                @Override
                public void call(Long length) {
                    fileTransferProgressMonitor.progress(length);
                }
            });

            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.end();
            }
        } finally {
            if (out != null) {
                ResourceUtils.closeQuietly(out);
            }
        }

        //sets file permissions. This must be done after the out stream has been closed.
        if (fileAttributes != null && fileAttributes.getPermissions() != null && fileAttributes.getPermissions() >= 0) {
            try {
                setFilePermissions(remoteDir, remoteFile, fileAttributes.getPermissions());
            } catch (SftpException e) {
                //Need to translate SftpException's to FileTransferException's in upload and download methods.
                throw new FileTransferException(e);
            }
        }

        //do not close the inputStream. It is up to the calling method to do so.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void download(@NotNull OutputStream out, @NotNull String remoteDirIn, @NotNull final String remoteFile, long fileLength, long fileOffset, @Nullable final FileTransferProgressMonitor fileTransferProgressMonitor) throws IOException, FileTransferException {
        final String remoteDir = addTrailingSlash(remoteDirIn);
        validateNames(remoteDir, remoteFile);

        InputStream in = null;
        try {
            final AtomicLong filesize = new AtomicLong();
            try {
                //Get the file upload input stream
                in = channel.get(remoteDir + remoteFile, fileTransferProgressMonitor == null ? null : new SftpProgressMonitor() {
                    @Override
                    public void init(int op, String src, String dest, long max) {
                        //get the file size
                        filesize.set(max);
                    }

                    @Override
                    public boolean count(long count) {
                        //just return true to continue the transfer. We will keep track of the count in the copy stream method.
                        return true;
                    }

                    @Override
                    public void end() {
                        //do nothing, we will notify after the copy stream method returns.
                    }
                }, fileOffset);
            } catch (SftpException e) {
                throw new FileTransferException(e);
            }
            //notify the progress monitor that the download is starting. This should be done here and not in the above SftpProgressMonitor in order to properly catch some exceptions.
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.start(FileTransferProgressMonitor.DOWNLOAD, new XmlSshFile(remoteDir + remoteFile, true, filesize.get(), 0));
            }

            // Copy the sftp input stream to the output stream
            IOUtils.copyStream(in, out, fileLength, fileTransferProgressMonitor == null ? null : new Functions.UnaryVoid<Long>() {
                @Override
                public void call(Long length) {
                    fileTransferProgressMonitor.progress(length);
                }
            });
            //notify the progress monitor
            if (fileTransferProgressMonitor != null) {
                fileTransferProgressMonitor.end();
            }
        } finally {
            if (in != null) {
                ResourceUtils.closeQuietly(in);
            }
        }
        //do not close the OutputStream. It is up to the calling method to do so.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect() throws JSchException {
        channel.connect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        channel.disconnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmlVirtualFileList listDirectory(String remoteDir, String remoteFile) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);

        final ArrayList<XmlSshFile> files = new ArrayList<>();
        channel.ls(remoteDir + remoteFile, new ChannelSftp.LsEntrySelector() {
            @Override
            public int select(ChannelSftp.LsEntry entry) {
                //The size is in bytes
                //Modified time * 1000 to convert to milliseconds
                //Permissions are given in the format described here: http://tools.ietf.org/html/draft-ietf-secsh-filexfer-13#section-7.6
                files.add(new XmlSshFile(entry.getFilename(), !entry.getAttrs().isDir(), entry.getAttrs().getSize(), entry.getAttrs().getMTime() * 1000L, Integer.parseInt(Integer.toOctalString(entry.getAttrs().getPermissions())) % 1000));
                return ChannelSftp.LsEntrySelector.CONTINUE;
            }
        });
        return new XmlVirtualFileList(files);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XmlSshFile getFileAttributes(String remoteDir, final String remoteFile) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);

        try {
            SftpATTRS attrs = channel.stat(remoteDir + remoteFile);
            return new XmlSshFile(remoteFile, !attrs.isDir(), attrs.getSize(), attrs.getMTime() * 1000L, Integer.parseInt(Integer.toOctalString(attrs.getPermissions())) % 1000);
        } catch (SftpException e) {
            //Return null if no such file exists. Otherwise throw the exception
            if (e.getMessage() != null && e.getMessage().contains("No such file")) {
                return null;
            }
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFile(String remoteDir, String remoteFile) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);
        channel.rm(remoteDir + remoteFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renameFile(String remoteDir, String remoteFile, String newFileName) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);
        channel.rename(remoteDir + remoteFile, newFileName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(String remoteDir, String remoteFile) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);
        channel.mkdir(remoteDir + remoteFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDirectory(String remoteDir, String remoteFile) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);
        channel.rmdir(remoteDir + remoteFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFilePermissions(String remoteDir, String remoteFile, int permissions) throws SftpException {
        remoteDir = addTrailingSlash(remoteDir);
        validateNames(remoteDir, remoteFile);
        //validate that the permission value is valid
        if (permissions < 0 || permissions / 1000 > 0 || Integer.toString(permissions).matches(".*[9|8].*")) {
            throw new IllegalArgumentException("Invalid permissions value: " + permissions);
        }
        channel.chmod(Integer.parseInt(Integer.toString(permissions), 8), remoteDir + remoteFile);
    }

    /**
     * Validate the remote directory and the remote file Strings.
     *
     * @param remoteDir  The remote directory string to validate
     * @param remoteFile The remote file string to validate.
     */
    private void validateNames(String remoteDir, String remoteFile) {
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
