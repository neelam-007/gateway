package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * This exception is thrown when there's a runtime SFTP polling problem.
 */
public class SftpPollingListenerRuntimeException extends Exception {
    public SftpPollingListenerRuntimeException(String message) {
        super( message );
    }

    public SftpPollingListenerRuntimeException(Throwable e) {
        super(e);
    }

    public SftpPollingListenerRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}