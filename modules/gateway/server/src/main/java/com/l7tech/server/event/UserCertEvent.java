/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.event;

import com.l7tech.server.event.admin.Updated;
import com.l7tech.identity.User;

public class UserCertEvent extends Updated<User> {
    public UserCertEvent(UserCertEventInfo cer) {
        super(cer.getUser(), EntityChangeSet.NONE, cer.getNote());
    }
}
