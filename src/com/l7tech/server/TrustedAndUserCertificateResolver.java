/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server;

import com.l7tech.common.security.X509Entity;
import com.l7tech.common.security.xml.CertificateResolver;
import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.FindException;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that can look up any certificate known to the SSG by thumbprint.
 */
public class TrustedAndUserCertificateResolver implements CertificateResolver {
    private static final Logger logger = Logger.getLogger(TrustedAndUserCertificateResolver.class.getName());
    private TrustedCertManager trustedCertManager = null;
    private ClientCertManager clientCertManager = null;

    private X509Certificate sslKeystoreCertificate = null;
    private String sslKeystoreCertThumbprint = null;
    private String sslKeystoreCertSki = null;

    private X509Certificate rootCertificate = null;
    private String rootCertificateThumbprint = null;
    private String rootCertificateSki = null;

    public void setTrustedCertManager(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setSslKeystoreCertificate(X509Certificate sslKeystoreCertificate) {
        this.sslKeystoreCertificate = sslKeystoreCertificate;
        try {
            if (sslKeystoreCertificate != null) {
                this.sslKeystoreCertThumbprint = CertUtils.getThumbprintSHA1(sslKeystoreCertificate);
                this.sslKeystoreCertSki = CertUtils.getSki(sslKeystoreCertificate);
            }
        } catch (CertificateException e) {
            throw new RuntimeException("Invalid SSL certificate", e);
        }
    }

    public void setRootCertificate(X509Certificate rootCertificate) {
        this.rootCertificate = rootCertificate;
        try {
            if (rootCertificate != null) {
                this.rootCertificateThumbprint = CertUtils.getThumbprintSHA1(rootCertificate);
                this.rootCertificateSki = CertUtils.getSki(rootCertificate);
            }
        } catch (CertificateException e) {
            throw new RuntimeException("Invalid root certificate", e);
        }
    }

    public X509Certificate lookup(String thumbprint) {
        try {
            if (rootCertificateThumbprint != null && rootCertificateThumbprint.equals(thumbprint))
                return rootCertificate;

            if (sslKeystoreCertThumbprint != null && sslKeystoreCertThumbprint.equals(thumbprint))
                return sslKeystoreCertificate;

            List got = trustedCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            got = clientCertManager.findByThumbprint(thumbprint);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        } catch (IOException e) {
            logger.log(Level.WARNING, "Bad certificate in database: " + e.getMessage(), e);
            return null;
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Bad certificate in database: " + e.getMessage(), e);
            return null;
        }
    }

    public X509Certificate lookupBySki(String ski) {
        try {
            if (rootCertificateSki != null && rootCertificateSki.equals(ski))
                return rootCertificate;

            if (sslKeystoreCertSki != null && sslKeystoreCertSki.equals(ski))
                return sslKeystoreCertificate;

            List got = trustedCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();

            got = clientCertManager.findBySki(ski);
            if (got != null && got.size() >= 1)
                return ((X509Entity)got.get(0)).getCertificate();



            return null;
        } catch (FindException e) {
            throw new RuntimeException(e); // very bad place
        } catch (IOException e) {
            logger.log(Level.WARNING, "Bad certificate in database: " + e.getMessage(), e);
            return null;
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Bad certificate in database: " + e.getMessage(), e);
            return null;
        }
    }

    public X509Certificate lookupByKeyName(String keyName) {
        // TODO Implement this using a lookup by cert DN if we decide to bother supporting this feature here
        return null;
    }
}
