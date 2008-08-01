/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.cert;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

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
        this.trustedFor.clear();
        this.trustedFor.addAll(cert.trustedFor);
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

    /** Describes what things this cert is trusted for. */
    public static enum TrustedFor {
        /** Is this cert is trusted as an SSL server cert? (probably self-signed) */
        SSL("SSL"),

        /** Is this cert is trusted as a CA that signs SSL server certs? */
        SIGNING_SERVER_CERTS("Sign Server"),

        /** Is this cert is trusted as a CA that signs SSL client certs? */
        SIGNING_CLIENT_CERTS("Sign Client"),

        /** Is this cert is trusted to sign SAML tokens? */
        SAML_ISSUER("Sign SAML"),

        /** Is this cert trusted as a SAML attesting entity? This applies to sender-vouches only. */
        SAML_ATTESTING_ENTITY("SAML Attesting Entity");

        private final String usageDescription;

        TrustedFor(String usageDescription) {
            this.usageDescription = usageDescription;
        }

        public String getUsageDescription() {
            return usageDescription;
        }
    }

    /**
     * Returns a textual description of this cert's usage. Don't parse it; use the boolean flags instead!
     *
     * @return a textual description of this cert's usage
     */
    public String getUsageDescription() {
        StringBuffer buf = new StringBuffer();
        if (EnumSet.allOf(TrustedFor.class).equals(trustedFor)) {
            buf.append("All");
        } else {
            for (TrustedFor trust : TrustedFor.values())
                if (trustedFor.contains(trust)) add(buf, trust.getUsageDescription());
        }
        if (buf.length() < 1) buf.append("None");
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
        return isTrustedFor(TrustedFor.SSL);
    }

    /**
     * Is this cert is trusted as an SSL server cert? (probably self-signed)
     * @param trustedForSsl <code>true</code> if this cert is trusted as an SSL server cert (probably self-signed), <code>false</code> otherwise.
     */
    public void setTrustedForSsl( boolean trustedForSsl ) {
        setTrustedFor(TrustedFor.SSL, trustedForSsl);
    }

    /**
     * Is this cert is trusted as a CA that signs SSL client certs?
     * @return <code>true</code> if this cert is trusted as a CA cert for signing client certs, <code>false</code> otherwise.
     */
    public boolean isTrustedForSigningClientCerts() {
        return isTrustedFor(TrustedFor.SIGNING_CLIENT_CERTS);
    }

    /**
     * Is this cert is trusted as a CA that signs SSL client certs?
     * @param trustedForSigningClientCerts <code>true</code> if this cert is trusted as a CA cert for signing client certs,
     * <code>false</code> otherwise.
     */
    public void setTrustedForSigningClientCerts( boolean trustedForSigningClientCerts ) {
        setTrustedFor(TrustedFor.SIGNING_CLIENT_CERTS, trustedForSigningClientCerts);
    }

    /**
     * Is this cert is trusted as a CA that signs SSL server certs?
     * @return <code>true</code> if this cert is trusted as a CA cert for signing server certs,
     * <code>false</code> otherwise.
     */
    public boolean isTrustedForSigningServerCerts() {
        return isTrustedFor(TrustedFor.SIGNING_SERVER_CERTS);
    }

    /**
     * Is this cert is trusted as a CA that signs SSL server certs?
     * @param trustedForSigningServerCerts <code>true</code> if this cert is trusted as a CA cert
     * for signing server certs, <code>false</code> otherwise.
     */
    public void setTrustedForSigningServerCerts( boolean trustedForSigningServerCerts ) {
        setTrustedFor(TrustedFor.SIGNING_SERVER_CERTS, trustedForSigningServerCerts);
    }

    /**
     * Is this cert is trusted to sign SAML tokens?
     * @return <code>true</code> if this cert is trusted to sign SAML tokens, <code>false</code> otherwise.
     */
    public boolean isTrustedAsSamlIssuer() {
        return isTrustedFor(TrustedFor.SAML_ISSUER);
    }

    /**
     * Is this cert is trusted to sign SAML tokens?
     * @param trustedAsSamlIssuer <code>true</code> if this cert is trusted to sign SAML tokens, <code>false</code> otherwise.
     */
    public void setTrustedAsSamlIssuer( boolean trustedAsSamlIssuer ) {
        setTrustedFor(TrustedFor.SAML_ISSUER, trustedAsSamlIssuer);
    }

    /**
     * Is this cert trusted as a SAML attesting entity? This applies to sender-vouches only.
     * @return <code>true</code> if this cert is trusted as SAML attesting entity, <code>false</code> otherwise.
     */
    public boolean isTrustedAsSamlAttestingEntity() {
        return isTrustedFor(TrustedFor.SAML_ATTESTING_ENTITY);
    }

    /**
     * @param trustedAsSamlAttestingEntity the new flag value
     * @see #isTrustedAsSamlAttestingEntity()
     */
    public void setTrustedAsSamlAttestingEntity(boolean trustedAsSamlAttestingEntity) {
        setTrustedFor(TrustedFor.SAML_ATTESTING_ENTITY, trustedAsSamlAttestingEntity);
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

    /**
     * Check if the trusted cert is expired or not.
     * @return true if the cert is expired.
     * @throws java.security.cert.CertificateException if the cert cannot be deserialized.
     */
    public boolean isExpiredCert() throws CertificateException {
        Date expiryDate = this.getCertificate().getNotAfter();
        Date today = new Date(System.currentTimeMillis());
        return expiryDate.before(today);
    }

    /**
     * Check a TrustedFor flag value.
     *
     * @param flag the flag to test.  Required.
     * @return true if the specified TrustedFor flag is set for this TrustedCert.
     */
    public boolean isTrustedFor(TrustedFor flag) {
        return trustedFor.contains(flag);
    }

    /**
     * Check for more than one TrustedFor flag value.
     *
     * @param flags the flags to require.  Required.
     * @return true if this TrustedCert has set all of the specified TrustedFor flags.
     */
    public boolean isTrustedForAll(Set<TrustedFor> flags) {
        return trustedFor.containsAll(flags);
    }

    /**
     * Set a TrustedFor flag value.
     *
     * @param flag  the flag to set.  Required.
     * @param value  the new value to set it to.
     */
    public void setTrustedFor(TrustedFor flag, boolean value) {
        if (flag == null) throw new NullPointerException();
        if (value)
            trustedFor.add(flag);
        else
            trustedFor.remove(flag);
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrustedCert)) return false;
        if (!super.equals(o)) return false;

        TrustedCert that = (TrustedCert) o;

        if (trustAnchor != that.trustAnchor) return false;
        if (verifyHostname != that.verifyHostname) return false;
        if (subjectDn != null ? !subjectDn.equals(that.subjectDn) : that.subjectDn != null) return false;
        if (trustedFor != null ? !trustedFor.equals(that.trustedFor) : that.trustedFor != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (subjectDn != null ? subjectDn.hashCode() : 0);
        result = 31 * result + (trustedFor != null ? trustedFor.hashCode() : 0);
        result = 31 * result + (verifyHostname ? 1 : 0);
        result = 31 * result + (trustAnchor ? 1 : 0);
        result = 31 * result + (revocationCheckPolicyType != null ? revocationCheckPolicyType.hashCode() : 0);
        result = 31 * result + (revocationCheckPolicyOid != null ? revocationCheckPolicyOid.hashCode() : 0);
        return result;
    }

    private String subjectDn;
    private final Set<TrustedFor> trustedFor = EnumSet.noneOf(TrustedFor.class);
    private boolean verifyHostname;
    private boolean trustAnchor;
    private PolicyUsageType revocationCheckPolicyType;
    private Long revocationCheckPolicyOid;
}
