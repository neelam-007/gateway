/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.admin.RoleUtils;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

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
public class TrustedCertAdminImpl extends HibernateDaoSupport
  implements TrustedCertAdmin, InitializingBean, ApplicationContextAware {
    private ApplicationContext applicationContext;

    public List findAllCerts() throws FindException, RemoteException {
        return new ArrayList(getManager().findAll());
    }

    public TrustedCert findCertByPrimaryKey(final long oid) throws FindException, RemoteException {
        return (TrustedCert)getManager().findByPrimaryKey(oid);
    }

    public TrustedCert findCertBySubjectDn(final String dn) throws FindException, RemoteException {
        return (TrustedCert)getManager().findBySubjectDn(dn);
    }

    public long saveCert(final TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException {
        RoleUtils.enforceAdminRole(applicationContext);
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
        RoleUtils.enforceAdminRole(applicationContext);
        getManager().delete(oid);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl) throws IOException, RemoteException, HostnameMismatchException {
        return retrieveCertFromUrl(purl, false);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl, boolean ignoreHostname)
      throws IOException, RemoteException, HostnameMismatchException {
        if (!purl.startsWith("https://")) throw new IllegalArgumentException("Can't load certificate from non-https URLs");
        URL url = new URL(purl);

        SSLContext sslContext = null;
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
            if (ignoreHostname) {
                conn.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify(String s, SSLSession sslSession) {
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

            X509Certificate cert = null;
            try {
                cert = (X509Certificate)conn.getServerCertificates()[0];
            } catch (IOException e) {
                logger.log(Level.WARNING, "SSL server hostname didn't match cert", e);
                if (e.getMessage().startsWith("HTTPS hostname wrong")) {
                    throw new HostnameMismatchException(cert.getSubjectDN().getName(), url.getHost());
                }
            }


            return (X509Certificate[])conn.getServerCertificates();
        } else
            throw new IOException("URL resulted in a non-HTTPS connection");
    }

    public X509Certificate getSSGRootCert() throws IOException, CertificateException, RemoteException {
        return KeystoreUtils.getInstance().getRootCert();
    }

    public void setTrustedCertManager(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    /**
     * Set the ApplicationContext that this object runs in.
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void initDao() throws Exception {
        checktrustedCertManager();
    }

    private void checktrustedCertManager() {
        if (trustedCertManager == null) {
            throw new IllegalArgumentException("trusted cert manager is required");
        }
    }


    private TrustedCertManager getManager() {
        return trustedCertManager;
    }

    private Logger logger = Logger.getLogger(getClass().getName());
    private TrustedCertManager trustedCertManager;

}
