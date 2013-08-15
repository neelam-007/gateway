package com.l7tech.identity.ldap;

import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;

import java.io.Serializable;

public class LdapGroup extends LdapIdentityBase implements Group, LdapIdentity, Serializable {
    public static final int OU_GROUP = 0;
    public static final int NORMAL_GROUP = 1;

    private String description;

    public LdapGroup() {
        this(IdentityProviderConfig.DEFAULT_GOID, null, null);
    }

    public LdapGroup(Goid providerGoid, String dn, String cn) {
        super(providerGoid, dn, cn);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "com.l7tech.identity.Group." +
                "\n\tName=" + getName() +
                "\n\tDN=" + getDn() +
                "\n\tproviderId=" + providerId;
    }

    /**
     * allows to set all properties from another object
     */
    public void copyFrom( Group objToCopy) {
        LdapGroup imp = (LdapGroup)objToCopy;
        setDn(imp.getDn());
        setCn(imp.getCn());
        setDescription(imp.getDescription());
        setProviderId(imp.getProviderId());
        setAttributes(imp.getAttributes());
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LdapGroup ldapGroup = (LdapGroup) o;

        if (description != null ? !description.equals(ldapGroup.description) : ldapGroup.description != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
