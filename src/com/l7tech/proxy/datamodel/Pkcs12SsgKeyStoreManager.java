/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.FileUtils;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.ssl.CertLoader;
import com.l7tech.proxy.ssl.CurrentSslPeer;
import com.l7tech.proxy.util.SslUtils;

import java.io.*;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of SsgKeyStoreManager for the stand-alone SecureSpan Bridge, saving the material to PKCS#12
 * files on disk.  This implementation keeps
 * two files in the Ssg's keystore path and certstore path, which hold the private and public information respectively.
 * <p>
 * The cert store (typically ~/.l7tech/certsNNN.p12, where NNN is the Ssg ID) is encrypted with the
 * hardcoded pass phrase "lwbnasudg" to guard against file corruption (but not deliberate sabotage).  It contains
 * the server cert and the active copy of the client cert.
 * <p>
 * The key store (typically ~/.l7tech/keysNNN.p12) is encrypted with the Ssg password.  It contains the private
 * key and an unused copy of the client certificate.
 */
public class Pkcs12SsgKeyStoreManager extends SsgKeyStoreManager {
    private static final String CLIENT_CERT_ALIAS = "getCachedClientCert";
    private static final String SERVER_CERT_ALIAS = "setCachedServerCert";
    /**
     * This is the password that will be used for obfuscating the trust store, and checking it for
     * corruption when it is reloaded.
     */
    private static final char[] TRUSTSTORE_PASSWORD = "lwbnasudg".toCharArray();
    /** Keystore type.  JCEKS is more secure than the default JKS format. */
    private static final String KEYSTORE_TYPE = "BCPKCS12";
    private static final String TRUSTSTORE_TYPE = "BCPKCS12";
    private static final Logger log = Logger.getLogger(SsgKeyStoreManager.class.getName());

    private final Ssg ssg;

    /**
     * Create a PKCS#12 Ssg keystore manager that will manage cert and key persistence for the specified Ssg.
     * The cert store will be kept in the file {@link Ssg#getTrustStoreFile} and the key store will
     * be kept in the file {@link Ssg#getKeyStoreFile}.
     *
     * @param ssg the Ssg whose key material we will be managing.
     */
    public Pkcs12SsgKeyStoreManager(Ssg ssg) {
        this.ssg = ssg;
    }

