package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass of entities that have certs.  Similar to CertEntryRow
 * and {@link TrustedCert}.
 */
public abstract class X509Entity extends NamedEntityImp {
    private static final Logger logger = Logger.getLogger(X509Entity.class.getName());

    private X509Certificate cachedCert;
    private String certBase64;
    private String thumbprintSha1;
    private String ski;
    private String subjectDn;
    private boolean readOnly = false;


    /**
     * Gets the {@link java.security.cert.X509Certificate} based on the saved {@link #certBase64}
     * @return an {@link java.security.cert.X509Certificate}
     */
    @XmlJavaTypeAdapter(X509CertificateAdapter.class)
    public X509Certificate getCertificate() {
        return cachedCert;
    }

    /**
     * Sets the {@link java.security.cert.X509Certificate}.  This also updates certb64, subjectdn, ski,
     * and thumbprintsha1.
     *
     * @param cert the {@link java.security.cert.X509Certificate}
     */
    public void setCertificate(X509Certificate cert) {
        setCertificate(cert, true);
    }

    /**
     * Update the cachedCert, subject DN, ski, thumbprintsha1 and optionally certBase64 fields based on the specified
     * cert.
     * <p/>
     * If the cert can't be encoded a warning will be logged and the certBase64 and thumbprint fields will be set to null.
     *
     * @param cert the cert to use to populate the other fields.
     * @param updateBase64 true to update the certBase64 field as well; false to leave its current value alone.
     */
    protected void setCertificate(X509Certificate cert, boolean updateBase64) {
        mutate();
        this.cachedCert = cert;
        if (updateBase64) {
            String certb64 = null;
            try {
                certb64 = cert == null ? null : HexUtils.encodeBase64(cert.getEncoded());
            } catch (CertificateEncodingException e) {
                logger.log(Level.WARNING, "Cert with DN '" + cert.getSubjectDN() + "' could not be encoded: " + ExceptionUtils.getMessage(e), e);
            }
            this.certBase64 = certb64;
        }
        setCertDerivedFields(cert);
    }


    /**
     * Set the subjet DN, thumbprintsha1 and ski fields based on the specified cert.
     * <p/>
     * If the cert can't be encoded a warning will be logged and the thumbprint field will be set to null.
     *
     * @param cert the cert to use to populate the thumbprint and sha1 fields.
     */
    protected void setCertDerivedFields(X509Certificate cert) {
        mutate();
        String subjectDn = null;
        String ski = null;
        String thumbprintSha1 = null;
        try {
            if (cert != null) {
                subjectDn = cert.getSubjectDN().toString();
                ski = CertUtils.getSki(cert);
                thumbprintSha1 = CertUtils.getCertificateFingerprint(cert, CertUtils.ALG_SHA1, CertUtils.FINGERPRINT_BASE64);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to find SHA-1 algorithm", e); // can't happen
        } catch (CertificateEncodingException e) {
            logger.log(Level.WARNING, "Cert with DN '" + cert.getSubjectDN() + "' could not be encoded: " + ExceptionUtils.getMessage(e), e);
        }
        this.subjectDn = subjectDn;
        this.ski = ski;
        this.thumbprintSha1 = thumbprintSha1;
    }

    /**
     * @return the SHA-1 thumbprint of the certificate (base64-encoded), or null if there is no cert.
     */
    public String getThumbprintSha1() {
        return thumbprintSha1;
    }

    /**
     * @return the SKI of the certificate (base64-encoded), or null either if there is no cert,
     * or the cert has no SKI
     */
    public String getSki() {
        return ski;
    }

    /**
     * @param ski  the Subject Key Identifier to set.
     * @deprecated for use only by serialization & persistence layers
     */
    public void setSki(String ski) {
        mutate();
        this.ski = ski;
    }

    /**
     * @param thumbprintSha1 the thumbprint of the certificate, base64-encoded.
     * @deprecated for use only by serialization & persistence layers
     */
    public void setThumbprintSha1(String thumbprintSha1) {
        mutate();
        this.thumbprintSha1 = thumbprintSha1;
    }

    /**
     * @return the subjectDn from the cert, or null.
     */
    public String getSubjectDn() {
        return subjectDn;
    }

    /**
     * @param subjectDn subject DN, or null.  Overrides any that was read from the cert or certb64.
     */
    protected void setSubjectDn(String subjectDn) {
        mutate();
        this.subjectDn = subjectDn;
    }

    /**
     * Gets the Base64 DER-encoded certificate
     * @return the Base64 DER-encoded certificate
     */
    public String getCertBase64() {
        return certBase64;
    }

    /**
     * Sets the Base64 DER-encoded certificate.  Also updates the certificate and all other certificate-related
     * fields.
     *
     * @param certBase64 the Base64 DER-encoded certificate
     */
    public void setCertBase64( String certBase64 ) {
        mutate();
        this.certBase64 = certBase64;
        this.cachedCert = null;
        if (certBase64 == null)
                return;

        // Avoid throwing since it can preven the SSG from booting if bad data is in the DB (Bug #2162)
        X509Certificate cert = null;
        try {
            cert = CertUtils.decodeCert(HexUtils.decodeBase64(certBase64));
        } catch (CertificateException e) {
            logger.log(Level.WARNING, "Following cert cannot be decoded and may be corrupted (bad ASN.1): " + certBase64, e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Following cert cannot be decoded and may be corrupted (bad Base64): " + certBase64, e);
        }
        setCertificate(cert, false);
    }

    /**
     * Throws an exception if this instance is read-only.
     *
     * @throws IllegalStateException if {@link #setReadOnly()} has been called on this instance.
     */
    protected void mutate() throws IllegalStateException {
        if (readOnly) throw new IllegalStateException("This instance is read-only.");
    }

    /**
     * Permanently mark this instance as read-only.  Once so marked, all mutators will throw
     * IllegalStateException.
     */
    public void setReadOnly() {
        this.readOnly = true;
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

    protected void copyFrom(X509Entity that) {
        mutate();
        this._name = that._name;
        this.certBase64 = that.certBase64;
        this.cachedCert = that.cachedCert;
        this.thumbprintSha1 = that.thumbprintSha1;
        this.subjectDn = that.subjectDn;
    }
}
