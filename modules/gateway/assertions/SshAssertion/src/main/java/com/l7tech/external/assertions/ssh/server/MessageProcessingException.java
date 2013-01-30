package com.l7tech.external.assertions.ssh.server;

/**
 * This Is a message processing exception. It is thrown when message processing fails or if it fails to begin properly.
 *
 * @author Victor Kazakov
 */
public class MessageProcessingException extends Exception {

    public MessageProcessingException(String message) {
        super(message);
    }

    public MessageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
