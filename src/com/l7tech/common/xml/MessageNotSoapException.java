/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

/**
 * Exception thrown when a SOAP-specific operation is attempted on a non-SOAP message.
 */
public class MessageNotSoapException extends InvalidDocumentFormatException {
    public MessageNotSoapException(String cause) {
        super(cause);
    }

    public MessageNotSoapException() {
        super("Message is not SOAP");
    }

    public MessageNotSoapException(Throwable cause) {
        super(cause);
    }

    public MessageNotSoapException(String message, Throwable cause) {
        super(message, cause);
    }
}
