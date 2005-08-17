/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.security;

import java.io.Serializable;
import java.security.cert.X509Certificate;

/**
 * An certificate that is trusted by the SSG for a variety of purposes.
 *
 * Contains a {@link X509Certificate} and several boolean flags indicating the purposes for
 * which the cert is trusted.
 *
 * @author alex
 */
public class TrustedCert extends X509Entity implements Serializable, Cloneable {
    public void copyFrom(TrustedCert cert) {
        this.certBase64 = cert.certBase64;
        this.cachedCert = cert.cachedCert;
        this.subjectDn = cert.subjectDn;
        this.trustedForSsl = cert.trustedForSsl;
        this.trustedForSigningClientCerts = cert.trustedForSigningClientCerts;
        this.trustedForSigningServerCerts = cert.trustedForSigningServerCerts;
        this.trustedAsSamlIssuer = cert.trustedAsSamlIssuer;
        this.trustedAsSamlAttestingEntity = cert.trustedAsSamlAttestingEntity;
        this.thumbprintSha1 = cert.thumbprintSha1;
    }

    public static final String CERT_FACTORY_ALGORITHM = "X.509";

    /**
     * Returns a textual description of this cert's usage. Don't parse it; use the boolean flags instead!
     * @return a textual description of this cert's usage
     */
    public String getUsageDescription() {
        StringBuffer buf = new StringBuffer();
        if (trustedForSsl && trustedForSigningClientCerts && trustedForSigningServerCerts && trustedAsSamlIssuer) {
            buf.append("All");
        } else {
            if (trustedForSsl) add(buf, "SSL");
            if (trustedForSigningServerCerts) add(buf, "Sign Server");
            if (trustedForSigningClientCerts) add(buf, "Sign Client");
            if (trustedAsSamlIssuer) add(buf, "Sign SAML");
            if (trustedAsSamlAttestingEntity) add(buf, "SAML Attesting Entity");
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
    public boolean isTrustedAsSamlIssuer() {
        return trustedAsSamlIssuer;
    }

    /**
     * Is this cert is trusted to sign SAML tokens?
     * @param trustedAsSamlIssuer <code>true</code> if this cert is trusted to sign SAML tokens, <code>false</code> otherwise.
     */
    public void setTrustedAsSamlIssuer( boolean trustedAsSamlIssuer ) {
        this.trustedAsSamlIssuer = trustedAsSamlIssuer;
    }

    /**
     * Is this cert trusted as a SAML attesting entity? This applies to sender-vouches only.
     * @return <code>true</code> if this cert is trusted as SAML attesting entity, <code>false</code> otherwise.
     */
    public boolean isTrustedAsSamlAttestingEntity() {
        return trustedAsSamlAttestingEntity;
    }

    /**
     * @see #isTrustedAsSamlAttestingEntity()
     */
    public void setTrustedAsSamlAttestingEntity(boolean trustedAsSamlAttestingEntity) {
        this.trustedAsSamlAttestingEntity = trustedAsSamlAttestingEntity;
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

        if (!super.equals(o)) return false;
        if ( trustedForSigningClientCerts != trustedCert.trustedForSigningClientCerts ) return false;
        if ( trustedAsSamlIssuer != trustedCert.trustedAsSamlIssuer ) return false;
        if ( trustedAsSamlAttestingEntity != trustedCert.trustedAsSamlAttestingEntity ) return false;
        if ( trustedForSigningServerCerts != trustedCert.trustedForSigningServerCerts ) return false;
        if ( trustedForSsl != trustedCert.trustedForSsl ) return false;
        if ( subjectDn != null ? !subjectDn.equals( trustedCert.subjectDn ) : trustedCert.subjectDn != null ) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = super.hashCode();
        result = 29 * result + (subjectDn != null ? subjectDn.hashCode() : 0);
        result = 29 * result + (trustedForSsl ? 1 : 0);
        result = 29 * result + (trustedForSigningClientCerts ? 1 : 0);
        result = 29 * result + (trustedForSigningServerCerts ? 1 : 0);
        result = 29 * result + (trustedAsSamlIssuer ? 1 : 0);
        result = 29 * result + (trustedAsSamlAttestingEntity ? 1 : 0);
        return result;
    }

    private String subjectDn;
    private boolean trustedForSsl;
    private boolean trustedForSigningClientCerts;
    private boolean trustedForSigningServerCerts;
    private boolean trustedAsSamlIssuer;
    private boolean trustedAsSamlAttestingEntity;
}
