/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

import java.security.cert.CertificateException;

/**
 * Exception thrown by ClientProxyTrustManager when it doesn't recognize an Ssg certificate.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 4:45:48 PM
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
