package com.l7tech.policy;

import java.security.cert.X509Certificate;
import java.math.BigInteger;

/**
 * Certificate information.
 *
 * @author steve
 */
public class CertificateInfo {

    //- PUBLIC

    public CertificateInfo( ) {
    }

    public CertificateInfo( final X509Certificate certificate ) {
        issuerDn = certificate.getIssuerDN().getName();
        serialNumber = certificate.getSerialNumber();
        subjectDn = certificate.getSubjectDN().getName();
    }

    public String getIssuerDn() {
        return issuerDn;
    }

    public void setIssuerDn( String issuerDn ) {
        this.issuerDn = issuerDn;
    }

    public BigInteger getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber( BigInteger serialNumber ) {
        this.serialNumber = serialNumber;
    }

    public String getSubjectDn() {
        return subjectDn;
    }

    public void setSubjectDn( String subjectDn ) {
        this.subjectDn = subjectDn;
    }

    //- PRIVATE

    private String issuerDn;
    private BigInteger serialNumber;
    private String subjectDn;
    
}
