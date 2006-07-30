/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.User;

/**
 * @author alex
 */
public interface PersistentUserManager<UT extends PersistentUser> extends UserManager<UT> {
    UT cast(User u);
}
