/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.FileUtils;
import com.l7tech.proxy.datamodel.exceptions.*;
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
 * Maintain the SSG-specific KeyStores.
 */
public class SsgKeyStoreManager {
    private static final Logger log = Logger.getLogger(SsgKeyStoreManager.class.getName());
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

    /**
     * Very quickly check if a client certificate is available for the specified SSG.
     * The first time this is called for a given SSG it will take the time to load the KeyStore.
     * May be slow if there is a problem with the key store.  Even if this method returns
     * true, the client cert might still require credentials to unlock its private key.
     * To check if the cert has already been unlocked, use isClientCertUnlocked() instead.
     *
     * @param ssg the ssg to look at
     * @return true if we have a client cert for this ssg
     * @throws KeyStoreCorruptException if the trust store is damaged
     */
    private static boolean isClientCertAvailabile(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return isClientCertAvailabile(trusted);
        if (ssg.isFederatedGateway())
            throw new RuntimeException("Not supported for WS-Trust federation");
        if (ssg.getRuntime().getHaveClientCert() == null)
            ssg.getRuntime().setHaveClientCert(getClientCert(ssg) == null ? Boolean.FALSE : Boolean.TRUE);
        return ssg.getRuntime().getHaveClientCert().booleanValue();
    }

    /**
     * Very quickly check if a client cert private key has already been unlocked for
     * the specified SSG.  The first time this is called for a given SSG it will
     * take the tiem to load the KeyStore.  May be slow if there are problems with
     * the key store.  If this method returns true, the client cert private key
     * is already cached in memory.
     * <p>
     * TODO get rid of this somehow
     *
     * @param ssg the ssg to look at
     * @return true if we have a client cert for this SSG and have already unlocked the private key
     * @throws KeyStoreCorruptException if the trust store or key store is damaged
     */
    public static boolean isClientCertUnlocked(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return isClientCertUnlocked(trusted);
        if (ssg.isFederatedGateway())
            return false;
        if (!isClientCertAvailabile(ssg)) return false;
        return ssg.getRuntime().getCachedPrivateKey() != null;
    }

    /**
     * Convert a generic certificate, which may be a BouncyCastle implementation, into a standard Sun
     * implementation.
     * @param generic  the certificate to convert, which may be null.
     * @return a new instance from the default X.509 CertificateFactory, or null if generic was null.
     * @throws CertificateException if the generic certificate could not be processed
     */
    private static X509Certificate convertToSunCertificate(X509Certificate generic) throws CertificateException {
        if (generic == null) return null;
        final byte[] encoded = generic.getEncoded();
        return CertUtils.decodeCert(encoded);
    }

