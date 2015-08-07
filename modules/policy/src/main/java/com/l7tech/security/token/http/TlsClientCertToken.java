package com.l7tech.security.token.http;

import java.security.cert.X509Certificate;
import com.l7tech.common.io.CertUtils;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.security.token.SecurityTokenType;

/**
 *
 */
public class TlsClientCertToken implements X509SecurityToken {

    //- PUBLIC

    public TlsClientCertToken(final X509Certificate certificate) {
        // Get DN from cert, ie "CN=testuser, OU=ssg.example.com"
        // String certCN = getCachedClientCert.getSubjectDN().getName();
        // fla changed this to:
        this.certDn = certificate.getSubjectX500Principal().getName();
        this.certCn = CertUtils.extractFirstCommonNameFromCertificate(certificate);
        this.certificate = certificate;
    }

    @Override
    public X509Certificate getCertificate() {
        return certificate;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.HTTP_CLIENT_CERT;
    }

    /**
     * Get the first CN value in the DN.
     *
     * @return The CN value or null
     */
    public String getCertCn() {
        return certCn;
    }

    /**
     * Get the DN for the certificate.
     *
     * @return The DN value
     */
    public String getCertDn() {
        return certDn;
    }

    //- PRIVATE

    private final X509Certificate certificate;
    private final String certDn;
    private final String certCn;
}
