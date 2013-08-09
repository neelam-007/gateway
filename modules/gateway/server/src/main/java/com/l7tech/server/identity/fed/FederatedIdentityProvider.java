/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.fed;

import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.identity.PersistentIdentityProvider;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * @author alex
 */
public interface FederatedIdentityProvider extends PersistentIdentityProvider<FederatedUser, FederatedGroup, FederatedUserManager, FederatedGroupManager> {
    @Transactional(propagation= Propagation.SUPPORTS)
    Set<Goid> getValidTrustedCertOids();
}
