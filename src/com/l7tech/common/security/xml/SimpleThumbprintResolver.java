/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.CertUtils;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A ThumbprintResolver that is given a list of certs that it is to recognize.
 */
public class SimpleThumbprintResolver implements ThumbprintResolver {
    private static final Logger logger = Logger.getLogger(SimpleThumbprintResolver.class.getName());

    private final Cert[] certs;

    private static class Cert {
        private final X509Certificate cert;
        private String thumb = null;

        public Cert(X509Certificate cert) {
            this.cert = cert;
        }

        public String getThumb() {
            if (thumb == null && cert != null) {
                try {
                    thumb = CertUtils.getThumbprintSHA1(cert);
                } catch (CertificateEncodingException e) {
                    logger.log(Level.WARNING, "Invalid certificate: " + e.getMessage(), e);
                }
            }
            return thumb;
        }
    }

    /**
     * Create a thumbprint resolver that will recognize any cert in the specified list.
     * For convenience, the certs array may contain nulls which will be ignored.
     */
    public SimpleThumbprintResolver(X509Certificate[] certs) {
        this.certs = new Cert[certs.length];
        for (int i = 0; i < certs.length; i++)
            this.certs[i] = new Cert(certs[i]);
    }

    public SimpleThumbprintResolver(X509Certificate cert) {
        this(new X509Certificate[] { cert });
    }

    public X509Certificate lookup(String thumbprint) {
        for (int i = 0; i < certs.length; i++) {
            Cert cert = certs[i];
            String thumb = cert.getThumb();
            if (thumb != null && thumb.equals(thumbprint))
                return cert.cert;
        }
        return null;
    }
}
