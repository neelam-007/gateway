package com.l7tech.external.assertions.ftprouting.server;

import org.apache.ftpserver.ftplet.FileSystemManager;
import org.apache.ftpserver.ftplet.FileSystemView;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;

/**
 * @author nilic
 */
class VirtualFileSystemManager implements FileSystemManager {

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
