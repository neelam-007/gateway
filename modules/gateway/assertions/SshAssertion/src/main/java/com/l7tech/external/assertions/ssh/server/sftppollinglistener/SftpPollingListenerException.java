package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

/**
 * This exception is thrown when there's a SFTP polling problem.
 */
class SftpPollingListenerException extends Exception {
    SftpPollingListenerException( String message ) {
        super( message );
    }

    SftpPollingListenerException( Throwable e ) {
        super(e);
    }

    SftpPollingListenerException( String message, Throwable cause ) {
        super(message, cause);
    }
}