/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.event;

import com.l7tech.server.event.admin.Updated;

/**
 * @author alex
 * @version $Revision$
 */
public class GroupMembershipEvent extends Updated {
    public GroupMembershipEvent(GroupMembershipEventInfo gm) {
        super(gm.getGroup(), EntityChangeSet.NONE, gm.getNote());
    }
}
