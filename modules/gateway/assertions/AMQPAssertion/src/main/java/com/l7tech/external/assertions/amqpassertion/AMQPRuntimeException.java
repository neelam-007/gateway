package com.l7tech.external.assertions.amqpassertion;

/**
 * Created by chaoy01 on 2018-03-21.
 */
public class AMQPRuntimeException extends RuntimeException {
    public AMQPRuntimeException() {
    }

    public AMQPRuntimeException(String message) {
        super(message);
    }

    public AMQPRuntimeException(Throwable cause) {
        super(cause);
    }

    public AMQPRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
