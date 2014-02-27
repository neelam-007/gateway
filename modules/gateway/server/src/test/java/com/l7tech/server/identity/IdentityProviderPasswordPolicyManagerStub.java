package com.l7tech.server.identity;

import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerStub;

/**
 * Stub password policy manager, returns test password policy
 */
public class IdentityProviderPasswordPolicyManagerStub
        extends EntityManagerStub<IdentityProviderPasswordPolicy, EntityHeader>
        implements IdentityProviderPasswordPolicyManager {

    public static final IdentityProviderPasswordPolicy testPasswordPolicy =
        new IdentityProviderPasswordPolicy(false,false,3,10,0,0,0,0,0,0,1,90,false); // 3-10 char length

    @Override
    public IdentityProviderPasswordPolicy findByInternalIdentityProviderOid(Goid identityProviderOid) throws FindException {
        return testPasswordPolicy;
    }
}
