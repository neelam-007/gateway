/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.io.IOException;
import java.io.Serializable;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * An certificate that is trusted by the SSG for a variety of purposes.
 *
 * Contains a {@link X509Certificate} and several boolean flags indicating the purposes for
 * which the cert is trusted.
 *
 * @author alex
 * @version $Revision$
 */
public class TrustedCert extends NamedEntityImp implements Serializable, Cloneable {
    public void copyFrom(TrustedCert cert) {
        this._oid = cert._oid;
        this._name = cert._name;
        this.subjectDn = cert.subjectDn;
        this.certBase64 = cert.certBase64;
        this.trustedForSsl = cert.trustedForSsl;
        this.trustedForSigningClientCerts = cert.trustedForSigningClientCerts;
        this.trustedForSigningServerCerts = cert.trustedForSigningServerCerts;
        this.trustedForSigningSamlTokens = cert.trustedForSigningSamlTokens;
    }

    public static final String CERT_FACTORY_ALGORITHM = "X.509";

    /**
     * Returns a textual description of this cert's usage. Don't parse it; use the boolean flags instead!
     * @return a textual description of this cert's usage
     */
    public String getUsageDescription() {
        StringBuffer buf = new StringBuffer();
        if (trustedForSsl && trustedForSigningClientCerts && trustedForSigningServerCerts && trustedForSigningSamlTokens ) {
            buf.append("All");
        } else {
            if (trustedForSsl) add(buf, "SSL");
            if (trustedForSigningServerCerts) add(buf, "Sign Server");
            if (trustedForSigningClientCerts) add(buf, "Sign Client");
            if (trustedForSigningSamlTokens) add(buf, "Sign SAML");
            if (buf.length() == 0) buf.append("None");
        }
        return buf.toString();
    }

    private void add(StringBuffer buf, String s) {
        if ( buf.length() == 0 )
            buf.append(s);
        else {
            buf.append( ", " );
            buf.append(s);
        }
    }

    /**
     * Clone a trusted certificate
     * @return  Object  - The instance of the cloned object
     * @throws CloneNotSupportedException  If the object cannot be cloned.
     */
    public Object clone() throws CloneNotSupportedException {
        TrustedCert tc = (TrustedCert)super.clone();
        return tc;
    }


    /**
     * Gets the {@link X509Certificate} based on the saved {@link #certBase64}
     * @return an {@link X509Certificate}
     * @throws CertificateException if the certificate cannot be deserialized
     * @throws IOException
     */
    public synchronized X509Certificate getCertificate() throws CertificateException, IOException {
        if ( cachedCert == null ) {
            cachedCert = (X509Certificate)CertUtils.decodeCert(HexUtils.decodeBase64(certBase64));
        }
        return cachedCert;
    }

    /**
     * Sets the {@link X509Certificate}
     * @param cert the {@link X509Certificate}
     * @throws CertificateEncodingException if the certificate cannot be serialized
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

    /**
     * Is this cert is trusted as an SSL server cert? (probably self-signed)
     * @return <code>true</code> if this cert is trusted as an SSL server cert (probably self-signed), <code>false</code> otherwise.
     */
    public boolean isTrustedForSsl() {
        return trustedForSsl;
    }

    /**
     * Is this cert is trusted as an SSL server cert? (probably self-signed)
     * @param trustedForSsl <code>true</code> if this cert is trusted as an SSL server cert (probably self-signed), <code>false</code> otherwise.
     */
    public void setTrustedForSsl( boolean trustedForSsl ) {
        this.trustedForSsl = trustedForSsl;
    }

    /**
     * Is this cert is trusted as a CA that signs SSL client certs?
     * @return <code>true</code> if this cert is trusted as a CA cert for signing client certs, <code>false</code> otherwise.
     */
    public boolean isTrustedForSigningClientCerts() {
        return trustedForSigningClientCerts;
    }

    /**
     * Is this cert is trusted as a CA that signs SSL client certs?
     * @param trustedForSigningClientCerts <code>true</code> if this cert is trusted as a CA cert for signing client certs,
     * <code>false</code> otherwise.
     */
    public void setTrustedForSigningClientCerts( boolean trustedForSigningClientCerts ) {
        this.trustedForSigningClientCerts = trustedForSigningClientCerts;
    }

    /**
     * Is this cert is trusted as a CA that signs SSL server certs?
     * @return <code>true</code> if this cert is trusted as a CA cert for signing server certs,
     * <code>false</code> otherwise.
     */
    public boolean isTrustedForSigningServerCerts() {
        return trustedForSigningServerCerts;
    }

    /**
     * Is this cert is trusted as a CA that signs SSL server certs?
     * @param trustedForSigningServerCerts <code>true</code> if this cert is trusted as a CA cert
     * for signing server certs, <code>false</code> otherwise.
     */
    public void setTrustedForSigningServerCerts( boolean trustedForSigningServerCerts ) {
        this.trustedForSigningServerCerts = trustedForSigningServerCerts;
    }

    /**
     * Is this cert is trusted to sign SAML tokens?
     * @return <code>true</code> if this cert is trusted to sign SAML tokens, <code>false</code> otherwise.
     */
    public boolean isTrustedForSigningSamlTokens() {
        return trustedForSigningSamlTokens;
    }

    /**
     * Is this cert is trusted to sign SAML tokens?
     * @param trustedForSigningSamlTokens <code>true</code> if this cert is trusted to sign SAML tokens, <code>false</code> otherwise.
     */
    public void setTrustedForSigningSamlTokens( boolean trustedForSigningSamlTokens ) {
        this.trustedForSigningSamlTokens = trustedForSigningSamlTokens;
    }

    /**
     * Gets the cert subject's DN (distinguished name)
     * @return the cert subject's DN (distinguished name)
     */
    public String getSubjectDn() {
        return subjectDn;
    }

    /**
     * Sets the cert subject's DN (distinguished name)
     * @param subjectDn the cert subject's DN (distinguished name)
     */
    public void setSubjectDn( String subjectDn ) {
        this.subjectDn = subjectDn;
    }

    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof TrustedCert) ) return false;

        final TrustedCert trustedCert = (TrustedCert) o;

        if ( trustedForSigningClientCerts != trustedCert.trustedForSigningClientCerts ) return false;
        if ( trustedForSigningSamlTokens != trustedCert.trustedForSigningSamlTokens ) return false;
        if ( trustedForSigningServerCerts != trustedCert.trustedForSigningServerCerts ) return false;
        if ( trustedForSsl != trustedCert.trustedForSsl ) return false;
        if ( certBase64 != null ? !certBase64.equals( trustedCert.certBase64 ) : trustedCert.certBase64 != null ) return false;
        if ( subjectDn != null ? !subjectDn.equals( trustedCert.subjectDn ) : trustedCert.subjectDn != null ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (certBase64 != null ? certBase64.hashCode() : 0);
        result = 29 * result + (subjectDn != null ? subjectDn.hashCode() : 0);
        result = 29 * result + (trustedForSsl ? 1 : 0);
        result = 29 * result + (trustedForSigningClientCerts ? 1 : 0);
        result = 29 * result + (trustedForSigningServerCerts ? 1 : 0);
        result = 29 * result + (trustedForSigningSamlTokens ? 1 : 0);
        return result;
    }

    private transient X509Certificate cachedCert;

    private String certBase64;
    private String subjectDn;
    private boolean trustedForSsl;
    private boolean trustedForSigningClientCerts;
    private boolean trustedForSigningServerCerts;
    private boolean trustedForSigningSamlTokens;
}
