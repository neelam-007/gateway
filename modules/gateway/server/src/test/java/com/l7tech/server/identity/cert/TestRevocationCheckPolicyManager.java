package com.l7tech.server.identity.cert;

import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.OidEntityManagerStub;

import java.util.Collection;

/**
 * A fake RevocationCheckPolicyManager for unit tests.
 */
public class TestRevocationCheckPolicyManager extends OidEntityManagerStub<RevocationCheckPolicy, EntityHeader> implements RevocationCheckPolicyManager {
    @Override
    public void updateDefault(long oid, RevocationCheckPolicy revocationCheckPolicy) throws FindException, UpdateException {
        // set default
        if ( revocationCheckPolicy.isDefaultPolicy() ) {
            Collection<RevocationCheckPolicy> policies =  findAll();
            for (RevocationCheckPolicy policy : policies) {
                if (policy.isDefaultPolicy() && policy.getOid()!=oid) {
                    policy.setDefaultPolicy(false);
                    update(policy);
                }
            }
        }
    }

    @Override
    public RevocationCheckPolicy getDefaultPolicy() throws FindException {
        Collection<RevocationCheckPolicy> policies =  findAll();
        for (RevocationCheckPolicy policy : policies) {
            if (policy.isDefaultPolicy())
                return policy;
        }
        return null;
    }

}
