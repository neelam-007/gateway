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
 * 
 */
@XmlRootElement(name="CertificateData")
@XmlType(name="CertificateDataType",propOrder={"issuerName","serialNumber","subjectName","extensions","encoded"})
public class CertificateData {

    //- PUBLIC

    @XmlElement(name="IssuerName")
    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName( final String issuerName ) {
        this.issuerName = issuerName;
    }

    @XmlElement(name="SerialNumber")
    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber( final BigInteger serialNumber ) {
        this.serialNumber = serialNumber;
    }

    @XmlElement(name="SubjectName")
    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName( final String subjectName ) {
        this.subjectName = subjectName;
    }

    @XmlElement(name="Encoded", required=true)
    public byte[] getEncoded() {
        return encoded;
    }

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
