package com.l7tech.xml.xpath;

/**
 * Exception thrown if an XPath references a variable that is not defined when the XPath is evaluated.
 */
public class NoSuchXpathVariableException extends Exception {
    public NoSuchXpathVariableException() {
    }

    public NoSuchXpathVariableException(String message) {
        super(message);
    }

    public NoSuchXpathVariableException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchXpathVariableException(Throwable cause) {
        super(cause);
    }
}
