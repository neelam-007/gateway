package com.l7tech.external.assertions.ssh.server;

import org.apache.sshd.server.FileSystemFactory;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Virtual file system factory.
 */
public class VirtualFileSystemFactory implements FileSystemFactory {

    private final Logger LOG = LoggerFactory.getLogger(VirtualFileSystemFactory.class);

    private boolean createHome;

    private boolean caseInsensitive;

    /**
     * Should the home directories be created automatically
     * @return true if the file system will create the home directory if not available
     */
    public boolean isCreateHome() {
        return createHome;
    }

    /**
     * Set if the home directories be created automatically
     * @param createHome true if the file system will create the home directory if not available
     */

    public void setCreateHome(boolean createHome) {
        this.createHome = createHome;
    }

    /**
     * Is this file system case insensitive.
     * Enabling might cause problems when working against case-sensitive file systems, like on Linux
     * @return true if this file system is case insensitive
     */
    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    /**
     * Should this file system be case insensitive.
     * Enabling might cause problems when working against case-sensitive file systems, like on Linux
     * @param caseInsensitive true if this file system should be case insensitive
     */
    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Create the appropriate user file system view (user name ignored).
     */
    public FileSystemView createFileSystemView(ServerSession session) {
        return new VirtualFileSystemView(caseInsensitive);
    }
}