    protected boolean isClientCertAvailabile() throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return trusted.getRuntime().getSsgKeyStoreManager().isClientCertAvailabile();
        if (ssg.isFederatedGateway()) {
            if (CertLoader.getConfiguredCertLoader() == null)
                throw new RuntimeException("Not supported for WS-Trust federation without custom keystore");
        }
        if (ssg.getRuntime().getHaveClientCert() == null)
            ssg.getRuntime().setHaveClientCert(getClientCert() == null ? Boolean.FALSE : Boolean.TRUE);
        return ssg.getRuntime().getHaveClientCert().booleanValue();
    }

    public boolean isClientCertUnlocked() throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return trusted.getRuntime().getSsgKeyStoreManager().isClientCertUnlocked();
        if (ssg.isFederatedGateway())
            return false;
        if (!isClientCertAvailabile()) return false;
        return ssg.getRuntime().getCachedPrivateKey() != null;
    }

    /**
     * Convert a generic certificate, which may be a BouncyCastle implementation, into a standard Sun
     * implementation.
     * @param generic  the certificate to convert, which may be null.
     * @return a new instance from the default X.509 CertificateFactory, or null if generic was null.
     * @throws java.security.cert.CertificateException if the generic certificate could not be processed
     */
    private X509Certificate convertToSunCertificate(X509Certificate generic) throws CertificateException {
        if (generic == null) return null;
        final byte[] encoded = generic.getEncoded();
        return CertUtils.decodeCert(encoded);
    }

    public X509Certificate getServerCert() throws KeyStoreCorruptException {
        try {
            X509Certificate cert = ssg.getRuntime().getCachedServerCert();
            if (cert != null) return cert;
            synchronized (ssg) {
                cert = (X509Certificate) getTrustStore().getCertificate(SERVER_CERT_ALIAS);
            }
            cert = convertToSunCertificate(cert);
            ssg.getRuntime().setCachedServerCert(cert);
            return cert;
        } catch (KeyStoreException e) {
            log.log(Level.SEVERE, "impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
        } catch (CertificateException e) {
            throw new RuntimeException("impossible exception", e); // can't happen
        }
    }

    public X509Certificate getClientCert() throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return trusted.getRuntime().getSsgKeyStoreManager().getClientCert();
        if (ssg.isFederatedGateway()) {
            CertLoader cc = CertLoader.getConfiguredCertLoader();
            if (cc != null) {
                log.log(Level.FINE, "Using preconfigured keystore for federated client cert");
                return cc.getCertChain()[0];
            }

            log.log(Level.FINE, "No client cert for Federated Gateway; returning null");
            return null;
        }
        try {
            X509Certificate cert = ssg.getRuntime().getCachedClientCert();
            if (cert != null) return cert;
            synchronized (ssg) {
                cert = (X509Certificate) getTrustStore().getCertificate(CLIENT_CERT_ALIAS);
            }
            cert = convertToSunCertificate(cert);
            ssg.getRuntime().setCachedClientCert(cert);
            return cert;
        } catch (KeyStoreException e) {
            log.log(Level.SEVERE, "impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
        } catch (CertificateException e) {
            throw new RuntimeException("impossible exception", e); // can't happen
        }
    }

    public void deleteClientCert()
            throws IOException, KeyStoreException, KeyStoreCorruptException
    {
        if (ssg.isFederatedGateway())
            throw new IllegalStateException("Federated SSGs may not delete their client certificate");
        synchronized (ssg) {
            KeyStore trustStore = getTrustStore();
            if (trustStore.containsAlias(CLIENT_CERT_ALIAS))
                trustStore.deleteEntry(CLIENT_CERT_ALIAS);
            saveTrustStore();
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            ssg.getRuntime().setCachedClientCert(null);
            ssg.getRuntime().setHaveClientCert(Boolean.FALSE);
            ssg.getRuntime().setPasswordCorrectForPrivateKey(false);
            ssg.getRuntime().keyStore(null);
            ssg.getRuntime().setCachedPrivateKey(null);
        }
    }

    public PrivateKey getClientCertPrivateKey()
            throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException
    {
        if (Thread.holdsLock(ssg))
            throw new IllegalStateException("Must not hold SSG monitor when calling for private key");

        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return trusted.getRuntime().getSsgKeyStoreManager().getClientCertPrivateKey();
        if (ssg.isFederatedGateway()) {
            CertLoader cc = CertLoader.getConfiguredCertLoader();
            if (cc != null) {
                log.log(Level.FINE, "Using preconfigured keystore for federated private key");
                return cc.getPrivateKey();
            }

            throw new RuntimeException("Not supported for WS-Trust federation without custom keystore");
        }

        synchronized (ssg) {
            if (!isClientCertAvailabile())
                return null;
            if (ssg.getRuntime().getCachedPrivateKey() != null)
                return ssg.getRuntime().getCachedPrivateKey();
        }
        PasswordAuthentication pw;
        pw = ssg.getRuntime().getCredentialManager().getCredentialsWithReasonHint(ssg,
                                                                          CredentialManager.ReasonHint.PRIVATE_KEY,
                                                                          false,
                                                                          false);
        if (pw == null) {
            log.finer("No credentials configured -- unable to access private key");
            return null;
        }

        synchronized (ssg) {
            if (!isClientCertAvailabile())
                return null;
            if (ssg.getRuntime().getCachedPrivateKey() != null)
                return ssg.getRuntime().getCachedPrivateKey();
            try {
                PrivateKey gotKey = (PrivateKey) getKeyStore(pw.getPassword()).getKey(CLIENT_CERT_ALIAS, pw.getPassword());
                ssg.getRuntime().setCachedPrivateKey(gotKey);
                ssg.getRuntime().setPasswordCorrectForPrivateKey(true);
                return gotKey;
            } catch (KeyStoreException e) {
                log.log(Level.SEVERE, "impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
                throw new RuntimeException("impossible exception", e);
            } catch (UnrecoverableKeyException e) {
                log.log(Level.SEVERE, "Private key for client cert with Gateway " + ssg + " is unrecoverable with the current password");
                throw new BadCredentialsException(e);
            }
        }
    }

    public boolean isPasswordWorkedForPrivateKey() {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return trusted.getRuntime().getSsgKeyStoreManager().isPasswordWorkedForPrivateKey();
        if (ssg.isFederatedGateway()) {
            if (CertLoader.getConfiguredCertLoader() == null)
                throw new RuntimeException("Not supported for WS-Trust federation without custom keystore");
        }
        return ssg.getRuntime().isPasswordCorrectForPrivateKey();
    }

    protected KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException {
        if (!Thread.holdsLock(ssg))
            throw new IllegalStateException("Caller of getKeyStore must hold the Ssg monitor");
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null) {
            if (Thread.holdsLock(trusted)) // deadlock avoidance; force locking from bottom-to-top
                throw new IllegalStateException("Caller of getKeyStore must not hold the trusted Ssg's monitor");
            synchronized (trusted) {
                return trusted.getRuntime().getSsgKeyStoreManager().getKeyStore(password);
            }
        }
        if (ssg.isFederatedGateway())
            throw new RuntimeException("Not supported for WS-Trust federation");
        if (ssg.getRuntime().keyStore() == null) {
            KeyStore keyStore;
            try {
                keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            } catch (KeyStoreException e) {
                log.log(Level.SEVERE, "Security provider configuration problem", e);
                throw new RuntimeException(e); // can't happen unless VM misconfigured
            }
            FileInputStream fis = null;
            try {
                fis = FileUtils.loadFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
                keyStore.load(fis, password);
            } catch (Exception e) {
                if (e instanceof FileNotFoundException)
                    log.info("Creating new key store " + ssg.getKeyStoreFile() + " for Gateway " + ssg);
                else {
                    log.log(Level.SEVERE, "Unable to load existing key store " + ssg.getKeyStoreFile() + " for Gateway " + ssg, e);
                    throw new KeyStoreCorruptException(e);
                }
                try {
                    keyStore.load(null, password);
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
                    throw new RuntimeException(e1); // can't happen
                }
            } finally {
                if (fis != null)
                    try {
                        fis.close();
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "Impossible IOException while closing an InputStream; will ignore and continue", e);
                    }
            }
            ssg.getRuntime().keyStore(keyStore);
        }
        return ssg.getRuntime().keyStore();
    }

    protected KeyStore getTrustStore() throws KeyStoreCorruptException {
        if (!Thread.holdsLock(ssg))
            throw new IllegalStateException("Caller of getTrustStore must hold the Ssg monitor");
        if (ssg.getRuntime().trustStore() == null) {
            KeyStore trustStore;
            try {
                trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
            } catch (KeyStoreException e) {
                log.log(Level.SEVERE, "Security provider configuration problem", e);
                throw new RuntimeException(e); // can't happen unless VM misconfigured
            }
            FileInputStream fis = null;
            try {
                fis = FileUtils.loadFileSafely(ssg.getTrustStoreFile().getAbsolutePath());
                trustStore.load(fis, TRUSTSTORE_PASSWORD);
            } catch (Exception e) {
                if (e instanceof FileNotFoundException)
                    log.info("Creating new trust store " + ssg.getTrustStoreFile() + " for Gateway " + ssg);
                else {
                    log.log(Level.SEVERE, "Unable to load existing trust store " + ssg.getTrustStoreFile() + " for Gateway " + ssg, e);
                    throw new KeyStoreCorruptException(e);
                }
                try {
                    trustStore.load(null, TRUSTSTORE_PASSWORD);
                } catch (Exception e1) {
                    log.log(Level.SEVERE, "impossible exception", e);
                    throw new RuntimeException(e1); // can't happen
                }
            } finally {
                if (fis != null)
                    try {
                        fis.close();
                    } catch (IOException e) {
                        log.log(Level.SEVERE, "Impossible IOException while closing an InputStream; will ignore and continue", e);
                    }
            }
            ssg.getRuntime().trustStore(trustStore);
        }
        return ssg.getRuntime().trustStore();
    }

    /**
     * Save the Trust Store and optionally Key Store for this Ssg to disk, safely replacing any previous file.
     * The Trust Store must have already been loaded If the Key Store has been loaded then it too will be saved.
     *
     * @throws IllegalStateException  if this SSG has not yet loaded its trust store
     * @throws java.io.IOException            if there was a problem writing the trust store or key store to disk
     */
    private void saveTrustStore() throws IllegalStateException, IOException {
        synchronized (ssg) {
            if (ssg.getRuntime().trustStore() == null)
                throw new IllegalStateException("Gateway " + ssg + " hasn't yet loaded its trust store");

            FileUtils.saveFileSafely(ssg.getTrustStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.getRuntime().trustStore().store(fos, TRUSTSTORE_PASSWORD);
                                                 fos.close();
                                             } catch (KeyStoreException e) {
                                                 throw new IOException("Unable to write trust store for Gateway " + ssg + ": " + e);
                                             } catch (NoSuchAlgorithmException e) {
                                                 throw new IOException("Unable to write trust store for Gateway " + ssg + ": " + e);
                                             } catch (CertificateException e) {
                                                 throw new IOException("Unable to write trust store for Gateway " + ssg + ": " + e);
                                             }
                                         }
                                     });

        }
    }

    private void saveKeyStore(final char[] privateKeyPassword)
            throws IllegalStateException, IOException
    {
        synchronized (ssg) {
            if (ssg.getRuntime().keyStore() == null)
                throw new IllegalStateException("Gateway " + ssg + " hasn't yet loaded its keystore");

            FileUtils.saveFileSafely(ssg.getKeyStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.getRuntime().keyStore().store(fos, privateKeyPassword);
                                                 fos.close();
                                             } catch (KeyStoreException e) {
                                                 throw new IOException("Unable to write KeyStore for Gateway " + ssg + ": " + e);
                                             } catch (NoSuchAlgorithmException e) {
                                                 throw new IOException("Unable to write KeyStore for Gateway " + ssg + ": " + e);
                                             } catch (CertificateException e) {
                                                 throw new IOException("Unable to write KeyStore for Gateway " + ssg + ": " + e);
                                             }
                                         }
                                     });

        }
    }

    public void deleteStores() {
        if (ssg.isFederatedGateway())
            throw new IllegalStateException("Not permitted to delete key stores for a Federated SSG.");

        synchronized (ssg) {
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            FileUtils.deleteFileSafely(ssg.getTrustStoreFile().getAbsolutePath());
            clearCachedKeystoreData();
        }
    }

    private void clearCachedKeystoreData() {
        synchronized (ssg) {
            ssg.getRuntime().keyStore(null);
            ssg.getRuntime().trustStore(null);
            ssg.getRuntime().setCachedPrivateKey(null);
            ssg.getRuntime().setPasswordCorrectForPrivateKey(false);
            ssg.getRuntime().setHaveClientCert(null);
            ssg.getRuntime().setCachedClientCert(null);
            ssg.getRuntime().setCachedServerCert(null);
            ssg.getRuntime().resetSslContext();
        }
    }

    public void saveSsgCertificate(X509Certificate cert)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException
    {
        synchronized (ssg) {
            log.info("Saving Gateway server certificate to disk");
            ssg.getRuntime().setCachedServerCert(null);
            cert = convertToSunCertificate(cert);
            KeyStore trustStore = getTrustStore();
            if (trustStore.containsAlias(SERVER_CERT_ALIAS))
                trustStore.deleteEntry(SERVER_CERT_ALIAS);
            trustStore.setCertificateEntry(SERVER_CERT_ALIAS, cert);
            saveTrustStore();
            ssg.getRuntime().setCachedServerCert(cert);
        }
    }

    public void saveClientCertificate(PrivateKey privateKey, X509Certificate cert,
                                             char[] privateKeyPassword)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException
    {
        if (ssg.isFederatedGateway())
            throw new IllegalArgumentException("Unable to save client certificate for Federated Gateway.");
        log.info("Saving client certificate to disk");
        synchronized (ssg) {
            ssg.getRuntime().setCachedClientCert(null);
            cert = convertToSunCertificate(cert);
            KeyStore trustStore = getTrustStore();
            if (trustStore.containsAlias(CLIENT_CERT_ALIAS))
                trustStore.deleteEntry(CLIENT_CERT_ALIAS);
            trustStore.setCertificateEntry(CLIENT_CERT_ALIAS, cert);

            KeyStore keyStore = getKeyStore(privateKeyPassword);
            if (keyStore.containsAlias(CLIENT_CERT_ALIAS))
                keyStore.deleteEntry(CLIENT_CERT_ALIAS);
            keyStore.setKeyEntry(CLIENT_CERT_ALIAS, privateKey, privateKeyPassword, new java.security.cert.Certificate[] { cert });

            saveKeyStore(privateKeyPassword);
            saveTrustStore();
            ssg.getRuntime().setHaveClientCert(Boolean.TRUE);
            ssg.getRuntime().setCachedPrivateKey(privateKey);
            ssg.getRuntime().setCachedClientCert(cert);
        }
    }

    public void obtainClientCertificate(PasswordAuthentication credentials)
            throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException,
            CertificateAlreadyIssuedException, IOException
    {
        if (credentials == null)
            throw new BadCredentialsException("Unable to apply for client certificate without credentials");
        if (ssg.isFederatedGateway())
            throw new IllegalArgumentException("Unable to obtain client certificate for Federated Gateway.");
        try {
            log.info("Generating new RSA key pair (could take several seconds)...");
            ssg.getRuntime().getCredentialManager().notifyLengthyOperationStarting(ssg, "Generating new client certificate...");
            obtainClientCertificate(credentials, JceProvider.generateRsaKeyPair());
        } finally {
            ssg.getRuntime().getCredentialManager().notifyLengthyOperationFinished(ssg);
        }
    }

    /**
     * Generate a Certificate Signing Request, and apply to the Ssg for a certificate for the
     * current user.  If this method returns, the certificate will have been downloaded and saved
     * locally, and the SSL context for this Client Proxy will have been reinitialized.
     *
     * @param credentials  the username and password to use for the application
     * @param keyPair  the public and private keys to use.  Get this from JceProvider
     * @throws com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException if we haven't yet discovered the Ssg server cert
     * @throws java.security.GeneralSecurityException   if there was a problem making the CSR
     * @throws java.security.GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws java.io.IOException                if there was a network problem
     * @throws com.l7tech.proxy.datamodel.exceptions.BadCredentialsException    if the SSG rejected the credentials we provided
     * @throws com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException if the SSG has already issued the client certificate for this account
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException   if the keystore is corrupt
     */
    private void obtainClientCertificate(PasswordAuthentication credentials, KeyPair keyPair)
            throws  ServerCertificateUntrustedException, GeneralSecurityException, IOException,
                    BadCredentialsException, CertificateAlreadyIssuedException, KeyStoreCorruptException
    {
        CertificateRequest csr = JceProvider.makeCsr(ssg.getUsername(), keyPair);

        X509Certificate caCert = getServerCert();
        if (caCert == null) {
            CurrentSslPeer.set(ssg);
            throw new ServerCertificateUntrustedException(); // fault in the SSG cert
        }
        X509Certificate cert = SslUtils.obtainClientCertificate(ssg,
                                                                credentials.getUserName(),
                                                                credentials.getPassword(),
                                                                csr,
                                                                caCert);
        // make sure private key is stored on disk encrypted with the password that was used to obtain it
        saveClientCertificate(keyPair.getPrivate(), cert, credentials.getPassword());
        ssg.getRuntime().resetSslContext(); // reset cached SSL state
        return;
    }

    public String lookupClientCertUsername() {
        boolean badKeystore = false;

        try {
            if (isClientCertAvailabile()) {
                X509Certificate cert = null;
                cert = getClientCert();
                return CertUtils.extractCommonNameFromClientCertificate(cert);
            }
        } catch (IllegalArgumentException e) {
            // bad client certificate format
            badKeystore = true; // TODO This will fail with arbitrary third-party PKI not using CN=username
        } catch (KeyStoreCorruptException e) {
            badKeystore = true;
        }

        if (badKeystore) {
            try {
                ssg.getRuntime().handleKeyStoreCorrupt();
                // FALLTHROUGH -- continue, with newly-blank keystore
            } catch (OperationCanceledException e1) {
                // FALLTHROUGH -- continue, pretending we had no keystore
            }
        }

        return null;
    }

    public void importClientCertificate(File certFile,
                                        char[] pass,
                                        AliasPicker aliasPicker,
                                        char[] ssgPassword)
            throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException
    {
        if (ssg.isFederatedGateway())
            throw new IllegalArgumentException("Unable to import client certificate for Federated Gateway.");
        KeyStore ks;
        try {
            ks = KeyStore.getInstance(KEYSTORE_TYPE);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e); // shouldn't happen
        }
        ks.load(new FileInputStream(certFile), pass);
        Certificate[] chainToImport = null;
        Key key = null;
        try {
            List aliases = new ArrayList();
            Enumeration aliasEnum = ks.aliases();
            while (aliasEnum.hasMoreElements()) {
                String alias = (String)aliasEnum.nextElement();
                if (ks.getKey(alias, pass) != null)
                    aliases.add(alias);
            }
            String alias = null;
            if (aliases.size() > 1 && aliasPicker != null) {
                alias = aliasPicker.selectAlias((String[])aliases.toArray(new String[0]));
                if (alias == null)
                    throw new AliasNotFoundException("The AliasPicker did not return an alias.");
            } else if (aliases.size() > 0)
                alias = (String)aliases.get(0);
            if (alias == null)
                throw new IOException("The specified file does not contain any client certificates.");
            chainToImport = ks.getCertificateChain(alias);
            if (chainToImport == null || chainToImport.length < 1)
                throw new AliasNotFoundException("The specified file does not contain a certificate chain for alias " + alias);
            key = ks.getKey(alias, pass);
            if (key == null || !(key instanceof PrivateKey))
                throw new AliasNotFoundException("The specified alias does not contain a private key.");
        } catch (KeyStoreException e) {
            throw new CausedIOException("Unable to read aliases from keystore", e);
        }

        saveClientCertificate((PrivateKey)key, (X509Certificate)chainToImport[0], ssgPassword);
    }
}
