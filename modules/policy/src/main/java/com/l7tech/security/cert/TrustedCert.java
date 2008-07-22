/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.cert;

import javax.xml.bind.annotation.XmlRootElement;
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

@XmlRootElement
public class TrustedCert extends X509Entity implements Serializable, Cloneable {
    public void copyFrom(TrustedCert cert) {
        this._name = cert._name;
        this.certBase64 = cert.certBase64;
        this.cachedCert = cert.cachedCert;
        this.subjectDn = cert.subjectDn;
        this.trustedForSsl = cert.trustedForSsl;
        this.trustedForSigningClientCerts = cert.trustedForSigningClientCerts;
        this.trustedForSigningServerCerts = cert.trustedForSigningServerCerts;
        this.trustedAsSamlIssuer = cert.trustedAsSamlIssuer;
        this.trustedAsSamlAttestingEntity = cert.trustedAsSamlAttestingEntity;
        this.verifyHostname = cert.verifyHostname;
        this.thumbprintSha1 = cert.thumbprintSha1;
    }

    public static final String CERT_FACTORY_ALGORITHM = "X.509";

    public static enum PolicyUsageType {
        /** Do not do revocation checking for this cert */
        NONE,

        /** Use the default RCP for this cert */
        USE_DEFAULT,

        /** Use the RCP specified by {@link TrustedCert#getRevocationCheckPolicyOid} for this cert */
        SPECIFIED
    }         

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
        return super.clone();
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
     * Should hostname verification be used with this certificate.
     *
     * @return The hostname verification flag
     */
    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    /**
     * Should hostname verification be used with this certificate.
     *
     * @param verifyHostname True to verify hostnames
     */
    public void setVerifyHostname(boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
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

    /**
     * @return true if this certificate is a trust anchor, i.e. path validation doesn't need to proceed any higher
     */
    public boolean isTrustAnchor() {
        return trustAnchor;
    }

    /**
     * @param trustAnchor true if this certificate is a trust anchor, i.e. path validation doesn't need to proceed any higher
     */
    public void setTrustAnchor(boolean trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    public PolicyUsageType getRevocationCheckPolicyType() {
        return revocationCheckPolicyType;
    }

    public void setRevocationCheckPolicyType(PolicyUsageType revocationCheckPolicyType) {
        this.revocationCheckPolicyType = revocationCheckPolicyType;
    }
    
    public Long getRevocationCheckPolicyOid() {
        return revocationCheckPolicyOid;
    }

    public void setRevocationCheckPolicyOid(Long oid) {
        this.revocationCheckPolicyOid = oid;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TrustedCert that = (TrustedCert) o;

        if (trustAnchor != that.trustAnchor) return false;
        if (trustedAsSamlAttestingEntity != that.trustedAsSamlAttestingEntity) return false;
        if (trustedAsSamlIssuer != that.trustedAsSamlIssuer) return false;
        if (trustedForSigningClientCerts != that.trustedForSigningClientCerts) return false;
        if (trustedForSigningServerCerts != that.trustedForSigningServerCerts) return false;
        if (trustedForSsl != that.trustedForSsl) return false;
        if (verifyHostname != that.verifyHostname) return false;
        if (subjectDn != null ? !subjectDn.equals(that.subjectDn) : that.subjectDn != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (subjectDn != null ? subjectDn.hashCode() : 0);
        result = 31 * result + (trustedForSsl ? 1 : 0);
        result = 31 * result + (trustedForSigningClientCerts ? 1 : 0);
        result = 31 * result + (trustedForSigningServerCerts ? 1 : 0);
        result = 31 * result + (trustedAsSamlIssuer ? 1 : 0);
        result = 31 * result + (trustedAsSamlAttestingEntity ? 1 : 0);
        result = 31 * result + (verifyHostname ? 1 : 0);
        result = 31 * result + (trustAnchor ? 1 : 0);
        return result;
    }

    private String subjectDn;
    private boolean trustedForSsl;
    private boolean trustedForSigningClientCerts;
    private boolean trustedForSigningServerCerts;
    private boolean trustedAsSamlIssuer;
    private boolean trustedAsSamlAttestingEntity;
    private boolean verifyHostname;
    private boolean trustAnchor;
    private PolicyUsageType revocationCheckPolicyType;
    private Long revocationCheckPolicyOid;
}
