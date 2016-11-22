/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.*;

/**
 * @author alex
 */
public interface PersistentGroupManager<UT extends PersistentUser, GT extends PersistentGroup> extends GroupManager<UT, GT> {
    GroupMembership newMembership(GT group, UT user);
    void deleteMembership(GT group, UT user);
}
