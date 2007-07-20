package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.common.security.RevocationCheckPolicy;

/**
 * Manager for Revocation Check Policies.
 *
 * @author Steve Jones
 */
public interface RevocationCheckPolicyManager extends EntityManager<RevocationCheckPolicy, EntityHeader> {
}
