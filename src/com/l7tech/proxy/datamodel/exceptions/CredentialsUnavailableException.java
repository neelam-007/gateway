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
    public static final String DEFAULT_MESSAGE = "Unable to obtain new credentials from user.";

    public CredentialsUnavailableException() {
        super(DEFAULT_MESSAGE);
    }

    public CredentialsUnavailableException(String message) {
        super(message != null ? message : DEFAULT_MESSAGE);
    }

    public CredentialsUnavailableException(String message, Throwable cause) {
        super(message != null ? message : DEFAULT_MESSAGE, cause);
    }

    public CredentialsUnavailableException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }
}
