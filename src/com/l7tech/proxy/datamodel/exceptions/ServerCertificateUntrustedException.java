/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel.exceptions;

import com.l7tech.proxy.datamodel.Ssg;

import java.security.cert.CertificateException;

/**
 * Exception thrown by ClientProxyTrustManager when it doesn't recognize an Ssg certificate.
 * User: mike
 * Date: Aug 13, 2003
 * Time: 4:45:48 PM
 */
public class ServerCertificateUntrustedException extends CertificateException {
    final Ssg peerSsg;

    public ServerCertificateUntrustedException(Ssg peerSsg) {
        this.peerSsg = peerSsg;
    }

    public ServerCertificateUntrustedException(Ssg peerSsg, String msg) {
        super(msg);
        this.peerSsg = peerSsg;
    }

    public ServerCertificateUntrustedException(Ssg peerSsg, Throwable t) {
        super(t == null ? "Server certificate untrusted" : t.toString());
        initCause(t);
        this.peerSsg = peerSsg;
    }

    /** @return the SSG we were attempting to connect to when the problem occurred.  If null, then that WAS the problem. :) */
    public Ssg getPeerSsg() {
        return peerSsg;
    }
}
