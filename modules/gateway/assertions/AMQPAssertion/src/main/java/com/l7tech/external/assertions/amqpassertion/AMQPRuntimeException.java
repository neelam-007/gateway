package com.l7tech.external.assertions.amqpassertion;

/**
 * This is used as a wrapper to catch exceptions that may arise from creating AMQP connections
 *
 * Created by chaoy01 on 2018-03-21.
 */
public class AMQPRuntimeException extends RuntimeException {

    public AMQPRuntimeException(Throwable cause) {
        super(cause);
    }

}
