/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Loads a certificate and private key from a keystore.
 */
public class CertLoader {
    private static final Logger logger = Logger.getLogger(CertLoader.class.getName());

    /** Alternate keystore path.  Default is alternate keystore disabled. */
    public static final String PROP_SSL_KEYSTORE_PATH = "com.l7tech.proxy.ssl.keystorePath";

    /** Alternate keystore pass phrase.  Default is alternate keystore disabled. */
    public static final String PROP_SSL_KEYSTORE_PASS = "com.l7tech.proxy.ssl.keystorePass";

    /** Keystore type.  Default is "PKCS12". */
    public static final String PROP_SSL_KEYSTORE_TYPE = "com.l7tech.proxy.ssl.keystoreType";

    /** Alias to use with alternate keystore.  Default is to use first Key entry in the keystore. */
    public static final String PROP_SSL_KEYSTORE_ALIAS = "com.l7tech.proxy.ssl.keystoreAlias";

    /** Key password for alternate keystore alias.  Default is to reuse keystore pass phrase. */
    public static final String PROP_SSL_KEYSTORE_ALIASPASS = "com.l7tech.proxy.ssl.keystoreAliasPass";


    private final X509Certificate[] certChain;
    private final PrivateKey privateKey;

    /**
     * Load a cert and private key from the specified keystore, which is assumed to be of type "PKCS12".
     * If the keystore contains more than one Key alias, the first one found will be loaded.
     *
     * @param path          path of keystore to load.  Must not be null or empty.
     * @param pass          keystore passphrase.  Must not be null or empty.
     * @throws IOException  if a usable cert chain and private key could not be obtained using these settings.
     */
    public CertLoader(String path, String pass) throws IOException {
        this(path, pass, null, null, null);
    }

    /**
     * Load a cert and private key from the specified keystore.  If the keystore contains more than one
     * Key alias, the first one found will be loaded.
     *
     * @param path          path of keystore to load.  Must not be null or empty.
     * @param pass          keystore passphrase.  Must not be null or empty.
     * @param type          keystore type, or null to default to "PKCS12".
     * @throws IOException  if a usable cert chain and private key could not be obtained using these settings.
     */
    public CertLoader(String path, String pass, String type) throws IOException {
        this(path, pass, type, null, null);
    }

    /**
     * Load a cert and private key from the specified alias of the specified keystore.
     *
     * @param path          path of keystore to load.  Must not be null or empty.
     * @param pass          keystore passphrase.  Must not be null or empty.
     * @param type          keystore type, or null to default to "PKCS12".
     * @param alias         alias, or null to use the first Key entry found in the keystore.
     * @param aliasPass     key passphrase, or null to reuse the keystore passphrase.
     * @throws IOException  if a usable cert chain and private key could not be obtained using these settings.
     */
    public CertLoader(String path, String pass, String type, String alias, String aliasPass) throws IOException {
        X509Certificate[] certChain = null;
        PrivateKey privateKey = null;

        if (path != null && pass != null && path.length() > 0) {
            if (type == null || type.length() < 1)
                type = "PKCS12";
            InputStream is = null;
            try {
                KeyStore ks = KeyStore.getInstance(type);
                is = new FileInputStream(new File(path));
                ks.load(is, pass.toCharArray());

                Certificate[] certs = null;
                Key key = null;

                if (aliasPass == null)
                    aliasPass = pass;

                if (alias != null && alias.length() > 0) {
                    // Specific alias
                    certs = ks.getCertificateChain(alias);
                    key = ks.getKey(alias, aliasPass.toCharArray());
                } else {
                    // No alias -- just use the first cert we see
                    Enumeration aliases = ks.aliases();
                    while (aliases.hasMoreElements()) {
                        alias = (String)aliases.nextElement();
                        if (ks.isKeyEntry(alias)) {
                            certs = ks.getCertificateChain(alias);
                            key = ks.getKey(alias, aliasPass.toCharArray());
                            break;
                        }
                    }
                }

                if (!(key instanceof PrivateKey))
                    throw new IOException("Unable to use configured keystore for outgoing SSL connections to Federated Gateways: key is not a private key");
                privateKey = (PrivateKey)key;

                if (certs != null && certs.length > 0) {
                    X509Certificate[] x509Certs = new X509Certificate[certs.length];
                    for (int i = 0; i < certs.length; i++) {
                        Certificate cert = certs[i];
                        if (!(cert instanceof X509Certificate))
                            throw new IOException("Unable to use configured keystore for outgoing SSL connections to Federated Gateways: a cert in the chain is not X.509");
                        x509Certs[i] = (X509Certificate)certs[i];
                    }
                    certChain = x509Certs;
                }

            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw (IOException)new IOException("Unable to use configured keystore for outgoing SSL connections to Federated Gateways: " + e.getMessage()).initCause(e);
            } finally {
                if (is != null) try { is.close(); } catch (Exception e) {}
            }

            if (certChain == null || certChain.length <= 0 || privateKey == null)
                throw new IOException("No usable client certificate found");
        }

        this.certChain = certChain;
        this.privateKey = privateKey;
        logger.info("Loaded custom keystore: " + path);
    }

    public X509Certificate[] getCertChain() {
        return certChain;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Return a certificate and private key configured according to current system properties, or null if
     * no such certificate is configured or can be loaded.
     *
     * @return a CertLoader containing a certificate chain and a private key, or null if one was not configured
     *         or one was configured but could not be loaded (errors logged).
     */
    private static CertLoader loadConfiguredCertLoader() {
        String path      = System.getProperty(PROP_SSL_KEYSTORE_PATH);
        String pass      = System.getProperty(PROP_SSL_KEYSTORE_PASS);

        if (path == null || pass == null || pass.length() < 1) {
            logger.info("No custom keystore is configured.");
            return null;
        }

        String type      = System.getProperty(PROP_SSL_KEYSTORE_TYPE);
        String alias     = System.getProperty(PROP_SSL_KEYSTORE_ALIAS);
        String aliasPass = System.getProperty(PROP_SSL_KEYSTORE_ALIASPASS);
        try {
            return new CertLoader(path, pass, type, alias, aliasPass);
        } catch (IOException e) {
            logger.log(Level.INFO, "Unable to load custom keystore: " + e.getMessage(), e);
            return null;
        }
    }

    private static Boolean loadedConfigured = null;
    private static CertLoader configuredCertLoader = null;

    /**
     * Return a certificate and private key configured according to current system properties, or null if
     * no such certificate is configured or can be loaded.
     *
     * @return a CertLoader containing a certificate chain and a private key, or null if one was not configured
     *         or one was configured but could not be loaded.
     */
    public static synchronized CertLoader getConfiguredCertLoader() {
        if (loadedConfigured != null) return configuredCertLoader;

        configuredCertLoader = loadConfiguredCertLoader();
        loadedConfigured = Boolean.TRUE;
        return configuredCertLoader;
    }
}
