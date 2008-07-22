/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.fed;

import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.server.identity.PersistentGroupManager;

/**
 * @author alex
 */
public interface FederatedGroupManager extends PersistentGroupManager<FederatedUser, FederatedGroup> {
    void configure( FederatedIdentityProvider provider );
}
