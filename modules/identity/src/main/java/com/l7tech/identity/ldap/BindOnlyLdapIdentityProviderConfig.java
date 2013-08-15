package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import org.hibernate.annotations.Proxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Configuration for a simple bind-only non-listable LDAP-based identity provider.
 */
@XmlRootElement
@Entity
@Proxy(lazy=false)
@DiscriminatorValue("4")
public class BindOnlyLdapIdentityProviderConfig extends LdapUrlBasedIdentityProviderConfig implements Serializable {
    public BindOnlyLdapIdentityProviderConfig(IdentityProviderConfig config) {
        super(IdentityProviderType.BIND_ONLY_LDAP);
        setVersion(config.getVersion());
	    setGoid(config.getGoid());
        copyFrom(config);
    }

    public BindOnlyLdapIdentityProviderConfig() {
        super(IdentityProviderType.BIND_ONLY_LDAP);
    }

    @Transient
    public String getBindPatternPrefix() {
        return (String) getProperty(BIND_PATTERN_PREFIX);
    }

    public void setBindPatternPrefix(String prefix) {
        setProperty(BIND_PATTERN_PREFIX, prefix);
    }

    @Transient
    public String getBindPatternSuffix() {
        return (String) getProperty(BIND_PATTERN_SUFFIX);
    }

    public void setBindPatternSuffix(String suffix) {
        setProperty(BIND_PATTERN_SUFFIX, suffix);
    }

    @Override
    @Transient
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean canIssueCertificates() {
        return false;
    }

    @Override
    @Transient
    public boolean isCredentialsRequiredForTest() {
        return true;
    }

    @Override
    @Transient
    public boolean isAdminEnabled() {
        return false;
    }

    public static final String BIND_PATTERN_PREFIX = "bindPatternPrefix";
    public static final String BIND_PATTERN_SUFFIX = "bindPatternSuffix";
}
