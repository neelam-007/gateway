package com.l7tech.identity.attribute;

import com.l7tech.identity.Identity;

public abstract class IdentityMapping extends AttributeMapping {
    private long providerOid;
    private boolean validForUsers;
    private boolean validForGroups;

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

    public abstract Object[] extractValues(Identity identity);
}
