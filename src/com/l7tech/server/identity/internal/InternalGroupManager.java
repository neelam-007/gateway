/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.internal;

import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.server.identity.PersistentGroupManager;

/**
 * @author alex
 */
public interface InternalGroupManager extends PersistentGroupManager<InternalUser, InternalGroup> {
    void configure( InternalIdentityProvider provider );
}
