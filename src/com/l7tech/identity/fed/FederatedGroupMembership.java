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
        setThisGroupId(Long.toString(groupOid));
        this.memberUserId = Long.toString(userOid);
    }

    public String getThisGroupId() {
        return Long.toString(getOid());
    }

    public void setThisGroupId(String thisGroupId) throws NumberFormatException {
        if (thisGroupId == null) {
            setOid(DEFAULT_OID);
        } else {
            setOid(Long.parseLong(thisGroupId));
        }
    }
}
