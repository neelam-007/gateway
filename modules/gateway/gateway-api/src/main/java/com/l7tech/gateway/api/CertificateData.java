package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ElementExtensionSupport;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigInteger;

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
@XmlType(name="CertificateDataType",propOrder={"issuerNameValue","serialNumberValue","subjectNameValue","encodedValue","extension","extensions"})
public class CertificateData extends ElementExtensionSupport {

    //- PUBLIC

    /**
     * Get the issuer name for the encoded certificate.
     *
     * @return The issuer name or null if not set.
     */
    public String getIssuerName() {
        return get(issuerName);
    }

    /**
     * Set the issuer name for the encoded certificate.
     *
     * @param issuerName The issuer name
     */
    public void setIssuerName( final String issuerName ) {
        this.issuerName = set(this.issuerName,issuerName);
    }

    /**
     * Get the serial number for the encoded certificate.
     *
     * @return The serial number or null if not set.
     */
    public BigInteger getSerialNumber() {
        return get(serialNumber);
    }

    /**
     * Set the serial number for the encoded certificate.
     *
     * @param serialNumber The serial number
     */
    public void setSerialNumber( final BigInteger serialNumber ) {
        this.serialNumber = set(this.serialNumber,serialNumber);
    }

    /**
     * Get the subject name for the encoded certificate.
     *
     * @return The subject name or null if not set
     */
    public String getSubjectName() {
        return get(subjectName);
    }

    /**
     * Set the subject name for the encoded certificate.
     *
     * @param subjectName The subject name
     */
    public void setSubjectName( final String subjectName ) {
        this.subjectName = set(this.subjectName,subjectName);
    }

    /**
     * Get the encoded certificate (required)
     *
     * @return The encoded certificate or null if not set
     */
    public byte[] getEncoded() {
        return get(encoded);
    }

    /**
     * Set the encoded certificate
     *
     * @param encoded The encoded bytes for the certificate.
     */
    public void setEncoded( final byte[] encoded ) {
        this.encoded = set(this.encoded,encoded);
    }

    //- PROTECTED

    @XmlElement(name="IssuerName")
    protected AttributeExtensibleString getIssuerNameValue() {
        return issuerName;
    }

    protected void setIssuerNameValue( final AttributeExtensibleString issuerName ) {
        this.issuerName = issuerName;
    }

    @XmlElement(name="SerialNumber")
    protected AttributeExtensibleBigInteger getSerialNumberValue() {
        return serialNumber;
    }

    protected void setSerialNumberValue( final AttributeExtensibleBigInteger serialNumber ) {
        this.serialNumber = serialNumber;
    }

    @XmlElement(name="SubjectName")
    protected AttributeExtensibleString getSubjectNameValue() {
        return subjectName;
    }

    protected void setSubjectNameValue( final AttributeExtensibleString subjectName ) {
        this.subjectName = subjectName;
    }

    @XmlElement(name="Encoded", required=true)
    protected AttributeExtensibleByteArray getEncodedValue() {
        return encoded;
    }

    protected void setEncodedValue( final AttributeExtensibleByteArray encoded ) {
        this.encoded = encoded;
    }

    //- PACKAGE

    CertificateData() {        
    }

    //- PRIVATE

    private AttributeExtensibleString issuerName;
    private AttributeExtensibleBigInteger serialNumber;
    private AttributeExtensibleString subjectName;
    private AttributeExtensibleByteArray encoded;

}
