/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.IOException;

/**
 * @author mike
 */
public class Pkcs12Importer {
    private static final String PKCS12_KEYSTORE_NAME = "PKCS12";

    public Pkcs12Importer() {
    }

    public static class Entry {
        public static final int UNKNOWN = -1;
        public static final int PUBLIC_CERTIFICATE = 0;
        public static final int PRIVATE_CERTIFICATE = 1;

        protected KeyStore ks;
        private String alias;
        private int type;

        protected Entry(KeyStore ks, String alias, int type) {
            this.ks = ks;
            this.alias = alias;
            this.type = type;
        }

        /** @return the name of this entry from the PKCS12 file. */
        public String getAlias() { return alias; }

        /** @return true iff. this entry from the PKCS12 file is of an unrecognized type. */
        public boolean isUnknown() { return type == UNKNOWN; }

        /** @return true iff. this entry from the PKCS12 file is a Public Certificate (cert only, no private key). */
        public boolean isPublicCertificate() { return type == PUBLIC_CERTIFICATE; }

        /** @return true iff. this entry from the PKCS12 file is a Private Certificate, with accompanying private key. */
        public boolean isPrivateCertificate() { return type == PRIVATE_CERTIFICATE; }

        /**
         * @return the certificate chain from this entry from the PKCS12 file.
         * @throws UnsupportedOperationException if this entry does not contain a certificate chain.
         */
        public Certificate[] getCertificateChain() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * @return the private key from this entry from the PKS12 file.
         * @throws UnsupportedOperationException if this entry does not contain a private key.
         */
        public Key getKey() throws UnsupportedOperationException, NoSuchAlgorithmException,
                                   UnrecoverableKeyException
        {
            throw new UnsupportedOperationException();
        }
    }

    public static class UnknownEntry extends Entry {
        public UnknownEntry(KeyStore ks, String alias) {
            super(ks, alias, UNKNOWN);
        }
    }

    public static class PublicCertificateEntry extends Entry {
        private PublicCertificateEntry(KeyStore ks, String alias) {
            super(ks, alias, PUBLIC_CERTIFICATE);
        }

        public Certificate[] getCertificateChain() throws UnsupportedOperationException {
            try {
                return new Certificate[] { ks.getCertificate(alias) };
            } catch (KeyStoreException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    }

    public static class PrivateCertificateEntry extends Entry {
        private PrivateCertificateEntry(KeyStore ks, String alias) {
            super(ks, alias, PRIVATE_CERTIFICATE);
        }

        public Certificate[] getCertificateChain() throws UnsupportedOperationException {
            try {
                return ks.getCertificateChain(alias);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e); // can't happen
            }
        }

        public Key getKey() throws NoSuchAlgorithmException, UnrecoverableKeyException {
            try {
                return ks.getKey(alias, null);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
    }

    public Entry[] importPkcs12(InputStream inputStream, char[] password)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException
    {
        List build = new ArrayList();
        KeyStore ks = KeyStore.getInstance(PKCS12_KEYSTORE_NAME);
        ks.load(inputStream, password);
        Enumeration aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement().toString();
            if (ks.isCertificateEntry(alias)) {
                build.add(new PublicCertificateEntry(ks, alias));
            } else if (ks.isKeyEntry(alias)) {
                build.add(new PrivateCertificateEntry(ks, alias));
            } else {
                build.add(new UnknownEntry(ks, alias));
            }
        }

        return (Entry[]) build.toArray(new Entry[0]);
    }
}
