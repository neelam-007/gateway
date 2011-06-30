package com.l7tech.external.assertions.ssh.server;

import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.SshFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File system view based on virtual file system. Here the root directory will be user virtual root (/).
 */
public class VirtualFileSystemView implements FileSystemView {

    private final Logger LOG = LoggerFactory.getLogger(VirtualFileSystemView.class);

    // the first and the last character will always be '/'
    // It is always with respect to the root directory.
    private String currDir;

    private boolean caseInsensitive = false;

    /**
     * Constructor - internal do not use directly, use {@link VirtualFileSystemFactory} instead
     */
    protected VirtualFileSystemView() {
        this(false);
    }

    /**
     * Constructor - internal do not use directly, use {@link VirtualFileSystemFactory} instead
     */
    public VirtualFileSystemView(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    /**
     * Get file object.
     */
    public SshFile getFile(String file) {
        // strip the root directory
        String physicalName = VirtualSshFile.getPhysicalName("/", currDir, file, caseInsensitive);
        String userFileName = physicalName.substring("/".length() - 1);
        return new VirtualSshFile(userFileName, false);
    }

    @Override
    public SshFile getFile(SshFile baseDir, String file) {
        return new VirtualSshFile(baseDir.getAbsolutePath() + file, false);
    }
}
