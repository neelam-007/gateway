/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Thrown if the user cannot be prompted for new credentials.
 * @author mike
 * @version 1.0
 */
public class CredentialsUnavailableException extends OperationCanceledException {
    public CredentialsUnavailableException() {
    }

    public CredentialsUnavailableException(String message) {
        super(message);
    }

    public CredentialsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public CredentialsUnavailableException(Throwable cause) {
        super(cause);
    }
}
