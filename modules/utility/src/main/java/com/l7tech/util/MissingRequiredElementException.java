/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

/**
 * Exception thrown if a document cannot be processed because a required element is missing.
 */
public class MissingRequiredElementException extends InvalidDocumentFormatException {
    public MissingRequiredElementException() {
    }

    public MissingRequiredElementException(String message) {
        super(message);
    }

    public MissingRequiredElementException(Throwable cause) {
        super(cause);
    }

    public MissingRequiredElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
