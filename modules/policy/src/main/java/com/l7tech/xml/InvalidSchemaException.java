/**
 * $Id$
 */
package com.l7tech.xml;

public class InvalidSchemaException extends Exception {
    public InvalidSchemaException(String message) {
        super(message);
    }

    public InvalidSchemaException(Throwable cause) {
        super(cause);
    }

    public InvalidSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
