package com.l7tech.xml;

import com.l7tech.util.InvalidDocumentFormatException;

/**
 * Extension of InvalidDocumentFormatException for signatures.
 */
public class InvalidDocumentSignatureException extends InvalidDocumentFormatException {
    public InvalidDocumentSignatureException() {
        super();
    }

    public InvalidDocumentSignatureException(String message) {
        super(message);
    }

    public InvalidDocumentSignatureException(Throwable cause) {
        super(cause);
    }

    public InvalidDocumentSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
