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
    public FederatedGroupMembership(long userOid, long groupOid) {
        super(userOid, groupOid);
    }
}
