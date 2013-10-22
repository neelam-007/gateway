package com.l7tech.server.transport.ftp;

import org.apache.ftpserver.ftplet.FileSystemFactory;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.FtpException;

/**
 * An empty file system.
 *
 * @author Steve Jones
 */
class VirtualFileSystemManager implements FileSystemFactory {

    /**
     * Create a virtual file system for the given user.
     *
     * @param user The user (ignored)
     * @return The file system for the user
     * @throws FtpException never
     */
    public FileSystemView createFileSystemView(User user) throws FtpException {
        return new VirtualFileSystem();
    }
}
