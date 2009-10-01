package com.l7tech.policy;

import com.l7tech.common.io.CertUtils;

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
        issuerDn = CertUtils.getIssuerDN( certificate );
        serialNumber = certificate.getSerialNumber();
        subjectDn = CertUtils.getSubjectDN( certificate );
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

    public boolean matches( final X509Certificate certificate ) {
        return getSubjectDn() != null &&
               getIssuerDn() != null &&
               getSerialNumber() !=null &&
               CertUtils.formatDN( getSubjectDn() ).equals( CertUtils.getSubjectDN( certificate ) ) &&
               CertUtils.formatDN( getIssuerDn() ).equals( CertUtils.getIssuerDN( certificate ) ) &&
               getSerialNumber().equals( certificate.getSerialNumber() );
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CertificateInfo that = (CertificateInfo) o;

        if (issuerDn != null ? !issuerDn.equals(that.issuerDn) : that.issuerDn != null) return false;
        if (serialNumber != null ? !serialNumber.equals(that.serialNumber) : that.serialNumber != null) return false;
        if (subjectDn != null ? !subjectDn.equals(that.subjectDn) : that.subjectDn != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (issuerDn != null ? issuerDn.hashCode() : 0);
        result = 31 * result + (serialNumber != null ? serialNumber.hashCode() : 0);
        result = 31 * result + (subjectDn != null ? subjectDn.hashCode() : 0);
        return result;
    }

    //- PRIVATE

    private String issuerDn;
    private BigInteger serialNumber;
    private String subjectDn;
    
}
