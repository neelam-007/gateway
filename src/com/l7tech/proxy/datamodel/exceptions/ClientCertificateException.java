/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Thrown if a client certificate is needed but could not be obtained.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 10:16:06 AM
 */
public class ClientCertificateException extends Exception {
    public ClientCertificateException() {
    }

    public ClientCertificateException(String message) {
        super(message);
    }

    public ClientCertificateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientCertificateException(Throwable cause) {
        super(cause);
    }
}
