/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.internal;

/**
 * @author alex
 * @version $Revision$
 */
public class GroupMembership {
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

    private long _userOid;
    private long _groupOid;
}
