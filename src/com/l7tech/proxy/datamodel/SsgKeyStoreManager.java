/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.common.util.FileUtils;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.PublicKey;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.cert.Certificate;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;

import org.apache.log4j.Category;

/**
 * Maintain the SSG-specified KeyStores.
 * User: mike
 * Date: Jul 30, 2003
 * Time: 7:36:04 PM
 */
public class SsgKeyStoreManager {
    private static final Category log = Category.getInstance(SsgKeyStoreManager.class);
    private static final String ALIAS = "clientProxy";
    private static final String SSG_ALIAS = "ssgCa";
    private static char[] KEYSTORE_PASSWORD = "password".toCharArray();

    /**
     * Very quickly check if a client certificate is available for the specified SSG.
     * The first time this is called for a given SSG it will take the time to load the KeyStore.
     * May be slow if there is a problem with the key store.
     * @param ssg
     * @return
     */
    public static boolean isClientCertAvailabile(Ssg ssg) {
        if (!ssg.isCredentialsConfigured())
            return false;
        if (ssg.haveClientCert() == null) {
            try {
                ssg.haveClientCert(getClientCert(ssg) == null ? Boolean.FALSE : Boolean.TRUE);
            } catch (IOException e) {
                log.error(e);
                return false;
            } catch (GeneralSecurityException e) {
                log.error(e);
                return false;
            }
        }
        return ssg.haveClientCert().booleanValue();
    }

    public static X509Certificate getServerCert(Ssg ssg)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
    {
        return (X509Certificate) getKeyStore(ssg).getCertificate(SSG_ALIAS);
    }

    public static X509Certificate getClientCert(Ssg ssg)
            throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException
    {
        return (X509Certificate) getKeyStore(ssg).getCertificate(ALIAS);
    }

    public static X509Certificate[] getClientCertificateChain(Ssg ssg)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
    {
        Certificate[] certs = getKeyStore(ssg).getCertificateChain(ALIAS);
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

    public static PrivateKey getPrivateKey(Ssg ssg)
            throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException,
                   UnrecoverableKeyException
    {
        if (!isClientCertAvailabile(ssg))
            return null;
        return (PrivateKey) getKeyStore(ssg).getKey(ALIAS, ssg.password());
    }

    public static PublicKey getPublicKey(Ssg ssg)
            throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException
    {
        if (!isClientCertAvailabile(ssg))
            return null;
        return getClientCert(ssg).getPublicKey();
    }

    public static KeyStore getKeyStore(Ssg ssg)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
    {
        synchronized (ssg) {
            if (ssg.keyStore() == null) {
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                FileInputStream fis = null;
                try {
                    fis = FileUtils.loadFileSafely(ssg.getKeyStoreFile().getAbsolutePath());
                    keyStore.load(fis, KEYSTORE_PASSWORD);
                } catch (FileNotFoundException e) {
                    keyStore.load(null, KEYSTORE_PASSWORD);
                } finally {
                    if (fis != null)
                        fis.close();
                }
                ssg.keyStore(keyStore);
            }
            return ssg.keyStore();
        }
    }

    public static void saveKeyStore(final Ssg ssg) throws IOException {
        synchronized (ssg) {
            if (ssg.keyStore() == null)
                throw new IllegalStateException("SSG " + ssg + " hasn't yet loaded its keystore");

            FileUtils.saveFileSafely(ssg.getKeyStoreFile().getAbsolutePath(),
                                     new FileUtils.Saver() {
                                         public void doSave(FileOutputStream fos) throws IOException {
                                             try {
                                                 ssg.keyStore().store(fos, ssg.password());
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

    public static void saveSsgCertificate(final Ssg ssg, X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
    {
        synchronized (ssg) {
            log.info("Saving SSG certificate to disk");
            getKeyStore(ssg).setCertificateEntry(SSG_ALIAS, cert);
            saveKeyStore(ssg);
        }
    }

    public static void saveClientCertificate(final Ssg ssg, PrivateKey privateKey, X509Certificate cert)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException
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
