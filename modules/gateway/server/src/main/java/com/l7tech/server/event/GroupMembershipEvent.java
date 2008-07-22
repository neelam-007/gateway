/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.identity.Group;
import com.l7tech.server.event.admin.Updated;

public class GroupMembershipEvent extends Updated<Group> {
    public GroupMembershipEvent(GroupMembershipEventInfo gm) {
        super(gm.getGroup(), EntityChangeSet.NONE, gm.getNote());
    }
}
