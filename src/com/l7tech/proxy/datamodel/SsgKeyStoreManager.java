/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.CertificateDownloader;
import com.l7tech.proxy.datamodel.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintain the SSG-specific KeyStores and manage persistences for key material in the Bridge.
 */
public abstract class SsgKeyStoreManager {
    private static final Logger logger = Logger.getLogger(SsgKeyStoreManager.class.getName());

    /**
     * Very quickly check if a client cert private key has already been unlocked for
     * the specified SSG.  The first time this is called for a given SSG it will
     * take the tiem to load the KeyStore.  May be slow if there are problems with
     * the key store.  If this method returns true, the client cert private key
     * is already cached in memory.
     * <p>
     * TODO get rid of this somehow
     *
     * @return true if we have a client cert for this SSG and have already unlocked the private key
     * @throws KeyStoreCorruptException if the trust store or key store is damaged
     */
    public abstract boolean isClientCertUnlocked() throws KeyStoreCorruptException;

    /**
     * Delete our client certificate for this SSG.  The KeyStore will be persisted to disk afterward.
     *
     * @throws IOException  if there was a problem saving the KeyStore to disk
     * @throws KeyStoreException  if the private key entry cannot be removed.  I don't think this ever happens, but
     *                            KeyStore.deleteEntry() contract states that some provider might someday throw this.
     */
    public abstract void deleteClientCert()
            throws IOException, KeyStoreException, KeyStoreCorruptException;

    /**
     * Check if the current SSG password matches the keystore private key password.
     * <p>
     * TODO remove this somehow, it sucks
     */
    public abstract boolean isPasswordWorkedForPrivateKey();

    /**
     * Delete any keystore that might exist for this Ssg.
     */
    public abstract void deleteStores();

    /**
     * Set the Ssg certificate for this Ssg.
     *
     * @param cert  The certificate to save.  Expected to be the SSG CA public key.
     * @throws KeyStoreException if the KeyStore is corrupt, or "the operation failed for some other reason"
     * @throws IOException       if there was a problem writing the keystore to disk
     */
    public abstract void saveSsgCertificate(X509Certificate cert)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException;

    /**
     * Set our client certificate for this Ssg.  Caller must ensure that the correct password is set
     * on the SSG before calling this, since the password will be used to protect the private key.  The
     * safest way to ensure this is to use the same password that was used to successfully apply for
     * the client certificate you are about to save.
     *
     * @param privateKey   the RSA private key corresponding to the public key in the certificate
     * @param cert    the certificate, signed by the SSG CA, and whose public key corrsponds to getCachedPrivateKey
     * @param privateKeyPassword the pass phrase with which to encrypt the private key
     * @throws IllegalArgumentException if the specified SSG has not yet had a password set
     * @throws KeyStoreException   if the key entry could not be saved for obscure reasons
     * @throws IOException  if there was a problem writing the keystore to disk
     * @throws CertificateException if the returned certificate can't be parsed
     */
    public abstract void saveClientCertificate(PrivateKey privateKey, X509Certificate cert,
                                             char[] privateKeyPassword)
            throws KeyStoreException, IOException, KeyStoreCorruptException, CertificateException;

