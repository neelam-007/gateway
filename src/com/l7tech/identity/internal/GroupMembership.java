/*
 * Copyright (C) 2003-4 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal;

import java.io.Serializable;

/**
 * A row in a user-group intersect table.
 *
 * Each row constitutes an edge in the many-to-many relationship between Users and Groups.
 *
 * @author alex
 * @version $Revision$
 */
public class GroupMembership implements Serializable {
    public GroupMembership() {
    }

    public GroupMembership( long userOid, long groupOid ) {
        this.userOid = userOid;
        this.groupOid = groupOid;
    }

    public long getUserOid() {
        return userOid;
    }

    public void setUserOid(long userOid) {
        this.userOid = userOid;
    }

    public long getGroupOid() {
        return groupOid;
    }

    public void setGroupOid(long groupOid) {
        this.groupOid = groupOid;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof GroupMembership)) return false;

        final GroupMembership groupMembership = (GroupMembership)other;

        if (groupOid != groupMembership.groupOid) return false;
        if (userOid != groupMembership.userOid) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (userOid ^ (userOid >>> 32));
        result = 29 * result + (int) (groupOid ^ (groupOid >>> 32));
        return result;
    }

    protected long userOid;
    protected long groupOid;
}
