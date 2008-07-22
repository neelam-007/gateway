package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;

/**
 * Manager for Revocation Check Policies.
 *
 * @author Steve Jones
 */
public interface RevocationCheckPolicyManager extends EntityManager<RevocationCheckPolicy, EntityHeader> {
    /**
     * Update any policies flagged as default.
     */
    void updateDefault(long oid, RevocationCheckPolicy revocationCheckPolicy) throws FindException, UpdateException;

    RevocationCheckPolicy getDefaultPolicy() throws FindException;
}
