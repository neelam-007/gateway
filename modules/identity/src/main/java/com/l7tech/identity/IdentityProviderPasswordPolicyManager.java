package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

/**
 * Manager interface for managing identity provider password policy.
 */
public interface IdentityProviderPasswordPolicyManager extends EntityManager<IdentityProviderPasswordPolicy, EntityHeader> {

    public IdentityProviderPasswordPolicy findByInternalIdentityProviderOid( long identityProviderOid ) throws FindException;
}