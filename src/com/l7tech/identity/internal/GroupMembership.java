/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal;

import java.io.Serializable;


/**
 * A row in the internal_user_group table.
 *
 * @author alex
 * @version $Revision$
 */
public class GroupMembership implements Serializable {
    public GroupMembership() {
    }

    public GroupMembership( long userOid, long groupOid ) {
        _userOid = userOid;
        _groupOid = groupOid;
    }

    public long getUserOid() {
        return _userOid;
    }

    public void setUserOid(long userOid) {
        _userOid = userOid;
    }

    public long getGroupOid() {
        return _groupOid;
    }

    public void setGroupOid(long groupOid) {
        _groupOid = groupOid;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupMembership)) return false;

        final GroupMembership groupMembership = (GroupMembership) o;

        if (_groupOid != groupMembership._groupOid) return false;
        if (_userOid != groupMembership._userOid) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (int) (_userOid ^ (_userOid >>> 32));
        result = 29 * result + (int) (_groupOid ^ (_groupOid >>> 32));
        return result;
    }

    private long _userOid;
    private long _groupOid;
}
