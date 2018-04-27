package com.l7tech.server.transport.jms2;

public class JmsConnectionMaxWaitException extends Exception {

    public JmsConnectionMaxWaitException() {
        super();
    }

    public JmsConnectionMaxWaitException(String message) {
        super(message);
    }

    public JmsConnectionMaxWaitException(String message, Throwable cause) {
        super(message,cause);
    }
}
