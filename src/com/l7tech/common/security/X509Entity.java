package com.l7tech.common.security;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Abstract superclass of entities that have certs, like {@link com.l7tech.server.identity.cert.CertEntryRow}
 * and {@link TrustedCert}
 */
public abstract class X509Entity extends NamedEntityImp {
    protected transient X509Certificate cachedCert;
    protected String certBase64;
    protected String thumbprintSha1;
    protected String ski;

    /**
     * Gets the {@link java.security.cert.X509Certificate} based on the saved {@link #certBase64}
     * @return an {@link java.security.cert.X509Certificate}
     * @throws java.security.cert.CertificateException if the certificate cannot be deserialized
     */
    @XmlJavaTypeAdapter(X509CertificateAdapter.class)
    public synchronized X509Certificate getCertificate() throws CertificateException {
        if ( cachedCert == null ) {
            if (certBase64 == null) return null;
            // note fla: this class is called by hibernate which does not know how to handle the thrown exceptions
            // is in some instances it causes the ssg to not boot (e.g. if the db is somehow corrupted as per
            // bugzilla 2162)
            // will catch this indirectly in the boot process
            try {
                cachedCert = CertUtils.decodeCert(HexUtils.decodeBase64(certBase64));
            } catch (CertificateException e) {
                throw new CertificateException("Following cert cannot be decoded and may be corrupted (bad ASN.1): " + certBase64, e);
            } catch (IOException e) {
                throw new CertificateException("Following cert cannot be decoded and may be corrupted (bad Base64): " + certBase64, e);
            }
        }
        return cachedCert;
    }

    /**
     * Sets the {@link java.security.cert.X509Certificate}
     * @param cert the {@link java.security.cert.X509Certificate}
     * @throws java.security.cert.CertificateEncodingException if the certificate cannot be serialized
     */
    public synchronized void setCertificate(X509Certificate cert) throws CertificateEncodingException {
        this.cachedCert = cert;
        if (cert == null) {
            this.certBase64 = null;
        } else {
            this.certBase64 = HexUtils.encodeBase64( cert.getEncoded() );
        }
    }

    /**
     * @return the SHA-1 thumbprint of the certificate (base64-encoded), or null if there is no cert.
     * @see #getCertificate()
     * @see #getCertBase64()
     * @throws CertificateException if the certificate cannot be deserialized
     */
    public String getThumbprintSha1() throws CertificateException {
        if (thumbprintSha1 == null) {
            X509Certificate cert = getCertificate();
            if (cert == null) return null;
            try {
                thumbprintSha1 = CertUtils.getCertificateFingerprint(cert, CertUtils.ALG_SHA1, CertUtils.FINGERPRINT_BASE64);
            } catch (NoSuchAlgorithmException e) {
                throw new CertificateException("Unable to find SHA-1 algorithm", e);
            }
        }
        return thumbprintSha1;
    }

    /**
     * @return the SKI of the certificate (base64-encoded), or null either if there is no cert,
     * or the cert has no SKI
     * @throws CertificateException if the certificate cannot be deserialized
     */
    public String getSki() throws CertificateException {
        if (ski == null) {
            X509Certificate cert = getCertificate();
            if (cert == null) return null;
            ski = CertUtils.getSki(cert);
        }
        return ski;
    }

    /**
     * @param ski  the Subject Key Identifier to set.
     * @deprecated for use only by serialization & persistence layers
     */
    public void setSki(String ski) {
        this.ski = ski;
    }

    /**
     * @param thumbprintSha1 the thumbprint of the certificate, base64-encoded.
     * @deprecated for use only by serialization & persistence layers
     */
    public void setThumbprintSha1(String thumbprintSha1) {
        this.thumbprintSha1 = thumbprintSha1;
    }

    /**
     * Gets the Base64 DER-encoded certificate
     * @return the Base64 DER-encoded certificate
     */
    public synchronized String getCertBase64() {
        return certBase64;
    }

    /**
     * Sets the Base64 DER-encoded certificate
     * @param certBase64 the Base64 DER-encoded certificate
     */
    public synchronized void setCertBase64( String certBase64 ) {
        this.certBase64 = certBase64;
        this.cachedCert = null;
    }

    /** @noinspection RedundantIfStatement*/
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final X509Entity that = (X509Entity)o;

        if (certBase64 != null ? !certBase64.equals(that.certBase64) : that.certBase64 != null) return false;

        return true;
    }

    public int hashCode() {
        return (certBase64 != null ? certBase64.hashCode() : 0);
    }
}
