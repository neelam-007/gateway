/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

/**
 * Exception thrown when a client certificate cannot be obtained because the Gateway has already issued
 * a client certificate for this username.
 * @author mike
 * @version 1.0
 */
public class CertificateAlreadyIssuedException extends ClientCertificateException {
    public CertificateAlreadyIssuedException(Throwable cause) {
        super(cause);
    }

    public CertificateAlreadyIssuedException(String message) {
        super(message);
    }
}
