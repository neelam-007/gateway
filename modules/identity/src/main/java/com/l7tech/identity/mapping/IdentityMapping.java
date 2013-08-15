package com.l7tech.identity.mapping;

import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UsersOrGroups;
import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.util.GoidUpgradeMapper;

/**
 * Describes how the attribute described by an {@link AttributeConfig} is implemented in a particular
 * {@link com.l7tech.identity.IdentityProvider}.
 */
public abstract class IdentityMapping extends AttributeMapping {
    private Goid providerGoid;
    private UsersOrGroups usersOrGroups;
    private boolean unique;
    private boolean searchable;

    /** The name of a custom attribute to retrieve from the identity provider.  Currently only supported for LDAP. */
    protected String customAttributeName;

    public static AttributeHeader[] getBuiltinAttributes() {
        return new AttributeHeader[] {
            AttributeHeader.ID,
            AttributeHeader.LOGIN,
            AttributeHeader.NAME,
            AttributeHeader.EMAIL,
            AttributeHeader.FIRST_NAME,
            AttributeHeader.LAST_NAME,
            AttributeHeader.DEPARTMENT,
            AttributeHeader.PROVIDER_OID,
            AttributeHeader.DESCRIPTION,
        };
    }

    protected IdentityMapping(AttributeConfig parent, Goid providerGoid, UsersOrGroups uog) {
        super(parent);
        this.providerGoid = providerGoid;
        this.usersOrGroups = uog;
    }

    public Goid getProviderOid() {
        return providerGoid;
    }

    public void setProviderOid(Goid providerOid) {
        this.providerGoid = providerOid;
    }

    // For backward compat while parsing pre-GOID policies.  Not needed for new assertions.
    @Deprecated
    public void setProviderOid(long providerOid) {
        this.providerGoid = (providerOid== IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OLD_OID) ?
                IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID:
                GoidUpgradeMapper.mapOid(EntityType.ID_PROVIDER_CONFIG,providerOid);
    }

    public boolean isValidForUsers() {
        return usersOrGroups == UsersOrGroups.USERS || usersOrGroups == UsersOrGroups.BOTH;
    }

    public void setValidForUsers(boolean validForUsers) {
        if (validForUsers) {
            if (usersOrGroups == UsersOrGroups.GROUPS) usersOrGroups = UsersOrGroups.BOTH;
        } else {
            if (usersOrGroups == UsersOrGroups.BOTH)
                usersOrGroups = UsersOrGroups.GROUPS;
            else if (usersOrGroups == UsersOrGroups.USERS)
                usersOrGroups = null; // Hopefully a transient situation
        }
    }

    public boolean isValidForGroups() {
        return usersOrGroups == UsersOrGroups.GROUPS || usersOrGroups == UsersOrGroups.BOTH;
    }

    public void setValidForGroups(boolean validForGroups) {
        if (validForGroups) {
            if (usersOrGroups == UsersOrGroups.USERS) usersOrGroups = UsersOrGroups.BOTH;
        } else {
            if (usersOrGroups == UsersOrGroups.BOTH)
                usersOrGroups = UsersOrGroups.USERS;
            else if (usersOrGroups == UsersOrGroups.GROUPS)
                usersOrGroups = null; // Hopefully a transient situation
        }
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
     * The name of a custom attribute to retrieve from the identity provider.  Currently only supported for LDAP.
     */
    public String getCustomAttributeName() {
        return customAttributeName;
    }

    public void setCustomAttributeName(String customAttributeName) {
        this.customAttributeName = customAttributeName;
    }

    @Override
    public String toString() {
        if (customAttributeName != null) return customAttributeName + " (custom)";
        if (attributeConfig == null) return "<unknown>";
        if (attributeConfig.getName() != null) return attributeConfig.getName();
        if (attributeConfig.getHeader() != null) return attributeConfig.getHeader().getName();
        return attributeConfig.getVariableName();
    }

    @SuppressWarnings({"RedundantIfStatement"})
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        IdentityMapping that = (IdentityMapping) o;

        if (providerGoid != null ? !providerGoid.equals(that.providerGoid) : that.providerGoid != null) return false;
        if (searchable != that.searchable) return false;
        if (unique != that.unique) return false;
        if (customAttributeName != null ? !customAttributeName.equals(that.customAttributeName) : that.customAttributeName != null)
            return false;
        if (usersOrGroups != that.usersOrGroups) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (providerGoid != null ? providerGoid.hashCode() : 0);
        result = 31 * result + (usersOrGroups != null ? usersOrGroups.hashCode() : 0);
        result = 31 * result + (unique ? 1 : 0);
        result = 31 * result + (searchable ? 1 : 0);
        result = 31 * result + (customAttributeName != null ? customAttributeName.hashCode() : 0);
        return result;
    }
}
