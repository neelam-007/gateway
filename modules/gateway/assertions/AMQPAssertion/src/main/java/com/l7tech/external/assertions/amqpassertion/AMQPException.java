package com.l7tech.external.assertions.amqpassertion;

/**
 * Created by chaoy01 on 2018-03-21.
 */
public class AMQPException extends Exception {
    public AMQPException() {
    }

    public AMQPException(String message) {
        super(message);
    }

    public AMQPException(Throwable cause) {
        super(cause);
    }

    public AMQPException(String message, Throwable cause) {
        super(message, cause);
    }
}
