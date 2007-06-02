/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.keystore.SsgKeyEntry;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.keystore.SsgKeyFinder;
import com.l7tech.server.security.keystore.SsgKeyStore;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;

import javax.net.ssl.*;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.security.*;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.interfaces.RSAPrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TrustedCertAdminImpl implements TrustedCertAdmin {
    private final X509Certificate rootCertificate;
    private final X509Certificate sslCertificate;
    private final LicenseManager licenseManager;
    private final SsgKeyStoreManager ssgKeyStoreManager;

    public TrustedCertAdminImpl(TrustedCertManager trustedCertManager,
                                X509Certificate rootCertificate,
                                X509Certificate sslCertificate,
                                LicenseManager licenseManager,
                                SsgKeyStoreManager ssgKeyStoreManager)
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
        this.ssgKeyStoreManager = ssgKeyStoreManager;
        if (ssgKeyStoreManager == null)
            throw new IllegalArgumentException("SsgKeyStoreManager is required");
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    private void checkLicenseHeavy() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_TRUSTSTORE);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    public List<TrustedCert> findAllCerts() throws FindException, RemoteException {
        checkLicense();
        return new ArrayList<TrustedCert>(getManager().findAll());
    }

    public TrustedCert findCertByPrimaryKey(final long oid) throws FindException, RemoteException {
        checkLicense();
        return getManager().findByPrimaryKey(oid);
    }

    public TrustedCert findCertBySubjectDn(final String dn) throws FindException, RemoteException {
        checkLicense();
        return getManager().findBySubjectDn(dn);
    }

    public long saveCert(final TrustedCert cert) throws SaveException, UpdateException, RemoteException {
        checkLicenseHeavy();
        long oid;
        if (cert.getOid() == TrustedCert.DEFAULT_OID) {
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
        checkLicenseHeavy();
        getManager().delete(oid);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl) throws IOException, HostnameMismatchException {
        checkLicenseHeavy();
        return retrieveCertFromUrl(purl, false);
    }

    public X509Certificate[] retrieveCertFromUrl(String purl, boolean ignoreHostname)
      throws IOException, HostnameMismatchException {
        checkLicenseHeavy();
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

    public X509Certificate getSSGRootCert() throws IOException, CertificateException {
        checkLicense();
        return rootCertificate;
    }

    public X509Certificate getSSGSslCert() throws IOException, CertificateException {
        checkLicense();
        return sslCertificate;
    }

    public List<KeystoreInfo> findAllKeystores() throws IOException, FindException, KeyStoreException {
        List<SsgKeyFinder> finders = ssgKeyStoreManager.findAll();
        List<KeystoreInfo> list = new ArrayList<KeystoreInfo>();
        for (SsgKeyFinder finder : finders) {
            long id = finder.getId();
            String name = finder.getName();
            SsgKeyFinder.SsgKeyStoreType type = finder.getType();
            boolean readonly = !finder.isMutable();
            list.add(new KeystoreInfo(id, name, type.toString(), readonly));
        }
        return list;
    }

    public List<SsgKeyEntry> findAllKeys(long keystoreId) throws IOException, CertificateException, FindException {
        try {
            SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);

            List<SsgKeyEntry> list = new ArrayList<SsgKeyEntry>();
            List<String> aliases = keyFinder.getAliases();
            for (String alias : aliases) {
                SsgKeyEntry entry = keyFinder.getCertificateChain(alias);
                list.add(entry);
            }

            return list;
        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        }
    }

    public void deleteKey(long keystoreId, String keyAlias) throws IOException, CertificateException, DeleteException {
        try {
            SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
            SsgKeyStore store = keyFinder.getKeyStore();
            if (store == null)
                throw new DeleteException("Unable to delete key: keystore id " + keystoreId + " is read-only");
            store.deletePrivateKeyEntry(keyAlias);

        } catch (KeyStoreException e) {
            throw new CertificateException(e);
        } catch (FindException e) {
            throw new DeleteException("Unable to find keystore: " + ExceptionUtils.getMessage(e), e);
        }
    }

    public X509Certificate generateKeyPair(long keystoreId, String alias, String dn, int keybits, int expiryDays) throws RemoteException, FindException, GeneralSecurityException {
        if (alias == null) throw new NullPointerException("alias is null");
        if (alias.length() < 1) throw new IllegalArgumentException("alias is empty");
        if (dn == null) throw new NullPointerException("dn is null");
        if (dn.length() < 1) throw new IllegalArgumentException("dn is empty");

        // Ensure that Sun certificate parser will like this dn
        new X500Principal(dn).getEncoded();

        SsgKeyFinder keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        SsgKeyStore keystore = null;
        if (keyFinder != null && keyFinder.isMutable())
            keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new FindException("No mutable keystore found with id " + keystoreId);
        if (expiryDays < 1)
            throw new IllegalArgumentException("expiryDays must be positive");

        return keystore.generateKeyPair(alias, new X500Principal(dn), keybits, expiryDays);
    }

    public byte[] generateCSR(long keystoreId, String alias, String dn) throws FindException {
        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error getting keystore", e);
        }
        SsgKeyStore keystore;
        if (keyFinder != null) {
            keystore = keyFinder.getKeyStore();
        } else {
            logger.log(Level.WARNING, "error getting keystore");
            throw new FindException("cannot find keystore");
        }
        try {
            CertificateRequest res = keystore.makeCertificateSigningRequest(alias, dn);
            return res.getEncoded();
        } catch (Exception e) {
            logger.log(Level.WARNING, "error getting keystore", e);
            throw new FindException("error making CSR", e);
        }
    }

    public void assignNewCert(long keystoreId, String alias, String[] pemChain) throws UpdateException, CertificateException {
        X509Certificate[] safeChain = parsePemChain(pemChain);

        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error setting new cert", e);
            throw new UpdateException("error getting keystore", e);
        } catch (FindException e) {
            throw new UpdateException("error getting keystore", e);
        }

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new UpdateException("error: keystore ID " + keystoreId + " is read-only");

        try {
            keystore.replaceCertificateChain(alias, safeChain);
        } catch (Exception e) {
            logger.log(Level.WARNING, "error setting new cert", e);
            throw new UpdateException("error setting new cert", e);
        }
    }

    private X509Certificate[] parsePemChain(String[] pemChain) throws CertificateException {
        if (pemChain == null || pemChain.length < 1)
            throw new IllegalArgumentException("PEM chain must contain at least one certificate");

        X509Certificate[] safeChain = new X509Certificate[pemChain.length];
        try {
            for (int i = 0; i < pemChain.length; i++) {
                String pem = pemChain[i];
                safeChain[i] = CertUtils.decodeFromPEM(pem);
            }
        } catch (IOException e) {
            throw new CertificateException("error setting new cert", e);
        }
        return safeChain;
    }

    public void importKey(long keystoreId, String alias, String[] pemChain, BigInteger modulus, BigInteger privateExponent)
            throws SaveException, CertificateException, InvalidKeySpecException
    {
        X509Certificate[] safeChain = parsePemChain(pemChain);

        SsgKeyFinder keyFinder;
        try {
            keyFinder = ssgKeyStoreManager.findByPrimaryKey(keystoreId);
        } catch (KeyStoreException e) {
            throw new SaveException("error getting keystore: " + ExceptionUtils.getMessage(e), e);
        } catch (FindException e) {
            throw new SaveException("error getting keystore: " + ExceptionUtils.getMessage(e), e);
        }

        SsgKeyStore keystore = keyFinder.getKeyStore();
        if (keystore == null)
            throw new SaveException("error: keystore ID " + keystoreId + " is read-only");

        // Ensure all certs are instances that have come from the default certificate factory
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(modulus, privateExponent);
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey)KeyFactory.getInstance("RSA").generatePrivate(keySpec);
            SsgKeyEntry entry = new SsgKeyEntry(keystoreId, alias, safeChain, rsaPrivateKey);
            keystore.storePrivateKeyEntry(entry, false);
        } catch (InvalidKeySpecException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw new SaveException("error setting new cert: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreException e) {
            logger.log(Level.WARNING, "error setting new cert", e);
            throw new SaveException("Error setting new cert: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private TrustedCertManager getManager() {
        return trustedCertManager;
    }

    private Logger logger = Logger.getLogger(getClass().getName());
    private final TrustedCertManager trustedCertManager;

}