    public void installSsgServerCertificate(Ssg ssg, PasswordAuthentication credentials)
            throws IOException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException, CertificateException, KeyStoreException
    {
        logger.log(Level.FINER, "Discovering server certificate for Gateway " + ssg + " (" + ssg.getLocalEndpoint() + ")");
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
                ssg.getRuntime().getCredentialManager().notifySslCertificateUntrusted(ssg, "the Gateway " + ssg, gotCert);
            } else
                throw new BadCredentialsException("The downloaded Gateway server certificate could not be verified with the current user name and password.");
        }

        saveSsgCertificate(gotCert);
    }


    /**
     * Generate a Certificate Signing Request, and apply to the Ssg for a certificate for the
     * current user.  If this method returns, the certificate will have been downloaded and saved
     * locally, and the SSL context for this Client Proxy will have been reinitialized.
     *
     * It is an error to pass in a Federated SSG; pass in the Trusted SSG (and it's credentials) instead.
     *
     * @param credentials  the username and password to use for the application
     * @throws ServerCertificateUntrustedException if we haven't yet discovered the Ssg server cert
     * @throws GeneralSecurityException   if there was a problem making the CSR
     * @throws GeneralSecurityException   if we were unable to complete SSL handshake with the Ssg
     * @throws IOException                if there was a network problem
     * @throws BadCredentialsException    if the SSG rejected the credentials we provided
     * @throws com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException if the SSG has already issued the client certificate for this account
     * @throws KeyStoreCorruptException   if the keystore is corrupt
     */
    public abstract void obtainClientCertificate(PasswordAuthentication credentials)
            throws BadCredentialsException, GeneralSecurityException, KeyStoreCorruptException,
                   CertificateAlreadyIssuedException, IOException;


    /** Return the username in our client certificate, or null if we don't have an active client cert. */
    public abstract String lookupClientCertUsername();

    /**
     * Look up the server certificate for this SSG.
     *
     * @return the server certificate (nominally the CA cert), or null if it hasn't yet been discovered.
     */
    protected abstract X509Certificate getServerCert() throws KeyStoreCorruptException;

    /**
     * Look up our client certificate on this SSG.  Note that the password is not needed to do this.
     *
     * @return our client certificate, or null if we haven't yet applied for one
     */
    protected abstract X509Certificate getClientCert() throws KeyStoreCorruptException;

    /**
     * Get the private key for our client certificate with this SSG.  Caller *must not* hold the SSG monitor
     * when calling this method.
     *
     * @return      The PrivateKey, or null if we haven't yet applied for a client certificate with this SSG.
     * @throws java.security.NoSuchAlgorithmException   if the VM is misconfigured, or the keystore was tampered with
     * @throws com.l7tech.proxy.datamodel.exceptions.BadCredentialsException    if the SSG password does not match the password used to encrypt the key.
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if the user canceled the password prompt
     */
    public abstract PrivateKey getClientCertPrivateKey()
            throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException, KeyStoreCorruptException;

    /**
     * Very quickly check if a client certificate is available for the specified SSG.
     * The first time this is called for a given SSG it will take the time to load the KeyStore.
     * May be slow if there is a problem with the key store.  Even if this method returns
     * true, the client cert might still require credentials to unlock its private key.
     * To check if the cert has already been unlocked, use isClientCertUnlocked() instead.
     *
     * @return true if we have a client cert for this ssg
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the trust store is damaged
     */
    protected abstract boolean isClientCertAvailabile() throws KeyStoreCorruptException;

    /**
     * Obtain a key store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new keystore will be created in memory.  Call saveStores() to safely
     * save the keystore back to disk.
     * <p>
     * To protect the returned keystore from concurrent modification, the caller must hold the ssg monitor
     * before calling this method.
     *
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the key store is damaged
     * @throws IllegalStateException if the current thread does not already hold the ssg monitor.
     */
    protected abstract KeyStore getKeyStore(char[] password) throws KeyStoreCorruptException;

    /**
     * Obtain a trust store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new trust store will be created in memory.  Call saveTrustStore() to safely
     * save the trust store back to disk.
     * <p>
     * To protect the returned trust store from concurrent modification, the caller must already hold the
     * Ssg monitor before calling this method.
     *
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException if the key store is damaged
     */
    protected abstract KeyStore getTrustStore() throws KeyStoreCorruptException;

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
    public static interface AliasPicker {
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
     * @param certFile the PKCS#12 file from which to read the new cert
     * @param pass the pass phrase to use when reading the file
     * @param aliasPicker optional AliasPicker in case there is more than one certificate in the file.  If not provided,
     *                    will always select the very first cert.
     * @throws KeyStoreCorruptException if the Ssg keystore is damaged or could not be writted using ssgPassword
     * @throws IOException if there is a problem reading the file or the file does not contain any private keys
     * @throws GeneralSecurityException if the file can't be decrypted or the imported certificate can't be saved
     * @throws AliasNotFoundException if the specified alias was not found or did not contain both a cert chain and a private key
     */
    public abstract void importClientCertificate(File certFile,
                                 char[] pass,
                                 AliasPicker aliasPicker,
                                 char[] ssgPassword)
            throws IOException, GeneralSecurityException, KeyStoreCorruptException, AliasNotFoundException;
}
