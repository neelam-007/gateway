/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.common.util.FileUtils;
import com.l7tech.common.util.SslUtils;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.ServerCertificateUntrustedException;
import com.l7tech.proxy.util.ClientLogger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Maintain the SSG-specific KeyStores.
 *
 * User: mike
 * Date: Jul 30, 2003
 * Time: 7:36:04 PM
 */
public class SsgKeyStoreManager {
    private static final ClientLogger log = ClientLogger.getInstance(SsgKeyStoreManager.class);
    private static final String ALIAS = "clientProxy";
    private static final String SSG_ALIAS = "ssgCa";

    /**
     * This is the password that will be used for obfuscating the keystore, and checking it for
     * corruption when it is reloaded.
     */
    private static final char[] KEYSTORE_PASSWORD = "lwbnasudg".toCharArray();

    /** Keystore type.  This is a read-write PKCS12 keystore provided by Bouncy Castle. */
    private static final String KEYSTORE_TYPE = "JCEKS";

    /**
     * Very quickly check if a client certificate is available for the specified SSG.
     * The first time this is called for a given SSG it will take the time to load the KeyStore.
     * May be slow if there is a problem with the key store.
     *
     * @param ssg
     * @return
     */
    public static boolean isClientCertAvailabile(Ssg ssg) throws KeyStoreCorruptException {
        if (ssg.haveClientCert() == null)
            ssg.haveClientCert(getClientCert(ssg) == null ? Boolean.FALSE : Boolean.TRUE);
        return ssg.haveClientCert().booleanValue();
    }

