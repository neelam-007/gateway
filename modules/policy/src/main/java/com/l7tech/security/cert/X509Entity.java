package com.l7tech.security.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.objectmodel.imp.GoidEntityImp;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract superclass of entities that have certs.  Similar to CertEntryRow
 * and {@link TrustedCert}.
 */
@MappedSuperclass
public abstract class X509Entity extends GoidEntityImp {
    private static final Logger logger = Logger.getLogger(X509Entity.class.getName());

    private X509Certificate cachedCert;
    private String certBase64;
    private String thumbprintSha1;
    private String ski;
    private String subjectDn;
    private String issuerDn;
    private BigInteger serial;
    private boolean readOnly = false;


    /**
     * Gets the {@link java.security.cert.X509Certificate} based on the saved {@link #certBase64}
     * @return an {@link java.security.cert.X509Certificate}
     */
    @Transient
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
        String issuerDn = null;
        String ski = null;
        String thumbprintSha1 = null;
        BigInteger serial = null;
        try {
            if (cert != null) {
                subjectDn = CertUtils.getSubjectDN(cert);
                issuerDn = CertUtils.getIssuerDN(cert);
                ski = CertUtils.getSki(cert);
                thumbprintSha1 = CertUtils.getCertificateFingerprint(cert, CertUtils.ALG_SHA1, CertUtils.FINGERPRINT_BASE64);
                serial = cert.getSerialNumber();
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to find SHA-1 algorithm", e); // can't happen
        } catch (CertificateEncodingException e) {
            logger.log(Level.WARNING, "Cert with DN '" + cert.getSubjectDN() + "' could not be encoded: " + ExceptionUtils.getMessage(e), e);
        }
        
        this.subjectDn = subjectDn;
        this.issuerDn = issuerDn;
        this.ski = ski;
        this.thumbprintSha1 = thumbprintSha1;
        this.serial = serial;
    }

    /**
     * @return the SHA-1 thumbprint of the certificate (base64-encoded), or null if there is no cert.
     */
    @Size(max=64)
    @Column(name="thumbprint_sha1",length=64)
    public String getThumbprintSha1() {
        return thumbprintSha1;
    }

    /**
     * @return the SKI of the certificate (base64-encoded), or null either if there is no cert,
     * or the cert has no SKI
     */
    @Size(max=64)
    @Column(name="ski",length=64)
    public String getSki() {
        return ski;
    }

    /**
     * @param ski  the Subject Key Identifier to set.
     * @deprecated for use only by serialization & persistence layers
     */
    @Deprecated
    public void setSki(String ski) {
        mutate();
        this.ski = ski;
    }

    /**
     * @param thumbprintSha1 the thumbprint of the certificate, base64-encoded.
     * @deprecated for use only by serialization & persistence layers
     */
    @Deprecated
    public void setThumbprintSha1(String thumbprintSha1) {
        mutate();
        this.thumbprintSha1 = thumbprintSha1;
    }

    /**
     * The Subject DN in canonical format (not for display use)
     *
     * @return the subjectDn from the cert, or null.
     */
    @Size(max=2048)
    @Column(name="subject_dn", length=2048)
    public String getSubjectDn() {
        return subjectDn;
    }

    /**
     * @param subjectDn subject DN, or null.  Overrides any that was read from the cert or certb64.
     */
    public void setSubjectDn(String subjectDn) {
        mutate();
        this.subjectDn = subjectDn;
    }

    /**
     * Gets the Base64 DER-encoded certificate
     * @return the Base64 DER-encoded certificate
     */
    @NotNull
    @Column(name="cert_base64", length=Integer.MAX_VALUE)
    @Lob
    public String getCertBase64() {
        return certBase64;
    }

    /**
     * The Issuer DN in canonical format (not for display use)
     *
     * @return The issuer DN or null
     */
    @Size(max=2048)
    @Column(name="issuer_dn",length=2048)
    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn(String issuerDn) {
        mutate();
        this.issuerDn = issuerDn;
    }

    @Type(type="com.l7tech.server.util.BigIntegerBase64UserType")
    public BigInteger getSerial() {
        return serial;
    }

    public void setSerial(BigInteger serial) {
        mutate();
        this.serial = serial;
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

    @SuppressWarnings({ "RedundantIfStatement" })
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        X509Entity that = (X509Entity)o;

        if (certBase64 != null ? !certBase64.equals(that.certBase64) : that.certBase64 != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (certBase64 != null ? certBase64.hashCode() : 0);
        return result;
    }

    protected void copyFrom(X509Entity that) {
        mutate();
        this.certBase64 = that.certBase64;
        this.cachedCert = that.cachedCert;
        this.thumbprintSha1 = that.thumbprintSha1;
        this.subjectDn = that.subjectDn;
        this.issuerDn = that.issuerDn;
        this.serial = that.serial;
    }
}
