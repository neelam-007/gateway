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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class TrustedCertAdminImpl extends ApplicationObjectSupport implements TrustedCertAdmin, InitializingBean {

    public List findAllCerts() throws FindException, RemoteException {
        try {
            return (List)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return new ArrayList(getManager().findAll());
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't get list of certs", e);
        }
    }

    public TrustedCert findCertByPrimaryKey(final long oid) throws FindException, RemoteException {
        try {
            return (TrustedCert)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return (TrustedCert)getManager().findByPrimaryKey(oid);
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't find cert", e);
        }
    }

    public TrustedCert findCertBySubjectDn(final String dn) throws FindException, RemoteException {
        try {
            return (TrustedCert)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    return (TrustedCert)getManager().findBySubjectDn(dn);
                }
            });
        } catch ( ObjectModelException e ) {
            throw new FindException("Couldn't find cert", e);
        }
    }

    public long saveCert(final TrustedCert cert) throws SaveException, UpdateException, VersionException, RemoteException {
        try {
            RoleUtils.enforceAdminRole();
            Long oid = (Long)doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    long oid;
                    if ( cert.getOid() == Entity.DEFAULT_OID ) {
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
                    return new Long(oid);
                }
            });
            return oid.longValue();
        } catch (ObjectModelException e ) {
            throw new SaveException("Couldn't save cert", e);
        }
    }

    public void deleteCert(final long oid) throws FindException, DeleteException, RemoteException {
        try {
            RoleUtils.enforceAdminRole();
            doInTransactionAndClose(new PersistenceAction() {
                public Object run() throws ObjectModelException {
                    getManager().delete(oid);
                    return null;
                }
            });
        } catch ( FindException e ) {
            throw new DeleteException("Couldn't find cert to be deleted", e);
        } catch ( ObjectModelException e ) {
            throw new DeleteException("Couldn't delete cert", e);
        }
    }

    public X509Certificate[] retrieveCertFromUrl( String purl ) throws IOException, RemoteException, HostnameMismatchException {
        return retrieveCertFromUrl(purl, false);
    }

    public X509Certificate[] retrieveCertFromUrl( String purl, boolean ignoreHostname )
            throws IOException, RemoteException, HostnameMismatchException {
        if ( !purl.startsWith("https://") ) throw new IllegalArgumentException("Can't load certificate from non-https URLs");
        URL url = new URL(purl);

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init( null,
                             new X509TrustManager[] { new X509TrustManager() {
                                 public X509Certificate[] getAcceptedIssuers() {
                                     return new X509Certificate[0];
                                 }
                                 public void checkClientTrusted( X509Certificate[] x509Certificates, String s ) { }
                                 public void checkServerTrusted( X509Certificate[] x509Certificates, String s ) { }
                             } },
                             null);
        } catch ( NoSuchAlgorithmException e ) {
            logger.log( Level.INFO, e.getMessage(), e );
            throw new IOException(e.getMessage());
        } catch ( KeyManagementException e ) {
            logger.log( Level.INFO, e.getMessage(), e );
            throw new IOException(e.getMessage());
        }

        URLConnection gconn = url.openConnection();
        if ( gconn instanceof HttpsURLConnection ) {
            HttpsURLConnection conn = (HttpsURLConnection)gconn;
            conn.setSSLSocketFactory(sslContext.getSocketFactory());
            if ( ignoreHostname ) {
                conn.setHostnameVerifier(new HostnameVerifier() {
                    public boolean verify( String s, SSLSession sslSession ) {
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
                cert = (X509Certificate) conn.getServerCertificates()[0];
            } catch (IOException e) {
                logger.log(Level.WARNING, "SSL server hostname didn't match cert", e);
                if (e.getMessage().startsWith("HTTPS hostname wrong")) {
                    throw new HostnameMismatchException(cert.getSubjectDN().getName(), url.getHost());
                }
            }


            return (X509Certificate[])conn.getServerCertificates();
        } else throw new IOException("URL resulted in a non-HTTPS connection");
    }

    public X509Certificate getSSGRootCert() throws IOException, CertificateException, RemoteException {
        return KeystoreUtils.getInstance().getRootCert();
    }

    public void setTrustedCertManager(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    public void afterPropertiesSet() throws Exception {
        checktrustedCertManager();
    }

    private void checktrustedCertManager() {
         if (trustedCertManager == null) {
             throw new IllegalArgumentException("trusted cert manager is required");
         }
     }

    private Object doInTransactionAndClose(PersistenceAction r) throws ObjectModelException {
        HibernatePersistenceContext context = null;
        try {
            context = (HibernatePersistenceContext)PersistenceContext.getCurrent();
            return context.doInTransaction(r);
        } catch (SQLException e) {
            throw new ObjectModelException(e);
        } finally {
            if (context != null) context.close();
        }
    }

    private TrustedCertManager getManager() {
        return trustedCertManager;
    }

    private Logger logger = Logger.getLogger(getClass().getName());
    private TrustedCertManager trustedCertManager;


}
