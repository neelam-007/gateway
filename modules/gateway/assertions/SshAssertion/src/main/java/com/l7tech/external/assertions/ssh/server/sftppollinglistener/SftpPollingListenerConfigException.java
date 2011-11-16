package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * This exception is thrown when a SFTP polling listener configuration is problematic
 *
 * @author njordan
 */
class SftpPollingListenerConfigException extends Exception {
    SftpPollingListenerConfigException(String message) {
        super( message );
    }

    SftpPollingListenerConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
