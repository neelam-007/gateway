/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.io.OutputStream;
import java.io.IOException;

/**
 * @author mike
 */
public class Pkcs12Exporter {
    private static final String PKCS12_KEYSTORE_TYPE = "PKCS12";

    public Pkcs12Exporter() {
    }

    /**
     * Export the specified public certificate as a new PKCS12 file to the specified OutputStream.
     *
     * @param certificate the certificate to export
     * @param alias the user-visible name to use for this certificate within the PKCS12 file.  The username (for
     *              a client cert) or hostname (for a server cert), or a fixed string such as "exported", are
     *              possible choices.
     * @param password the password to use to protect the exported PKCS12 information.  May be zero length but not null.
     * @param outputStream the OutputStream to which to send the new PKCS12 file.
     * @throws KeyStoreException if there is no implementation of "PKCS12" keystore available
     * @throws NoSuchAlgorithmException if there is no implementation of a required algorithm such as Triple DES
     * @throws IOException if there is a problem writing to the OutputStream
     * @throws CertificateException if the specified certificate could not be serialized or stored for any reason
     */
    public void exportPublicCertificateAsPkcs12(Certificate certificate,
                                                String alias,
                                                char[] password,
                                                OutputStream outputStream)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException 
    {
        KeyStore ks = KeyStore.getInstance(PKCS12_KEYSTORE_TYPE);
        ks.load(null, password);
        ks.setCertificateEntry(alias, certificate);
        ks.store(outputStream, password);
    }

    /**
     * Export the specified certificate and accompanying private key as a new PKCS12 file to the specified OutputStream.
     *
     * @param certificate the certificate to export
     * @param privateKey the private key corresponding to the public key in the certificate
     * @param alias the user-visible name to use for this certificate within the PKCS12 file.  The username (for
     *              a client cert) or hostname (for a server cert), or a fixed string such as "exported", are
     *              possible choices.
     * @param password the password to use to protect the exported PKCS12 information.  May be zero length but not null.
     * @param outputStream the OutputStream to which to send the new PKCS12 file.
     * @throws KeyStoreException if there is no implementation of "PKCS12" keystore available, or if the private key
     *                           cannot be encrypted
     * @throws NoSuchAlgorithmException if there is no implementation of a required algorithm such as Triple DES
     * @throws IOException if there is a problem writing to the OutputStream
     * @throws CertificateException if the specified certificate could not be serialized or stored for any reason
     */
    public void exportPrivateCertificateAsPkcs12(Certificate certificate,
                                                 PrivateKey privateKey,
                                                 String alias,
                                                 char[] password,
                                                 OutputStream outputStream)
            throws NoSuchAlgorithmException, IOException, CertificateException, KeyStoreException
    {
        KeyStore ks = KeyStore.getInstance(PKCS12_KEYSTORE_TYPE);
        ks.load(null, password);
        ks.setKeyEntry(alias, privateKey, null, new Certificate[] { certificate });
        ks.store(outputStream, password);
    }
}
