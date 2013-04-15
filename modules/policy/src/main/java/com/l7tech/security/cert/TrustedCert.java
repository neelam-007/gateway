package com.l7tech.security.cert;

import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import com.l7tech.util.Functions;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
@Entity
@Proxy(lazy=false)
@Table(name="trusted_cert")
public class TrustedCert extends X509Entity implements NamedEntity, Cloneable, ZoneableEntity {
    public void copyFrom(TrustedCert cert) {
        mutate();
        super.copyFrom(cert);
        this.name = cert.name;
        this.trustedFor.clear();
        this.trustedFor.addAll(cert.trustedFor);
        this.verifyHostname = cert.verifyHostname;
        this.trustAnchor = cert.trustAnchor;
        this.revocationCheckPolicyType = cert.revocationCheckPolicyType;
        this.revocationCheckPolicyOid = cert.revocationCheckPolicyOid;
        this.setSecurityZone(cert.getSecurityZone());
    }

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

        /**
         * Return a predicate that returns true for a TrustedCert instance if and only if it is trusted for the specified purpose.
         * @param trustFlag trust flag to use as a predicate.
         * @return a predicate that will return true only for a TrustedCert with the specified flag.
         */
        public static Functions.Unary<Boolean, TrustedCert> predicate(final TrustedFor trustFlag) {
            return new Functions.Unary<Boolean, TrustedCert>() {
                @Override
                public Boolean call(TrustedCert trustedCert) {
                    return trustedCert.isTrustedFor(trustFlag);
                }
            };
        }
    }

    @Override
    @NotNull
    @Size(min=1,max=128)
    @Column(name="name", nullable=false, length=128)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        mutate();
        this.name = name;
    }

    /**
     * Returns a textual description of this cert's usage. Don't parse it; use the boolean flags instead!
     *
     * @return a textual description of this cert's usage
     */
    @Transient
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
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Is this cert is trusted as an SSL server cert? (probably self-signed)
     * @return <code>true</code> if this cert is trusted as an SSL server cert (probably self-signed), <code>false</code> otherwise.
     */
    @Column(name="trusted_for_ssl")
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
    @Column(name="trusted_for_client")
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
    @Column(name="trusted_for_server")
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
    @Column(name="trusted_for_saml")
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
    @Column(name="trusted_as_saml_attesting_entity")
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
    @Column(name="verify_hostname")
    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    /**
     * Should hostname verification be used with this certificate.
     *
     * @param verifyHostname True to verify hostnames
     */
    public void setVerifyHostname(boolean verifyHostname) {
        mutate();
        this.verifyHostname = verifyHostname;
    }

    /**
     * @return true if this certificate is a trust anchor, i.e. path validation doesn't need to proceed any higher
     */
    @Column(name="trust_anchor")
    public boolean isTrustAnchor() {
        return trustAnchor;
    }

    /**
     * @param trustAnchor true if this certificate is a trust anchor, i.e. path validation doesn't need to proceed any higher
     */
    public void setTrustAnchor(boolean trustAnchor) {
        mutate();
        this.trustAnchor = trustAnchor;
    }

    @NotNull
    @Column(name="revocation_type",nullable=false,length=128)
    @Enumerated(EnumType.STRING)
    public PolicyUsageType getRevocationCheckPolicyType() {
        return revocationCheckPolicyType;
    }

    public void setRevocationCheckPolicyType(PolicyUsageType revocationCheckPolicyType) {
        mutate();
        this.revocationCheckPolicyType = revocationCheckPolicyType;
    }

    @Column(name="revocation_policy_oid")
    public Long getRevocationCheckPolicyOid() {
        return revocationCheckPolicyOid;
    }

    public void setRevocationCheckPolicyOid(Long oid) {
        mutate();
        this.revocationCheckPolicyOid = oid;
    }

    /**
     * Check if the trusted cert is expired or not.
     * @return true if the cert is expired.
     * @throws java.security.cert.CertificateException if the cert cannot be deserialized.
     */
    @Transient
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
        mutate();
        if (flag == null) throw new NullPointerException();
        if (value)
            trustedFor.add(flag);
        else
            trustedFor.remove(flag);
    }

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @Override
    @ManyToOne
    @JoinColumn(name = "security_zone_oid")
    @XmlTransient
    @Nullable
    public SecurityZone getSecurityZone() {
        return securityZone;
    }

    @Override
    public void setSecurityZone(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrustedCert)) return false;
        if (!super.equals(o)) return false;

        TrustedCert that = (TrustedCert) o;

        if (trustAnchor != that.trustAnchor) return false;
        if (verifyHostname != that.verifyHostname) return false;
        if (trustedFor != null ? !trustedFor.equals(that.trustedFor) : that.trustedFor != null) return false;
        if (securityZone != null ? !securityZone.equals(that.securityZone) : that.securityZone != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (trustedFor != null ? trustedFor.hashCode() : 0);
        result = 31 * result + (verifyHostname ? 1 : 0);
        result = 31 * result + (trustAnchor ? 1 : 0);
        result = 31 * result + (securityZone != null ? securityZone.hashCode() : 0);
        return result;
    }

    private String name;
    private final Set<TrustedFor> trustedFor = EnumSet.noneOf(TrustedFor.class);
    private boolean verifyHostname;
    private boolean trustAnchor;
    private PolicyUsageType revocationCheckPolicyType;
    private Long revocationCheckPolicyOid;
    private SecurityZone securityZone;
}
