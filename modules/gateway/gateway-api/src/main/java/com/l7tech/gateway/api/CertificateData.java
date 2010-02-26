package com.l7tech.gateway.api;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Holder for encoded {@link java.security.cert.X509Certificate X509Certificate} data.
 *
 * <p>The encoded property contains the encoded certificate bytes, other
 * properties are informational and should be derived from the certificate.</p>
 *
 * @see ManagedObjectFactory#createCertificateData()
 * @see ManagedObjectFactory#createCertificateData(String) ManagedObjectFactory.createCertificateData(String)
 * @see ManagedObjectFactory#createCertificateData(java.security.cert.X509Certificate) ManagedObjectFactory.createCertificateData(X509Certificate)
 * @see java.security.cert.CertificateFactory#generateCertificate(java.io.InputStream) CertificateFactory.generateCertificate(InputStream)
 */
@XmlRootElement(name="CertificateData")
@XmlType(name="CertificateDataType",propOrder={"issuerName","serialNumber","subjectName","extensions","encoded"})
public class CertificateData {

    //- PUBLIC

    /**
     * Get the issuer name for the encoded certificate.
     *
     * @return The issuer name or null if not set.
     */
    @XmlElement(name="IssuerName")
    public String getIssuerName() {
        return issuerName;
    }

    /**
     * Set the issuer name for the encoded certificate.
     *
     * @param issuerName The issuer name
     */
    public void setIssuerName( final String issuerName ) {
        this.issuerName = issuerName;
    }

    /**
     * Get the serial number for the encoded certificate.
     *
     * @return The serial number or null if not set.
     */
    @XmlElement(name="SerialNumber")
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    /**
     * Set the serial number for the encoded certificate.
     *
     * @param serialNumber The serial number
     */
    public void setSerialNumber( final BigInteger serialNumber ) {
        this.serialNumber = serialNumber;
    }

    /**
     * Get the subject name for the encoded certificate.
     *
     * @return The subject name or null if not set
     */
    @XmlElement(name="SubjectName")
    public String getSubjectName() {
        return subjectName;
    }

    /**
     * Set the subject name for the encoded certificate.
     *
     * @param subjectName The subject name
     */
    public void setSubjectName( final String subjectName ) {
        this.subjectName = subjectName;
    }

    /**
     * Get the encoded certificate (required)
     *
     * @return The encoded certificate or null if not set
     */
    @XmlElement(name="Encoded", required=true)
    public byte[] getEncoded() {
        return encoded;
    }

    /**
     * Set the encoded certificate
     *
     * @param encoded The encoded bytes for the certificate.
     */
    public void setEncoded( final byte[] encoded ) {
        this.encoded = encoded;
    }

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PACKAGE

    CertificateData() {        
    }

    //- PRIVATE

    private String issuerName;
    private BigInteger serialNumber;
    private String subjectName;
    private byte[] encoded;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;

}
