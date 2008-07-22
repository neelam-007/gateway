/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.internal;

import com.l7tech.identity.internal.InternalUser;
import com.l7tech.server.identity.PersistentUserManager;

/**
 * @author alex
 */
public interface InternalUserManager extends PersistentUserManager<InternalUser> {
    void configure( InternalIdentityProvider provider );
}
