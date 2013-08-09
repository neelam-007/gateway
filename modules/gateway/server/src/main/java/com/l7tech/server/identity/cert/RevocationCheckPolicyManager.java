package com.l7tech.server.identity.cert;

import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.*;

/**
 * Manager for Revocation Check Policies.
 *
 * @author Steve Jones
 */
public interface RevocationCheckPolicyManager extends GoidEntityManager<RevocationCheckPolicy, EntityHeader> {
    /**
     * Update any policies flagged as default.
     */
    void updateDefault(Goid oid, RevocationCheckPolicy revocationCheckPolicy) throws FindException, UpdateException;

    RevocationCheckPolicy getDefaultPolicy() throws FindException;
}
