/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class TrustedCertAdminImpl implements TrustedCertAdmin {
    private final X509Certificate rootCertificate;
    private final X509Certificate sslCertificate;
    private final LicenseManager licenseManager;

    public TrustedCertAdminImpl(TrustedCertManager trustedCertManager,
                                X509Certificate rootCertificate,
                                X509Certificate sslCertificate,
                                LicenseManager licenseManager)
    {
        this.trustedCertManager = trustedCertManager;
        if (trustedCertManager == null) {
            throw new IllegalArgumentException("trusted cert manager is required");
        }
        this.rootCertificate = rootCertificate;
        if (rootCertificate == null) {
            throw new IllegalArgumentException("Root Certificate is required");
        }
        this.sslCertificate = sslCertificate;
        if (sslCertificate == null) {
            throw new IllegalArgumentException("Ssl Certificate is required");
        }
        this.licenseManager = licenseManager;
        if (licenseManager == null)
            throw new IllegalArgumentException("License manager is required");
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public List findAllCerts() throws FindException, RemoteException {
        checkLicense();
        return new ArrayList(getManager().findAll());
    }

    public TrustedCert findCertByPrimaryKey(final long oid) throws FindException, RemoteException {
        checkLicense();
        return getManager().findByPrimaryKey(oid);
    }

    public TrustedCert findCertBySubjectDn(final String dn) throws FindException, RemoteException {
        checkLicense();
        return getManager().findBySubjectDn(dn);
    }

    public long saveCert(final TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException {
        checkLicense();
        long oid;
        if (cert.getOid() == Entity.DEFAULT_OID) {
            // check that cert with same dn not already exist
            // because the sql error thrown by hibernate makes it impossible
            // to handle that case specifically.
            try {
                TrustedCert existingCert = getManager().findBySubjectDn(cert.getSubjectDn());
                if (existingCert != null) {
                    throw new DuplicateObjectException("Cert with dn=" + cert.getSubjectDn() +
                      " already exists.");
                }
            } catch (FindException e) {
                logger.log(Level.FINE, "error looking for similar cert", e);
            }
            oid = getManager().save(cert);
        } else {
            getManager().update(cert);
            oid = cert.getOid();
        }
        return oid;
    }

    public void deleteCert(final long oid) throws FindException, DeleteException, RemoteException {
        checkLicense();
        getManager().delete(oid);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl) throws IOException, RemoteException, HostnameMismatchException {
        checkLicense();
        return retrieveCertFromUrl(purl, false);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl, boolean ignoreHostname)
      throws IOException, RemoteException, HostnameMismatchException {
        checkLicense();
        if (!purl.startsWith("https://")) throw new IllegalArgumentException("Can't load certificate from non-https URLs");
        URL url = new URL(purl);

        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null,
              new X509TrustManager[]{new X509TrustManager() {
                  public X509Certificate[] getAcceptedIssuers() {
                      return new X509Certificate[0];
                  }

                  public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
                  }

                  public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
                  }
              }},
              null);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.INFO, e.getMessage(), e);
            throw new IOException(e.getMessage());
        } catch (KeyManagementException e) {
            logger.log(Level.INFO, e.getMessage(), e);
            throw new IOException(e.getMessage());
        }

        URLConnection gconn = url.openConnection();
        if (gconn instanceof HttpsURLConnection) {
            HttpsURLConnection conn = (HttpsURLConnection)gconn;
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            final String[] sawHost = new String[] { null };
            if (ignoreHostname) {
                conn.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String s, SSLSession sslSession) {
                        sawHost[0] = s;
                        return true;
                    }
                });
            }

            try {
                conn.connect();
            } catch (IOException e) {
                logger.log(Level.INFO, "Unable to connect to: " + purl);

                // rethrow it
                throw e;
            }

            try {
                return (X509Certificate[])conn.getServerCertificates();
            } catch (IOException e) {
                logger.log(Level.WARNING, "SSL server hostname didn't match cert", e);
                if (e.getMessage().startsWith("HTTPS hostname wrong")) {
                    throw new HostnameMismatchException(sawHost[0], url.getHost());
                }
                throw e;
            }
        } else
            throw new IOException("URL resulted in a non-HTTPS connection");
    }

    public X509Certificate getSSGRootCert() throws IOException, CertificateException, RemoteException {
        checkLicense();
        return rootCertificate;
    }

    public X509Certificate getSSGSslCert() throws IOException, CertificateException, RemoteException {
        checkLicense();
        return sslCertificate;
    }

    private TrustedCertManager getManager() {
        return trustedCertManager;
    }

    private Logger logger = Logger.getLogger(getClass().getName());
    private final TrustedCertManager trustedCertManager;

}
