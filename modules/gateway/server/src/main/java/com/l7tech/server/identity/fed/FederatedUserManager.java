/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.fed;

import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.PersistentUserManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author alex
 */
public interface FederatedUserManager extends PersistentUserManager<FederatedUser> {
    @Transactional(readOnly=true)
    FederatedUser findBySubjectDN(String dn) throws FindException;

    @Transactional(readOnly=true)
    FederatedUser findByEmail(String email) throws FindException;

    void configure( FederatedIdentityProvider provider );
}
