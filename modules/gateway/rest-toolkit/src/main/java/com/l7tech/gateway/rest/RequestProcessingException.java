package com.l7tech.gateway.rest;

/**
 * This exception is thrown by the restHandler when an exception is encountered processing the rest request.
 */
public class RequestProcessingException extends Exception {
    public RequestProcessingException(String message) {
        super(message);
    }

    public RequestProcessingException(String message, Throwable t) {
        super(message, t);
    }
}
