package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntityManager;

/**
 * Manager interface for managing identity provider password policy.
 */
public interface IdentityProviderPasswordPolicyManager extends GoidEntityManager<IdentityProviderPasswordPolicy, EntityHeader> {

    public IdentityProviderPasswordPolicy findByInternalIdentityProviderOid( Goid identityProviderOid ) throws FindException;
}