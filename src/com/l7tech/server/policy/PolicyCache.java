/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.LicenseException;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.util.Set;

/**
 * @author alex
 */
public interface PolicyCache {
    ServerAssertion getServerPolicy(Policy policy) throws ServerPolicyException, LicenseException, IOException;

    ServerAssertion getServerPolicy(long policyOid) throws ServerPolicyException, LicenseException, IOException, FindException;

    /**
     * Notify the PolicyCache that the specified policy is new or updated.
     * @param policy the policy that has been saved or updated
     * @return the compiled server policy
     * @throws LicenseException if the policy contains at least one unlicensed assertion
     * @throws ServerPolicyException if the policy cannot be compiled (e.g. due to an exception thrown from a 
     *         {@link ServerAssertion} constructor)
     * @throws IOException if the policy cannot be parsed
     */
    void update(Policy policy) throws ServerPolicyException, LicenseException, IOException;

    /**
     * Notify the PolicyCache that the policy with the specified OID has been deleted.
     * @param oid the OID of the policy that was deleted
     */
    void remove(long oid) throws PolicyDeletionForbiddenException;

    /**
     * Find any Policies that use the policy with the specified OID.
     * @param oid the OID of the policy to find usages of
     * @return the Set of policies that use the policy with the specified OID. Never null.
     */
    Set<Policy> findUsages(long oid);

}
