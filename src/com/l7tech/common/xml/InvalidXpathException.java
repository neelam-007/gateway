/**
 * $Id$
 */
package com.l7tech.common.xml;

public class InvalidXpathException extends Exception {
    public InvalidXpathException(String message) {
        super(message);
    }

    public InvalidXpathException(Throwable cause) {
        super(cause);
    }

    public InvalidXpathException(String message, Throwable cause) {
        super(message, cause);
    }
}
