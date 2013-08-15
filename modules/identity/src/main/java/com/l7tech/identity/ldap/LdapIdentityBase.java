/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;

import javax.naming.InvalidNameException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapName;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
abstract class LdapIdentityBase implements LdapIdentity, Serializable {
    private static final Logger logger = Logger.getLogger(LdapIdentityBase.class.getName());

    /** Used in {@link #equals} and {@link #hashCode} for a semantic comparison (e.g. <code>OU=foo</code> is equivalent to <code>ou=foo</code>) */
    private transient LdapName ldapName;

    protected Goid providerId;
    protected String cn;
    protected String dn;
    protected transient Attributes attributes;

    /**
     * Required for serialization
     */
    @Deprecated
    protected LdapIdentityBase() {
        this(IdentityProviderConfig.DEFAULT_GOID, null, null);
    }

    protected LdapIdentityBase(Goid providerGoid, String dn, String cn) {
        this.providerId = providerGoid;
        this.dn = dn;
        this.cn = cn;
    }

    public String getId() {
        return dn;
    }

    public String getDn() {
        return dn;
    }

    public String getCn() {
        return cn;
    }

    public Goid getProviderId() {
        return providerId;
    }

    public void setProviderId(Goid providerGoid) {
        this.providerId = providerGoid;
    }

    public String getName() {
        return getCn();
    }

    public void setName(String name) {
        setCn(name);
    }

    public synchronized void setDn(String dn) {
        this.dn = dn;
        getLdapName();
    }

    public void setCn(String cn) {
        this.cn = cn;
    }

    protected synchronized LdapName getLdapName() {
        if (ldapName == null) {
            if (dn == null) return null;

            try {
                ldapName = new LdapName(dn);
            } catch (InvalidNameException e) {
                throw new IllegalArgumentException(dn + " is not a valid distinguished name", e);
            }
        }
        return ldapName;
    }

    public boolean isEquivalentId(Object thatId) {
        final LdapName thisName = getLdapName();

        if (thatId instanceof LdapName) {
            LdapName thatName = (LdapName) thatId;
            return thatName.equals(thisName);
        }

        if (thatId == null) return false;
        final String id = thatId.toString();
        try {
            LdapName thatName = new LdapName(id);
            return thatName.equals(thisName);
        } catch (InvalidNameException e) {
            logger.log(Level.INFO, "{0} is not a valid LDAP DN", thatId);
            return false;
        }
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public void setAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    /**
     * DO NOT REGENERATE!  This class has special equality rules: the {@link #ldapName} field is considered 
     * rather than {@link #dn}.
     */
    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LdapIdentityBase that = (LdapIdentityBase) o;

        final LdapName thisName = getLdapName();
        final LdapName thatName = that.getLdapName();

        if (providerId != null ? !providerId.equals(that.providerId) : that.providerId != null) return false;
        if (attributes != null ? !attributes.equals(that.attributes) : that.attributes != null) return false;
        if (cn != null ? !cn.equals(that.cn) : that.cn != null) return false;
        if (thisName != null ? !thisName.equals(thatName) : thatName != null) return false;

        return true;
    }

    /**
     * DO NOT REGENERATE!  This class has special equality rules: the {@link #ldapName} field is considered
     * rather than {@link #dn}.
     */
    public int hashCode() {
        int result;
        final LdapName thisName = getLdapName();
        result = (thisName != null ? thisName.hashCode() : 0);
        result = 31 * result + (providerId != null ? providerId.hashCode() : 0);
        result = 31 * result + (cn != null ? cn.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        return result;
    }
}
