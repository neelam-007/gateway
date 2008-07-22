package com.l7tech.util;

/**
 * Exception thrown if a document cannot be processed because an element was found which wasn't expected.
 * 
 * @author mike
 */
public class ElementAlreadyExistsException extends InvalidDocumentFormatException {
    public ElementAlreadyExistsException() {
    }

    public ElementAlreadyExistsException(String message) {
        super(message);
    }

    public ElementAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public ElementAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