    /**
     * Look up the server certificate for this SSG.
     *
     * @param ssg  the Ssg whose KeyStore to examine
     * @return the server certificate (nominally the CA cert), or null if it hasn't yet been discovered.
     */
    public static X509Certificate getServerCert(Ssg ssg) throws KeyStoreCorruptException {
        try {
            return (X509Certificate) getKeyStore(ssg).getCertificate(SSG_ALIAS);
        } catch (KeyStoreException e) {
            log.error("impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
        }
    }

    /**
     * Look up our client certificate on this SSG.  Note that the password is not needed to do this.
     *
     * @param ssg  the Ssg whose KeyStore to examine
     * @return our client certificate, or null if we haven't yet applied for one
     */
    public static X509Certificate getClientCert(Ssg ssg) throws KeyStoreCorruptException {
        try {
            return (X509Certificate) getKeyStore(ssg).getCertificate(ALIAS);
        } catch (KeyStoreException e) {
            log.error("impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
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
        synchronized (ssg) {
            getKeyStore(ssg).deleteEntry(ALIAS);
            saveKeyStore(ssg);
            ssg.haveClientCert(Boolean.FALSE);
            ssg.passwordWorkedForPrivateKey(false);
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
        Certificate[] certs;
        try {
            certs = getKeyStore(ssg).getCertificateChain(ALIAS);
        } catch (KeyStoreException e) {
            log.error("impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
        }
        X509Certificate[] x5certs = new X509Certificate[certs.length];
        for (int i = 0; i < certs.length; i++) {
            Certificate cert = certs[i];
            if (cert instanceof X509Certificate)
                x5certs[i] = (X509Certificate) cert;
            else
                log.warn("Stored client cert is not X509Certificate: " + cert);
        }
        return x5certs;
    }

    /**
     * Get the private key for our client certificate with this SSG.
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
        synchronized (ssg) {
            if (!isClientCertAvailabile(ssg))
                return null;
            if (ssg.privateKey() != null)
                return ssg.privateKey();
        }
        PasswordAuthentication pw = Managers.getCredentialManager().getCredentials(ssg);
        synchronized (ssg) {
            if (!isClientCertAvailabile(ssg))
                return null;
            if (ssg.privateKey() != null)
                return ssg.privateKey();
            try {
                PrivateKey gotKey = (PrivateKey) getKeyStore(ssg).getKey(ALIAS, pw.getPassword());
                ssg.privateKey(gotKey);
                ssg.passwordWorkedForPrivateKey(true);
                return gotKey;
            } catch (KeyStoreException e) {
                log.error("impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
                throw new RuntimeException("impossible exception", e);
            } catch (UnrecoverableKeyException e) {
                log.error("Private key for client cert with Gateway " + ssg + " is unrecoverable with the current password");
                throw new BadCredentialsException(e);
            }
        }
    }

    /**
     * Check if the current SSG password matches the keystore private key password.
     */
    public static boolean isPasswordWorkedForPrivateKey(Ssg ssg) {
        return ssg.passwordWorkedForPrivateKey();
    }

    /**
     * Get the public key for our client certificate with this SSG.
     *
     * @param ssg   the SSG whose KeyStore to examine
     * @return our public key, or null if we haven't yet applied for a client cert with this Ssg.
     */
    public static PublicKey getClientCertPublicKey(Ssg ssg) throws KeyStoreCorruptException {
        if (!isClientCertAvailabile(ssg))
            return null;
        return getClientCert(ssg).getPublicKey();
    }

    /**
     * Obtain a key store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new keystore will be created in memory.  Call saveKeyStore() to safely
     * save the keystore back to disk.
     *
     * @param ssg The Ssg whose keystore we are setting up.  Must not be null.
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException (non-checked) if the key store is damaged, but user doesn't want to replace it just yet
     */
    public static KeyStore getKeyStore(Ssg ssg) throws KeyStoreCorruptException {
        synchronized (ssg) {
            if (ssg.keyStore() == null) {
                KeyStore keyStore;
                try {
                    keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                } catch (KeyStoreException e) {
                    log.error("Security provider configuration problem", e);
                    throw new RuntimeException(e); // can't happen unless VM misconfigured
                }
                FileInputStream fis = null;
                try {
                    fis = FileUtils.loadFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
                    keyStore.load(fis, KEYSTORE_PASSWORD);
                } catch (Exception e) {
                    if (e instanceof FileNotFoundException)
                        log.info("Creating new key store " + ssg.getKeyStoreFile() + " for Gateway " + ssg);
                    else {
                        log.error("Unable to load existing key store " + ssg.getKeyStoreFile() + " for Gateway " + ssg + " -- will create new one", e);
                        throw new KeyStoreCorruptException(e);
                    }
                    try {
                        keyStore.load(null, KEYSTORE_PASSWORD);
                    } catch (Exception e1) {
                        log.error("impossible exception", e1);
                        throw new RuntimeException(e1); // can't happen
                    }
                } finally {
                    if (fis != null)
                        try {
                            fis.close();
                        } catch (IOException e) {
                            log.error("Impossible IOException while closing an InputStream; will ignore and continue", e);
                        }
                }
                ssg.keyStore(keyStore);
            }
            return ssg.keyStore();
        }
    }

    /**
     * Save the KeyStore for this Ssg to disk, safely replacing any previous file.
     *
     * @param ssg  the Ssg whose KeyStore to save.
     * @throws IllegalStateException  if this SSG has not yet loaded its keystore
     * @throws IOException            if there was a problem writing the keystore to disk
     */
    private static void saveKeyStore(final Ssg ssg) throws IllegalStateException, IOException {
        synchronized (ssg) {
            if (ssg.keyStore() == null)
                throw new IllegalStateException("Gateway " + ssg + " hasn't yet loaded its keystore");

            FileUtils.saveFileSafely(ssg.getKeyStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.keyStore().store(fos, KEYSTORE_PASSWORD);
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
    public static void deleteKeyStore(Ssg ssg) {
        synchronized (ssg) {
            FileUtils.deleteFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
            ssg.keyStore(null);
            ssg.privateKey(null);
            ssg.passwordWorkedForPrivateKey(false);
            ssg.haveClientCert(null);
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
            throws KeyStoreException, IOException, KeyStoreCorruptException
    {
        synchronized (ssg) {
            log.info("Saving Gateway server certificate to disk");
            getKeyStore(ssg).setCertificateEntry(SSG_ALIAS, cert);
            saveKeyStore(ssg);
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
     * @throws IllegalArgumentException if the specified SSG has not yet had a password set
     * @throws KeyStoreException   if the key entry could not be saved for obscure reasons
     * @throws IOException  if there was a problem writing the keystore to disk
     */
    public static void saveClientCertificate(final Ssg ssg, PrivateKey privateKey, X509Certificate cert)
            throws KeyStoreException, IOException, KeyStoreCorruptException
    {
        log.info("Saving client certificate to disk");
        PasswordAuthentication pw;
        try {
            pw = Managers.getCredentialManager().getCredentials(ssg);
        } catch (OperationCanceledException e) {
            throw new IllegalArgumentException("Gateway " + ssg + " does not yet have credentials configured");
        }
        synchronized (ssg) {
            char[] password = pw.getPassword();
            getKeyStore(ssg).setKeyEntry(ALIAS, privateKey, password, new Certificate[] { cert });
            saveKeyStore(ssg);
            ssg.haveClientCert(Boolean.TRUE);
            ssg.privateKey(privateKey);
        }
    }

    /**
     * Download and install the SSG certificate.  If this completes successfully, the
     * next attempt to connect to the SSG via SSL should at least get past the SSL handshake.  Uses the
     * specified credentials for the download.
     *
     * @throws IOException if there was a network problem downloading the server cert
     * @throws IOException if there was a problem reading or writing the keystore for this SSG
     * @throws BadCredentialsException if the downloaded cert could not be verified with the SSG username and password
     * @throws OperationCanceledException if credentials were needed but the user declined to enter them
     * @throws GeneralSecurityException for miscellaneous and mostly unlikely certificate or key store problems
     */
    public static void installSsgServerCertificate(Ssg ssg, PasswordAuthentication credentials)
            throws IOException, BadCredentialsException, OperationCanceledException, GeneralSecurityException, KeyStoreCorruptException
    {
        CertificateDownloader cd = new CertificateDownloader(ssg.getServerUrl(),
                                                             credentials.getUserName(),
                                                             credentials.getPassword());

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
    public static void obtainClientCertificate(Ssg ssg, PasswordAuthentication credentials, KeyPair keyPair)
            throws  ServerCertificateUntrustedException, GeneralSecurityException, IOException,
                    BadCredentialsException, CertificateAlreadyIssuedException, KeyStoreCorruptException
    {
        CertificateRequest csr = JceProvider.makeCsr(ssg.getUsername(), keyPair);

        try {
            X509Certificate caCert = getServerCert(ssg);
            if (caCert == null)
                throw new ServerCertificateUntrustedException(); // fault in the SSG cert
            X509Certificate cert = SslUtils.obtainClientCertificate(ssg.getServerCertRequestUrl(),
                                                                    credentials.getUserName(),
                                                                    credentials.getPassword(),
                                                                    csr,
                                                                    caCert);
            // make sure private key is stored on disk encrypted with the password that was used to obtain it
            saveClientCertificate(ssg, keyPair.getPrivate(), cert);
            ssg.resetSslContext(); // reset cached SSL state
            return;
        } catch (SslUtils.BadCredentialsException e) {  // note: not the same class BadCredentialsException
            throw new BadCredentialsException(e);
        } catch (SslUtils.CertificateAlreadyIssuedException e) {
            throw new CertificateAlreadyIssuedException(e);
        }
    }
}
