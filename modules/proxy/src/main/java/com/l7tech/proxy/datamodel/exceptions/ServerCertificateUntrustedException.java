/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

import java.security.cert.CertificateException;

/**
 * Exception thrown by ClientProxyTrustManager when it doesn't recognize an Ssg certificate.
 */
public class ServerCertificateUntrustedException extends CertificateException {

    public ServerCertificateUntrustedException() {
    }

    public ServerCertificateUntrustedException(String msg) {
        super(msg);
    }

    public ServerCertificateUntrustedException(Throwable t) {
        super(t == null ? "Server certificate untrusted" : t.toString());
        initCause(t);
    }
}
