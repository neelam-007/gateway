package com.l7tech.identity.ldap;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntity;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.policy.UsesPrivateKeys;
import com.l7tech.util.GoidUpgradeMapper;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Transient;
import java.io.Serializable;

/**
 * Superclass for identity provider configs that are based on a list of LDAP and/or LDAPS URLs.
 */
public abstract class LdapUrlBasedIdentityProviderConfig extends IdentityProviderConfig implements UsesPrivateKeys, Serializable {
    public static final String URL = "ldapurl";
    private static final String CLIENT_AUTH_ENABLED = "clientAuth";
    private static final String KEYSTORE_ID = "keystoreId";
    private static final String KEY_ALIAS = "keyAlias";

    protected LdapUrlBasedIdentityProviderConfig(IdentityProviderType type) {
        super(type);
    }

    /**
     * @return the ldap urls for connecting to the directory.
     */
    @Transient
    public String[] getLdapUrl() {
        Object prop = getProperty(URL);
        // Backward compatibility
        if (prop instanceof String) {
            return new String[]{(String)prop};
        } else {
            return (String[])prop;
        }
    }

    /**
     * @param ldapUrl the ldap urls for connecting to the directory.
     */
    public void setLdapUrl(String[] ldapUrl) {
        setProperty(URL, ldapUrl);
    }

    /**
     * @return  TRUE if using client authentication or if no client auth was found (backward compatiblity) otherwise FALSE.
     */
    @Transient
    public boolean isClientAuthEnabled() {
        Boolean b = (Boolean) getProperty(CLIENT_AUTH_ENABLED);
        return b == null || b;
    }

    public void setClientAuthEnabled(boolean clientAuthEnabled) {
        setProperty(CLIENT_AUTH_ENABLED, clientAuthEnabled);
    }

    /**
     * @return  Keystore Id used for client auth or NULL for default key.
     */
    @Transient
    public Goid getKeystoreId() {
        return GoidUpgradeMapper.mapId(EntityType.SSG_KEYSTORE, this.<String>getProperty(KEYSTORE_ID));
    }

    public void setKeystoreId(@Nullable Goid keystoreId) {
        setProperty(KEYSTORE_ID, keystoreId);
    }

    /**
     * @return  Key alias used for client auth or NULL for default key.
     */
    @Transient
    public String getKeyAlias() {
        return (String) getProperty(KEY_ALIAS);
    }

    public void setKeyAlias(@Nullable String keyAlias) {
        setProperty(KEY_ALIAS, keyAlias);
    }

    @Override
    public SsgKeyHeader[] getPrivateKeysUsed() {
        if (isClientAuthEnabled()) {
            final Goid keystoreId = getKeystoreId() == null ? GoidEntity.DEFAULT_GOID : getKeystoreId();
            return new SsgKeyHeader[]{new SsgKeyHeader(keystoreId + ":" + getKeyAlias(), keystoreId, getKeyAlias(), getKeyAlias())};
        }
        return null;
    }
}
