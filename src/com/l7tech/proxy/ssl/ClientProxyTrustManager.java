/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import org.apache.log4j.Category;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Trust manager for the Client Proxy, which will decide whether a given connection is actually connected
 * to the SSG we expect it to be.
 * User: mike
 * Date: Jul 31, 2003
 * Time: 8:49:58 PM
 */
public class ClientProxyTrustManager implements X509TrustManager {
    private static final Category log = Category.getInstance(ClientProxyTrustManager.class);
    private SsgFinder ssgFinder = null;

    public ClientProxyTrustManager(SsgFinder ssgFinder) {
        this.ssgFinder = ssgFinder;
    }

    public X509Certificate[] getAcceptedIssuers() {
        log.info("Making list of SSG CA certs");
        List ssgs = ssgFinder.getSsgList();
        List certs = new ArrayList();
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            try {
                X509Certificate cert = SsgKeyStoreManager.getServerCert(ssg);
                if (cert != null)
                    certs.add(cert);
            } catch (Exception e) {
                log.warn(e);
                // eat it; we'll avoid listing this SSG as an accepted issuer.
            }
        }
        return (X509Certificate[]) certs.toArray(new X509Certificate[0]);
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        throw new ClientProxySslException("Server-side SSL sockets not supported by ClientProxyTrustManager");
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        // TODO: This is currently a little bit _too_ trustworthy.  To be finished on Tuesday when Francois is around!
        log.info("FIXME: automatically trusting the server certificate chain");
        return;
    }
}