    /**
     * Look up the server certificate for this SSG.
     *
     * @param ssg  the Ssg whose KeyStore to examine
     * @return the server certificate (nominally the CA cert), or null if it hasn't yet been discovered.
     */
    static X509Certificate getServerCert(Ssg ssg) throws KeyStoreCorruptException {
        try {
            X509Certificate cert = ssg.getRuntime().getCachedServerCert();
            if (cert != null) return cert;
            synchronized (ssg) {
                cert = (X509Certificate) getTrustStore(ssg).getCertificate(SERVER_CERT_ALIAS);
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

    /**
     * Look up our client certificate on this SSG.  Note that the password is not needed to do this.
     *
     * @param ssg  the Ssg whose KeyStore to examine
     * @return our client certificate, or null if we haven't yet applied for one
     */
    static X509Certificate getClientCert(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getClientCert(trusted);
        if (ssg.isFederatedGateway()) {
            log.log(Level.FINE, "No client cert for Federated Gateway; returning null");
            return null;
        }
        try {
            X509Certificate cert = ssg.getRuntime().getCachedClientCert();
            if (cert != null) return cert;
            synchronized (ssg) {
                cert = (X509Certificate) getTrustStore(ssg).getCertificate(CLIENT_CERT_ALIAS);
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

    /**
     * Delete our client certificate for this SSG.  The KeyStore will be persisted to disk afterward.
     *
     * @param ssg
     * @throws IOException  if there was a problem saving the KeyStore to disk
     * @throws KeyStoreException  if the private key entry cannot be removed.  I don't think this ever happens, but
     *                            KeyStore.deleteEntry() contract states that some provider might someday throw this.
     */
    public static void deleteClientCert(Ssg ssg)
            throws IOException, KeyStoreException, KeyStoreCorruptException
    {
        if (ssg.isFederatedGateway())
            throw new IllegalStateException("Federated SSGs may not delete their client certificate");
        synchronized (ssg) {
            KeyStore trustStore = getTrustStore(ssg);
            if (trustStore.containsAlias(CLIENT_CERT_ALIAS))
                trustStore.deleteEntry(CLIENT_CERT_ALIAS);
            saveTrustStore(ssg);
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            ssg.getRuntime().setCachedClientCert(null);
            ssg.getRuntime().setHaveClientCert(Boolean.FALSE);
            ssg.getRuntime().setPasswordCorrectForPrivateKey(false);
            ssg.getRuntime().keyStore(null);
            ssg.getRuntime().setCachedPrivateKey(null);
        }
    }

    /**
     * Get the private key for our client certificate with this SSG.  Caller *must not* hold the SSG monitor
     * when calling this method.
     *
     * @param ssg   the SSG whose KeyStore to examine
     * @return      The PrivateKey, or null if we haven't yet applied for a client certificate with this SSG.
     * @throws NoSuchAlgorithmException   if the VM is misconfigured, or the keystore was tampered with
     * @throws BadCredentialsException    if the SSG password does not match the password used to encrypt the key.
     * @throws OperationCanceledException if the user canceled the password prompt
     */
    static PrivateKey getClientCertPrivateKey(Ssg ssg)
            throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException
    {
        if (Thread.holdsLock(ssg))
            throw new IllegalStateException("Must not hold SSG monitor when calling for private key");

        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getClientCertPrivateKey(trusted);
        if (ssg.isFederatedGateway())
            throw new RuntimeException("Not supported for WS-Trust federation");

        synchronized (ssg) {
            if (!isClientCertAvailabile(ssg))
                return null;
            if (ssg.getRuntime().getCachedPrivateKey() != null)
                return ssg.getRuntime().getCachedPrivateKey();
        }
        PasswordAuthentication pw;
        pw = Managers.getCredentialManager().getCredentialsWithReasonHint(ssg,
                                                                          CredentialManager.ReasonHint.PRIVATE_KEY,
                                                                          false,
                                                                          false);
        if (pw == null) {
            log.finer("No credentials configured -- unable to access private key");
            return null;
        }

        synchronized (ssg) {
            if (!isClientCertAvailabile(ssg))
                return null;
            if (ssg.getRuntime().getCachedPrivateKey() != null)
                return ssg.getRuntime().getCachedPrivateKey();
            try {
                PrivateKey gotKey = (PrivateKey) getKeyStore(ssg, pw.getPassword()).getKey(CLIENT_CERT_ALIAS, pw.getPassword());
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

    /**
     * Check if the current SSG password matches the keystore private key password.
     * <p>
     * TODO remove this somehow, it sucks
     */
    public static boolean isPasswordWorkedForPrivateKey(Ssg ssg) {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return isPasswordWorkedForPrivateKey(trusted);
        if (ssg.isFederatedGateway())
            throw new RuntimeException("Not supported for WS-Trust federation");
        return ssg.getRuntime().isPasswordCorrectForPrivateKey();
    }

    /**
     * Obtain a key store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new keystore will be created in memory.  Call saveStores() to safely
     * save the keystore back to disk.
     * <p>
     * To protect the returned keystore from concurrent modification, the caller must hold the ssg monitor
     * before calling this method.
     *
     * @param ssg The Ssg whose keystore we are setting up.  Must not be null.
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the key store is damaged
     * @throws IllegalStateException if the current thread does not already hold the ssg monitor.
     */
    private static KeyStore getKeyStore(Ssg ssg, char[] password) throws KeyStoreCorruptException {
        if (!Thread.holdsLock(ssg))
            throw new IllegalStateException("Caller of getKeyStore must hold the Ssg monitor");
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null) {
            if (Thread.holdsLock(trusted)) // deadlock avoidance; force locking from bottom-to-top
                throw new IllegalStateException("Caller of getKeyStore must not hold the trusted Ssg's monitor");
            synchronized (trusted) {
                return getKeyStore(trusted, password);
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

    /**
     * Obtain a trust store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new trust store will be created in memory.  Call saveTrustStore() to safely
     * save the trust store back to disk.
     * <p>
     * To protect the returned trust store from concurrent modification, the caller must already hold the
     * Ssg monitor before calling this method.
     *
     * @param ssg The Ssg whose trust store we are setting up.  Must not be null.
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the key store is damaged
     */
    private static KeyStore getTrustStore(Ssg ssg) throws KeyStoreCorruptException {
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
     * @param ssg  the Ssg whose Trust Store and Key Store to save.
     * @throws IllegalStateException  if this SSG has not yet loaded its trust store
     * @throws IOException            if there was a problem writing the trust store or key store to disk
     */
    private static void saveTrustStore(final Ssg ssg) throws IllegalStateException, IOException {
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

    private static void saveKeyStore(final Ssg ssg, final char[] privateKeyPassword)
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

    /**
     * Delete any keystore that might exist for this Ssg.
     *
     * @param ssg The SSG whose keystore is to be deleted.
     */
    public static void deleteStores(Ssg ssg) {
        if (ssg.isFederatedGateway())
            throw new IllegalStateException("Not permitted to delete key stores for a Federated SSG.");

        synchronized (ssg) {
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            FileUtils.deleteFileSafely(ssg.getTrustStoreFile().getAbsolutePath());
            clearCachedKeystoreData(ssg);
        }
    }

    private static void clearCachedKeystoreData(Ssg ssg) {
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

    /**
     * Set the Ssg certificate for this Ssg.
     *
     * @param ssg  the SSG whose keystore to save
     * @param cert  The certificate to save.  Expected to be the SSG CA public key.
     * @throws KeyStoreException if the KeyStore is corrupt, or "the operation failed for some other reason"
     * @throws IOException       if there was a problem writing the keystore to disk
     */
    public static void saveSsgCertificate(final Ssg ssg, X509Certificate cert)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException
    {
        synchronized (ssg) {
            log.info("Saving Gateway server certificate to disk");
            ssg.getRuntime().setCachedServerCert(null);
            cert = convertToSunCertificate(cert);
            KeyStore trustStore = getTrustStore(ssg);
            if (trustStore.containsAlias(SERVER_CERT_ALIAS))
                trustStore.deleteEntry(SERVER_CERT_ALIAS);
            trustStore.setCertificateEntry(SERVER_CERT_ALIAS, cert);
            saveTrustStore(ssg);
            ssg.getRuntime().setCachedServerCert(cert);
        }
    }

    /**
     * Set our client certificate for this Ssg.  Caller must ensure that the correct password is set
     * on the SSG before calling this, since the password will be used to protect the private key.  The
     * safest way to ensure this is to use the same password that was used to successfully apply for
     * the client certificate you are about to save.
     *
     * @param ssg  the Ssg whose KeyStore to save
     * @param privateKey   the RSA private key corresponding to the public key in the certificate
     * @param cert    the certificate, signed by the SSG CA, and whose public key corrsponds to getCachedPrivateKey
     * @param privateKeyPassword the pass phrase with which to encrypt the private key
     * @throws IllegalArgumentException if the specified SSG has not yet had a password set
     * @throws KeyStoreException   if the key entry could not be saved for obscure reasons
     * @throws IOException  if there was a problem writing the keystore to disk
     */
    public static void saveClientCertificate(final Ssg ssg, PrivateKey privateKey, X509Certificate cert,
                                             char[] privateKeyPassword)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException
    {
        if (ssg.isFederatedGateway())
            throw new IllegalArgumentException("Unable to save client certificate for Federated Gateway.");
        log.info("Saving client certificate to disk");
        synchronized (ssg) {
            ssg.getRuntime().setCachedClientCert(null);
            cert = convertToSunCertificate(cert);
            KeyStore trustStore = getTrustStore(ssg);
            if (trustStore.containsAlias(CLIENT_CERT_ALIAS))
                trustStore.deleteEntry(CLIENT_CERT_ALIAS);
            trustStore.setCertificateEntry(CLIENT_CERT_ALIAS, cert);

            KeyStore keyStore = getKeyStore(ssg, privateKeyPassword);
            if (keyStore.containsAlias(CLIENT_CERT_ALIAS))
                keyStore.deleteEntry(CLIENT_CERT_ALIAS);
            keyStore.setKeyEntry(CLIENT_CERT_ALIAS, privateKey, privateKeyPassword, new Certificate[] { cert });

            saveKeyStore(ssg, privateKeyPassword);
            saveTrustStore(ssg);
            ssg.getRuntime().setHaveClientCert(Boolean.TRUE);
            ssg.getRuntime().setCachedPrivateKey(privateKey);
            ssg.getRuntime().setCachedClientCert(cert);
        }
    }

    /**
     * Download and install the SSG certificate.  If this completes successfully, the
     * next attempt to connect to the SSG via SSL should at least get past the SSL handshake.  Uses the
     * specified credentials for the download.
     *
     * @param ssg the ssg whose cert to discover. may not be null
     * @param credentials the credentials for this SSG, if known, to enable automatic certificate trust, or null to
     *                    force manual certificate trust.
     * @throws IOException if there was a network problem downloading the server cert
     * @throws IOException if there was a problem reading or writing the keystore for this SSG
     * @throws BadCredentialsException if the downloaded cert could not be verified with the SSG username and password
     * @throws OperationCanceledException if credentials were needed but the user declined to enter them
     * @throws GeneralSecurityException for miscellaneous and mostly unlikely certificate or key store problems
     */
    public static void installSsgServerCertificate(Ssg ssg, PasswordAuthentication credentials)
            throws IOException, BadCredentialsException, OperationCanceledException, GeneralSecurityException, KeyStoreCorruptException
    {
        log.log(Level.FINER, "Discovering server certificate for Gateway " + ssg + " (" + ssg.getLocalEndpoint() + ")");
        CertificateDownloader cd = new CertificateDownloader(ssg.getRuntime().getHttpClient(),
                                                             ssg.getServerUrl(),
                                                             credentials != null ? credentials.getUserName() : null,
                                                             credentials != null ? credentials.getPassword() : null);

        X509Certificate gotCert = cd.downloadCertificate();
        boolean isValidated = cd.isValidCert();
        if (!isValidated) {
            if (cd.isUncheckablePassword()) {
                // The username was known to the SSG, but at least one of the accounts with that username
                // had an uncheckable password (either unavailable or hashed in an unsupported way).
                Managers.getCredentialManager().notifySslCertificateUntrusted(ssg, "the Gateway " + ssg, gotCert);
            } else
                throw new BadCredentialsException("The downloaded Gateway server certificate could not be verified with the current user name and password.");
        }

        saveSsgCertificate(ssg, gotCert);
    }


    /**
     * Generate a Certificate Signing Request, and apply to the Ssg for a certificate for the
     * current user.  If this method returns, the certificate will have been downloaded and saved
     * locally, and the SSL context for this Client Proxy will have been reinitialized.
     *
     * It is an error to pass in a Federated SSG; pass in the Trusted SSG (and it's credentials) instead.
     *
     * @param ssg   the Gateway on to which we are sending our application
     * @param credentials  the username and password to use for the application
     * @throws ServerCertificateUntrustedException if we haven't yet discovered the Ssg server cert
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws BadCredentialsException    if the SSG rejected the credentials we provided
     * @throws com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException if the SSG has already issued the client certificate for this account
     * @throws KeyStoreCorruptException   if the keystore is corrupt
     */
    public static void obtainClientCertificate(Ssg ssg, PasswordAuthentication credentials)
            throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException,
                   CertificateAlreadyIssuedException, IOException
    {
        if (credentials == null)
            throw new BadCredentialsException("Unable to apply for client certificate without credentials");
        if (ssg.isFederatedGateway())
            throw new IllegalArgumentException("Unable to obtain client certificate for Federated Gateway.");
        try {
            log.info("Generating new RSA key pair (could take several seconds)...");
            Managers.getCredentialManager().notifyLengthyOperationStarting(ssg, "Generating new client certificate...");
            obtainClientCertificate(ssg, credentials, JceProvider.generateRsaKeyPair());
        } finally {
            Managers.getCredentialManager().notifyLengthyOperationFinished(ssg);
        }
    }


    /**
     * Generate a Certificate Signing Request, and apply to the Ssg for a certificate for the
     * current user.  If this method returns, the certificate will have been downloaded and saved
     * locally, and the SSL context for this Client Proxy will have been reinitialized.
     *
     * @param ssg   the Gateway on to which we are sending our application
     * @param credentials  the username and password to use for the application
     * @param keyPair  the public and private keys to use.  Get this from JceProvider
     * @throws ServerCertificateUntrustedException if we haven't yet discovered the Ssg server cert
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws BadCredentialsException    if the SSG rejected the credentials we provided
     * @throws com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException if the SSG has already issued the client certificate for this account
     * @throws KeyStoreCorruptException   if the keystore is corrupt
     */
    private static void obtainClientCertificate(Ssg ssg, PasswordAuthentication credentials, KeyPair keyPair)
            throws  ServerCertificateUntrustedException, GeneralSecurityException, IOException,
                    BadCredentialsException, CertificateAlreadyIssuedException, KeyStoreCorruptException
    {
        CertificateRequest csr = JceProvider.makeCsr(ssg.getUsername(), keyPair);

        X509Certificate caCert = getServerCert(ssg);
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
        saveClientCertificate(ssg, keyPair.getPrivate(), cert, credentials.getPassword());
        ssg.getRuntime().resetSslContext(); // reset cached SSL state
        return;
    }

    /** Return the username in our client certificate, or null if we don't have an active client cert. */
    public static String lookupClientCertUsername(Ssg ssg) {
        boolean badKeystore = false;

        try {
            if (isClientCertAvailabile(ssg)) {
                X509Certificate cert = null;
                cert = getClientCert(ssg);
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

    /** Exception thrown if the desired alias is not found in a keystore file. */
    public static class AliasNotFoundException extends Exception {
        public AliasNotFoundException() {
        }

        public AliasNotFoundException(String message) {
            super(message);
        }

        public AliasNotFoundException(Throwable cause) {
            super(cause);
        }

        public AliasNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Caller passes an instance of this to importClientCertificate if they wish to present the user with a list of aliases in a file. */
    public interface AliasPicker {
        /**
         * @return the preferred alias.  May not be null.
         * @throws AliasNotFoundException if none of the available aliases look good.
         */
        String selectAlias(String[] options) throws AliasNotFoundException;
    }
    
    /**
     * Import the client certificate for the specified Ssg from the specified file, using the specified pass phrase.
     * If this method returns, the certificate was imported successfully.
     *
     * @param ssg  the SSG whose client certificate is to be set or replaced
     * @param certFile the PKCS#12 file from which to read the new cert
     * @param pass the pass phrase to use when reading the file
     * @param aliasPicker optional AliasPicker in case there is more than one certificate in the file.  If not provided,
     *                    will always select the very first cert.
     * @throws KeyStoreCorruptException if the Ssg keystore is damaged or could not be writted using ssgPassword
     * @throws IOException if there is a problem reading the file or the file does not contain any private keys
     * @throws GeneralSecurityException if the file can't be decrypted or the imported certificate can't be saved
     * @throws AliasNotFoundException if the specified alias was not found or did not contain both a cert chain and a private key
     */
    public static void importClientCertificate(Ssg ssg,
                                                  File certFile,
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

        saveClientCertificate(ssg, (PrivateKey)key, (X509Certificate)chainToImport[0], ssgPassword);
    }
}
