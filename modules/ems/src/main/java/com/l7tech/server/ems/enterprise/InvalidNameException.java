package com.l7tech.server.ems.enterprise;

/**
 * Thrown when an entity name is illegal or will have collision with siblings.
 *
 * @since Enterprise Manager 1.0
 * @author rmak
 */
public class InvalidNameException extends RuntimeException {
    public InvalidNameException(String message) {
        super(message);
    }
}
