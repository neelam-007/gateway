/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.FileUtils;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import org.apache.log4j.Category;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
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
    private static final Category log = Category.getInstance(SsgKeyStoreManager.class);
    private static final String ALIAS = "clientProxy";
    private static final String SSG_ALIAS = "ssgCa";
    private static final char[] KEYSTORE_PASSWORD = "lwbnasudg".toCharArray();

    /**
     * Very quickly check if a client certificate is available for the specified SSG.
     * The first time this is called for a given SSG it will take the time to load the KeyStore.
     * May be slow if there is a problem with the key store.
     *
     * @param ssg
     * @return
     */
    public static boolean isClientCertAvailabile(Ssg ssg) {
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
    public static X509Certificate getServerCert(Ssg ssg)
    {
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
    public static X509Certificate getClientCert(Ssg ssg)
    {
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
            throws IOException, KeyStoreException
    {
        synchronized (ssg) {
            getKeyStore(ssg).deleteEntry(ALIAS);
            saveKeyStore(ssg);
            ssg.haveClientCert(Boolean.FALSE);
        }
    }

    /**
     * Get the entire certificate chain for our client certificate.  Typically this will have only two certificates
     * in it: our client cert first, and then the SSG's CA cert.  Only X.509 certificates are returned.
     *
     * @param ssg
     * @return
     */
    public static X509Certificate[] getClientCertificateChain(Ssg ssg)
    {
        Certificate[] certs = new Certificate[0];
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
     * @throws com.l7tech.proxy.datamodel.exceptions.BadCredentialsException    if the SSG password does not match the password used to encrypt the key.
     * @throws com.l7tech.proxy.datamodel.exceptions.OperationCanceledException if the user canceled the password prompt
     */
    public static PrivateKey getClientCertPrivateKey(Ssg ssg)
            throws NoSuchAlgorithmException, BadCredentialsException, OperationCanceledException
    {
        if (!isClientCertAvailabile(ssg))
            return null;
        if (!ssg.isCredentialsConfigured())
            Managers.getCredentialManager().getCredentials(ssg);
        try {
            return (PrivateKey) getKeyStore(ssg).getKey(ALIAS, ssg.password());
        } catch (KeyStoreException e) {
            log.error("impossible exception", e);  // can't happen; keystore is initialized by getKeyStore()
            throw new RuntimeException("impossible exception", e);
        } catch (UnrecoverableKeyException e) {
            log.error("Private key for client cert with ssg " + ssg + " is unrecoverable with the current password");
            throw new BadCredentialsException(e);
        }
    }

    /**
     * Get the public key for our client certificate with this SSG.
     *
     * @param ssg   the SSG whose KeyStore to examine
     * @return our public key, or null if we haven't yet applied for a client cert with this Ssg.
     */
    public static PublicKey getClientCertPublicKey(Ssg ssg) {
        if (!isClientCertAvailabile(ssg))
            return null;
        return getClientCert(ssg).getPublicKey();
    }

    private static class KeyStoreCorruptException extends RuntimeException {
        public KeyStoreCorruptException() {
        }

        public KeyStoreCorruptException(String message) {
            super(message);
        }

        public KeyStoreCorruptException(String message, Throwable cause) {
            super(message, cause);
        }

        public KeyStoreCorruptException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Obtain a key store for this SSG.  If one is present on disk, it will be loaded.  If the one on disk
     * is missing or corrupt, a new keystore will be created in memory.  Call saveKeyStore() to safely
     * save the keystore back to disk.
     *
     * @param ssg The Ssg whose keystore we are setting up.  Must not be null.
     * @return an in-memory KeyStore object for this Ssg, either loaded from disk or newly created.
     * @throws KeyStoreCorruptException (non-checked) if the key store is damaged, but user doesn't want to replace it just yet
     */
    public static KeyStore getKeyStore(Ssg ssg) throws KeyStoreCorruptException {
        synchronized (ssg) {
            if (ssg.keyStore() == null) {
                KeyStore keyStore;
                try {
                    keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
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
                        log.info("Creating new key store " + ssg.getKeyStoreFile() + " for SSG " + ssg);
                    else {
                        log.error("Unable to load existing key store " + ssg.getKeyStoreFile() + " for SSG " + ssg + " -- will create new one", e);
                        try {
                            Managers.getCredentialManager().notifyKeyStoreCorrupt(ssg);
                        } catch (OperationCanceledException e1) {
                            throw new KeyStoreCorruptException(e1);
                        }
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
    public static void saveKeyStore(final Ssg ssg) throws IllegalStateException, IOException {
        synchronized (ssg) {
            if (ssg.keyStore() == null)
                throw new IllegalStateException("SSG " + ssg + " hasn't yet loaded its keystore");

            FileUtils.saveFileSafely(ssg.getKeyStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.keyStore().store(fos, KEYSTORE_PASSWORD);
                                                 fos.close();
                                             } catch (KeyStoreException e) {
                                                 throw new IOException("Unable to write KeyStore for SSG " + ssg + ": " + e);
                                             } catch (NoSuchAlgorithmException e) {
                                                 throw new IOException("Unable to write KeyStore for SSG " + ssg + ": " + e);
                                             } catch (CertificateException e) {
                                                 throw new IOException("Unable to write KeyStore for SSG " + ssg + ": " + e);
                                             }
                                         }
                                     });

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
            throws KeyStoreException, IOException
    {
        synchronized (ssg) {
            log.info("Saving SSG certificate to disk");
            getKeyStore(ssg).setCertificateEntry(SSG_ALIAS, cert);
            saveKeyStore(ssg);
        }
    }

    /**
     * Set our client certificate for this Ssg.
     *
     * @param ssg  the Ssg whose KeyStore to save
     * @param privateKey   the RSA private key corresponding to the public key in the certificate
     * @param cert    the certificate, signed by the SSG CA, and whose public key corrsponds to privateKey
     * @throws KeyStoreException   if the key entry could not be saved for obscure reasons
     * @throws IOException  if there was a problem writing the keystore to disk
     */
    public static void saveClientCertificate(final Ssg ssg, PrivateKey privateKey, X509Certificate cert)
            throws KeyStoreException, IOException
    {
        synchronized (ssg) {
            log.info("Saving client certificate to disk");
            if (ssg.password() == null)
                throw new IllegalArgumentException("SSG " + ssg + " does not have a password set.");
            getKeyStore(ssg).setKeyEntry(ALIAS, privateKey, ssg.password(), new Certificate[] { cert });
            saveKeyStore(ssg);
            ssg.haveClientCert(Boolean.TRUE);
        }
    }

}
