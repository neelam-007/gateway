/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import com.l7tech.identity.internal.GroupMembership;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedGroupMembership extends GroupMembership {
    public FederatedGroupMembership() {
        super();
    }

    public FederatedGroupMembership(long providerOid, long userOid, long groupOid) {
        super(userOid, groupOid);
        this.providerOid = providerOid;
    }

    public long getProviderOid() {
        return providerOid;
    }

    public void setProviderOid( long providerOid ) {
        this.providerOid = providerOid;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FederatedGroupMembership)) return false;

        final FederatedGroupMembership groupMembership = (FederatedGroupMembership)other;

        if (providerOid != groupMembership.providerOid) return false;
        if (groupOid != groupMembership.groupOid) return false;
        if (userOid != groupMembership.userOid) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (userOid ^ (userOid >>> 32));
        result = 29 * result + (int) (providerOid ^ (providerOid >>> 32));
        result = 29 * result + (int) (groupOid ^ (groupOid >>> 32));
        return result;
    }

    private long providerOid;
}
