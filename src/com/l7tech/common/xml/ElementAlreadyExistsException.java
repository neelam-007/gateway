/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

/**
 * Exception thrown if a document cannot be processed because an element was found which wasn't expected.
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
