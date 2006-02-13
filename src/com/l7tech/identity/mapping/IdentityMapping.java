package com.l7tech.identity.mapping;

import com.l7tech.identity.Identity;

/**
 * Describes how the attribute described by an {@link AttributeConfig} is implmented in a particular
 * {@link com.l7tech.identity.IdentityProvider}.
 */
public abstract class IdentityMapping extends AttributeMapping {
    private long providerOid;
    private boolean validForUsers;
    private boolean validForGroups;
    private boolean unique;
    private boolean searchable;

    public long getProviderOid() {
        return providerOid;
    }

    public void setProviderOid(long providerOid) {
        this.providerOid = providerOid;
    }

    public boolean isValidForUsers() {
        return validForUsers;
    }

    public void setValidForUsers(boolean validForUsers) {
        this.validForUsers = validForUsers;
    }

    public boolean isValidForGroups() {
        return validForGroups;
    }

    public void setValidForGroups(boolean validForGroups) {
        this.validForGroups = validForGroups;
    }

    /**
     * @return true if each value of this attribute is supposed to refer to a single identity in the provider.
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * @param unique true if each value of this attribute is supposed to refer to a single identity in the provider.
     */
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    /**
     * @return true if this attribute can be used as for fast searches
     */
    public boolean isSearchable() {
        return searchable;
    }

    /**
     * @param searchable true if this attribute can be used for fast searches
     */
    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    /**
     * Extracts all the values for this attribute from the specified {@link Identity}.  Never null, may be empty.
     * @param identity the identity from which values are to be extracted
     * @return the values extracted from the specified {@link Identity}.
     */
    public abstract Object[] extractValues(Identity identity);
}
