/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

/**
 * Exception thrown if a document cannot be processed because it's format is irrecoverably bad (examples:
 * a SOAP document with no Envelope or Body, or with multiple Headers)
 * @author mike
 */
public class InvalidDocumentFormatException extends Exception {
    public InvalidDocumentFormatException() {
        super();
    }

    public InvalidDocumentFormatException(String message) {
        super(message);
    }

    public InvalidDocumentFormatException(Throwable cause) {
        super(cause);
    }

    public InvalidDocumentFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
