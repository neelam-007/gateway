/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.ssl;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgFinder;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import org.apache.log4j.Category;

import javax.net.ssl.X509TrustManager;
import java.security.Principal;
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
        log.info("ClientProxyTrustManager: getAcceptedIssuers: Making list of SSG CA certs");
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
        log.info("ClientProxyTrustManager: checkServerTrusted");
        if (x509Certificates == null || x509Certificates.length < 1 || s == null)
            throw new IllegalArgumentException("empty certificate chain, or null auth type");

        // Get the list of trusted SSG CA keys.
        X509Certificate[] trustedCerts = getAcceptedIssuers();

        for (int i = 0; i < x509Certificates.length; i++) {
            X509Certificate cert = x509Certificates[i];
            cert.checkValidity(); // will abort if this throws
            if (i + 1 < x509Certificates.length) {
                try {
                    cert.verify(x509Certificates[i + 1].getPublicKey());
                } catch (Exception e) {
                    log.error("Unable to verify signature in peer certificate chain", e);
                    // This is a serious problem with the cert chain presented by the peer.  Do a full abort.
                    throw new CertificateException("Unable to verify signature in peer certificate chain: " + e);
                }
            }
            for (int j = 0; j < trustedCerts.length; j++) {
                X509Certificate trustedCert = trustedCerts[j];
                Principal trustedDN = trustedCert.getSubjectDN();
                if (cert.getIssuerDN().equals(trustedDN)) {
                    try {
                        cert.verify(trustedCert.getPublicKey());
                        log.info("Peer certificate was signed by a trusted SSG.");
                        return;
                    } catch (Exception e) {
                        log.error("Unable to verify peer certificate with trusted cert", e);
                        // Server SSL cert might have changed.  Attempt to reimport it
                        throw new ServerCertificateUntrustedException("Unable to verify peer certificate with trusted cert: " + e);
                    }
                } else if (cert.getSubjectDN().equals(trustedDN)) {
                    if (cert.equals(trustedCert)) {
                        log.info("Peer certificate exactly matched that of a trusted SSG.");
                        return;
                    }
                }
            }
        }

        // We probably just havne't talked to this Ssg before.  Trigger a reimport of the certificate.
        log.warn("Couldn't find trusted certificate in peer's certificate chain.");
        throw new ServerCertificateUntrustedException("Couldn't find trusted certificate in peer's certificate chain");
    }
}
