/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Thrown when a client certificate is found to have been revoked by the SSG.
 *
 * User: mike
 * Date: Sep 10, 2003
 * Time: 5:26:34 PM
 */
public class ClientCertificateRevokedException extends Exception {
    public ClientCertificateRevokedException() {
    }

    public ClientCertificateRevokedException(String message) {
        super(message);
    }

    public ClientCertificateRevokedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientCertificateRevokedException(Throwable cause) {
        super(cause);
    }
}
