package com.l7tech.identity.internal;

import com.l7tech.identity.IdentityProviderConfig;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 23, 2003
 *
 * The internal id provider config.
 * Setters have been overwriten because we only have one internal id provider.
 * 
 */
public class InternalIDProviderConfig implements IdentityProviderConfig {

    public InternalIDProviderConfig() {
    }

    public String getDescription() {
        return NAME;
    }

    public void setDescription(String description) {
        //_description = description;
    }

    public void setName(String name) {
        // do nothing on purpose
    }

    public String getName() {
        return NAME;
    }


    public long getOid() {
        return INTERNAL_ID_PROVIDER_CONFIG_OID;
    }

    public void setOid(long oid) {
        // do nothing on purpose
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalIDProviderConfig)) return false;
        // this is always true because we can only have one of these
        return true;
    }

    public int hashCode() {
        int result;
        result = (getDescription() != null ? getDescription().hashCode() : 0);
        return result;
    }

    // always the same
    public static long internalProviderConfigID () {
        return INTERNAL_ID_PROVIDER_CONFIG_OID;
    }

    private static final long INTERNAL_ID_PROVIDER_CONFIG_OID = -354684;
    private static final String NAME = "Internal Identity Provider";
}
