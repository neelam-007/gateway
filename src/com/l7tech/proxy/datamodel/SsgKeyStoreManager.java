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
 *
 * User: mike
 * Date: Jul 30, 2003
 * Time: 7:36:04 PM
 */
public class SsgKeyStoreManager {
    private static final Logger log = Logger.getLogger(SsgKeyStoreManager.class.getName());
    private static final String CLIENT_CERT_ALIAS = "clientCert";
    private static final String SERVER_CERT_ALIAS = "serverCert";

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
     * May be slow if there is a problem with the key store.
     *
     * @param ssg the ssg to look at
     * @return true if we have a client cert for this ssg
     * @throws KeyStoreCorruptException if the trust store is damaged
     */
    public static boolean isClientCertAvailabile(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return isClientCertAvailabile(trusted);
        if (ssg.haveClientCert() == null)
            ssg.haveClientCert(getClientCert(ssg) == null ? Boolean.FALSE : Boolean.TRUE);
        return ssg.haveClientCert().booleanValue();
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
    public static X509Certificate getServerCert(Ssg ssg) throws KeyStoreCorruptException {
        try {
            X509Certificate cert = ssg.serverCert();
            if (cert != null) return cert;
            cert = (X509Certificate) getTrustStore(ssg).getCertificate(SERVER_CERT_ALIAS);
            cert = convertToSunCertificate(cert);
            ssg.serverCert(cert);
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
    public static X509Certificate getClientCert(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getClientCert(trusted);
        try {
            X509Certificate cert = ssg.clientCert();
            if (cert != null) return cert;
            cert = (X509Certificate) getTrustStore(ssg).getCertificate(CLIENT_CERT_ALIAS);
            cert = convertToSunCertificate(cert);
            ssg.clientCert(cert);
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
        if (ssg.getTrustedGateway() != null)
            throw new IllegalStateException("Federated SSGs may not delete their client certificate");
        synchronized (ssg) {
            KeyStore trustStore = getTrustStore(ssg);
            if (trustStore.containsAlias(CLIENT_CERT_ALIAS))
                trustStore.deleteEntry(CLIENT_CERT_ALIAS);
            saveTrustStore(ssg);
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            ssg.clientCert(null);
            ssg.haveClientCert(Boolean.FALSE);
            ssg.passwordWorkedForPrivateKey(false);
            ssg.keyStore(null);
            ssg.privateKey(null);
        }
    }

    /**
     * Get the entire certificate chain for our client certificate.  Typically this will have only two certificates
     * in it: our client cert first, and then the SSG's CA cert.  Only X.509 certificates are returned.
     *
     * @param ssg
     * @return
     */
    public static X509Certificate[] getClientCertificateChain(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getClientCertificateChain(trusted);

        Certificate[] certs;
        try {
            certs = getTrustStore(ssg).getCertificateChain(CLIENT_CERT_ALIAS);
        } catch (KeyStoreException e) {
            log.log(Level.SEVERE, "impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
        }
        X509Certificate[] x5certs = new X509Certificate[certs.length];
        for (int i = 0; i < certs.length; i++) {
            Certificate cert = certs[i];
            if (cert instanceof X509Certificate)
                x5certs[i] = (X509Certificate) cert;
            else {
                String msg = "Stored client cert which is not X509Certificate: " + cert;
                log.log(Level.SEVERE, msg);
                throw new KeyStoreCorruptException(msg);
            }
        }
        return x5certs;
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
    public static PrivateKey getClientCertPrivateKey(Ssg ssg)
            throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException
    {
        if (Thread.holdsLock(ssg))
            throw new IllegalStateException("Must not hold SSG monitor when calling for private key");

        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getClientCertPrivateKey(trusted);

        synchronized (ssg) {
            if (!isClientCertAvailabile(ssg))
                return null;
            if (ssg.privateKey() != null)
                return ssg.privateKey();
        }
        PasswordAuthentication pw;
        pw = Managers.getCredentialManager().getCredentials(ssg);
        synchronized (ssg) {
            if (!isClientCertAvailabile(ssg))
                return null;
            if (ssg.privateKey() != null)
                return ssg.privateKey();
            try {
                PrivateKey gotKey = (PrivateKey) getKeyStore(ssg, pw.getPassword()).getKey(CLIENT_CERT_ALIAS, pw.getPassword());
                ssg.privateKey(gotKey);
                ssg.passwordWorkedForPrivateKey(true);
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
     */
    public static boolean isPasswordWorkedForPrivateKey(Ssg ssg) {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return isPasswordWorkedForPrivateKey(trusted);
        return ssg.passwordWorkedForPrivateKey();
    }

    /**
     * Get the public key for our client certificate with this SSG.
     *
     * @param ssg   the SSG whose KeyStore to examine
     * @return our public key, or null if we haven't yet applied for a client cert with this Ssg.
     */
    public static PublicKey getClientCertPublicKey(Ssg ssg) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getClientCertPublicKey(trusted);

        if (!isClientCertAvailabile(ssg))
            return null;
        return getClientCert(ssg).getPublicKey();
    }

    /**
     * Obtain a key store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new keystore will be created in memory.  Call saveStores() to safely
     * save the keystore back to disk.
     *
     * @param ssg The Ssg whose keystore we are setting up.  Must not be null.
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the key store is damaged
     */
    public static KeyStore getKeyStore(Ssg ssg, char[] password) throws KeyStoreCorruptException {
        Ssg trusted = ssg.getTrustedGateway();
        if (trusted != null)
            return getKeyStore(trusted, password);
        synchronized (ssg) {
            if (ssg.keyStore() == null) {
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
                ssg.keyStore(keyStore);
            }
            return ssg.keyStore();
        }
    }

    /**
     * Obtain a trust store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new trust store will be created in memory.  Call saveTrustStore() to safely
     * save the trust store back to disk.
     *
     * @param ssg The Ssg whose trust store we are setting up.  Must not be null.
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the key store is damaged
     */
    private static KeyStore getTrustStore(Ssg ssg) throws KeyStoreCorruptException {
        synchronized (ssg) {
            if (ssg.trustStore() == null) {
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
                ssg.trustStore(trustStore);
            }
            return ssg.trustStore();
        }
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
            if (ssg.trustStore() == null)
                throw new IllegalStateException("Gateway " + ssg + " hasn't yet loaded its trust store");

            FileUtils.saveFileSafely(ssg.getTrustStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.trustStore().store(fos, TRUSTSTORE_PASSWORD);
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
            if (ssg.keyStore() == null)
                throw new IllegalStateException("Gateway " + ssg + " hasn't yet loaded its keystore");

            FileUtils.saveFileSafely(ssg.getKeyStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.keyStore().store(fos, privateKeyPassword);
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
        if (ssg.getTrustedGateway() != null)
            throw new IllegalStateException("Not permitted to delete key stores for a Federated SSG.");

        synchronized (ssg) {
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            FileUtils.deleteFileSafely(ssg.getTrustStoreFile().getAbsolutePath());
            clearCachedKeystoreData(ssg);
        }
    }

    private static void clearCachedKeystoreData(Ssg ssg) {
        synchronized (ssg) {
            ssg.keyStore(null);
            ssg.privateKey(null);
            ssg.passwordWorkedForPrivateKey(false);
            ssg.haveClientCert(null);
            ssg.clientCert(null);
            ssg.serverCert(null);
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
            ssg.serverCert(null);
            cert = convertToSunCertificate(cert);
            KeyStore trustStore = getTrustStore(ssg);
            if (trustStore.containsAlias(SERVER_CERT_ALIAS))
                trustStore.deleteEntry(SERVER_CERT_ALIAS);
            trustStore.setCertificateEntry(SERVER_CERT_ALIAS, cert);
            saveTrustStore(ssg);
            ssg.serverCert(cert);
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
     * @param cert    the certificate, signed by the SSG CA, and whose public key corrsponds to privateKey
     * @param privateKeyPassword the pass phrase with which to encrypt the private key
     * @throws IllegalArgumentException if the specified SSG has not yet had a password set
     * @throws KeyStoreException   if the key entry could not be saved for obscure reasons
     * @throws IOException  if there was a problem writing the keystore to disk
     */
    public static void saveClientCertificate(final Ssg ssg, PrivateKey privateKey, X509Certificate cert,
                                             char[] privateKeyPassword)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException
    {
        if (ssg.getTrustedGateway() != null)
            throw new IllegalArgumentException("Unable to save client certificate for Federated Gateway.");
        log.info("Saving client certificate to disk");
        synchronized (ssg) {
            ssg.clientCert(null);
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
            ssg.haveClientCert(Boolean.TRUE);
            ssg.privateKey(privateKey);
            ssg.clientCert(cert);
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
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             credentials != null ? credentials.getUserName() : null,
                                                             credentials != null ? credentials.getPassword() : null);

        boolean isValidated = cd.downloadCertificate();
        if (!isValidated) {
            if (cd.isUncheckablePassword()) {
                // The username was known to the SSG, but at least one of the accounts with that username
                // had an uncheckable password (either unavailable or hashed in an unsupported way).
                Managers.getCredentialManager().notifySsgCertificateUntrusted(ssg, cd.getCertificate());
            } else
                throw new BadCredentialsException("The downloaded Gateway server certificate could not be verified with the current username and password.");
        }

        saveSsgCertificate(ssg, cd.getCertificate());
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
        if (ssg.getTrustedGateway() != null)
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
            CurrentRequest.setPeerSsg(ssg);
            throw new ServerCertificateUntrustedException(); // fault in the SSG cert
        }
        X509Certificate cert = SslUtils.obtainClientCertificate(ssg,
                                                                credentials.getUserName(),
                                                                credentials.getPassword(),
                                                                csr,
                                                                caCert);
        // make sure private key is stored on disk encrypted with the password that was used to obtain it
        saveClientCertificate(ssg, keyPair.getPrivate(), cert, credentials.getPassword());
        ssg.resetSslContext(); // reset cached SSL state
        return;
    }

    /** Caller passes an instance of this to importClientCertificate if they wish to present the user with a list of aliases in a file. */
    public interface AliasPicker {
        /** @return the preferred alias, or null if none look good. */
        String selectAlias(String[] options);
    }
    
    /**
     * Import the client certificate for the specified Ssg from the specified file, using the specified pass phrase.
     * @param ssg  the SSG whose client certificate is to be set or replaced
     * @param certFile the PKCS#12 file from which to read the new cert
     * @param pass the pass phrase to use when reading the file
     * @param aliasPicker optional AliasPicker in case there is more than one certificate in the file.  If not provided,
     *                    will always select the very first cert.
     * @return true if a certificate was imported successfully; false if operation canceled, but not due to an error.
     * @throws KeyStoreCorruptException if the Ssg keystore is damaged or could not be writted using ssgPassword.
     */
    public static boolean importClientCertificate(Ssg ssg,
                                                  File certFile,
                                                  char[] pass,
                                                  AliasPicker aliasPicker,
                                                  char[] ssgPassword)
            throws IOException, GeneralSecurityException, KeyStoreCorruptException
    {
        if (ssg.getTrustedGateway() != null)
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
                    return false;
            } else if (aliases.size() > 0)
                alias = (String)aliases.get(0);
            if (alias == null)
                throw new IOException("The specified file does not contain any client certificates.");
            chainToImport = ks.getCertificateChain(alias);
            if (chainToImport == null || chainToImport.length < 1)
                throw new IOException("The specified alias does not contain a certificate chain."); // shouldn't happen
            key = ks.getKey(alias, pass);
            if (key == null || !(key instanceof PrivateKey))
                throw new IOException("The specified alias does not contain a private key.");
        } catch (KeyStoreException e) {
            throw new CausedIOException("Unable to read aliases from keystore", e);
        }

        saveClientCertificate(ssg, (PrivateKey)key, (X509Certificate)chainToImport[0], ssgPassword);
        return true;
    }
}
