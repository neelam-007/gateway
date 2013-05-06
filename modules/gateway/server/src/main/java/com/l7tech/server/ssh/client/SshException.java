package com.l7tech.server.ssh.client;

/**
 * This is an SSH exception. It is used to be the base exception for other ssh exceptions.
 *
 * @author Victor Kazakov
 */
public class SshException extends Exception {
    public SshException(String message) {
        super(message);
    }

    public SshException(Throwable t) {
        super(t);
    }

    public SshException(String message, Throwable t) {
        super(message, t);
    }
}
