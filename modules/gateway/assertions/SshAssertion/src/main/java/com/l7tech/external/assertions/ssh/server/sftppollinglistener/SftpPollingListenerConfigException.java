package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * This exception is thrown when a SFTP polling listener configuration is problematic
 *
 * @author njordan
 */
public class SftpPollingListenerConfigException extends Exception {
    public SftpPollingListenerConfigException(String message) {
        super( message );
    }

    public SftpPollingListenerConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
