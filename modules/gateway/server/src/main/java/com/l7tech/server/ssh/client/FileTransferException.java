package com.l7tech.server.ssh.client;

/**
 * This is a file transfer exception. It should be thrown when there is an error transferring a file over ssh.
 *
 * @author Victor Kazakov
 */
public class FileTransferException extends SshException {

    public FileTransferException(String message) {
        super(message);
    }

    public FileTransferException(Throwable t) {
        super(t);
    }

    public FileTransferException(String message, Throwable t) {
        super(message, t);
    }
}
