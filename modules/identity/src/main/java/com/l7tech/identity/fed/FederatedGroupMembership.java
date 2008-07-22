/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.GroupMembership;

/**
 * This class still has a composite primary key, so it uses the inherited
 * {@link #getOid()} property to store the Federated Group OID
 */
public class FederatedGroupMembership extends GroupMembership {
    public FederatedGroupMembership() {
    }

    public FederatedGroupMembership(long providerOid, long groupOid, long userOid)
            throws NumberFormatException
    {
        this.thisGroupProviderOid = providerOid;
        this._oid = groupOid;
        this.memberUserId = Long.toString(userOid);
    }

    public String getThisGroupId() {
        return Long.toString(_oid);
    }

    public void setThisGroupId(String thisGroupId) throws NumberFormatException {
        if (thisGroupId == null) {
            _oid = DEFAULT_OID;
        } else {
            _oid = Long.parseLong(thisGroupId);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final FederatedGroupMembership that = (FederatedGroupMembership)o;

        if (_oid != that._oid) return false;
        if (thisGroupProviderOid != that.thisGroupProviderOid) return false;
        return !(memberUserId != null ? !memberUserId.equals(that.memberUserId) : that.memberUserId != null);
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (int)(_oid ^ (_oid >>> 32));
        result = 29 * result + (int)(thisGroupProviderOid ^ (thisGroupProviderOid >>> 32));
        result = 29 * result + (memberUserId != null ? memberUserId.hashCode() : 0);
        return result;
    }

}
