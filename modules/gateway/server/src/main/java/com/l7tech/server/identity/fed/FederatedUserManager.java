/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.fed;

import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.identity.PersistentUserManager;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author alex
 */
public interface FederatedUserManager extends PersistentUserManager<FederatedUser> {
    /**
     * Look up a federated user by canonical DN.
     *
     * @param dn DN to look up, in canonical format as returned by {@link com.l7tech.common.io.CertUtils#formatDN(java.lang.String)}.
     * @return the matching federated user, or null if not found.
     * @throws FindException
     */
    @Nullable
    @Transactional(readOnly=true)
    FederatedUser findBySubjectDN(String dn) throws FindException;

    @Transactional(readOnly=true)
    FederatedUser findByEmail(String email) throws FindException;

    void configure( FederatedIdentityProvider provider );
}
