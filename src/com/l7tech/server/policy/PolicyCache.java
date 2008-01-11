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
import java.util.Map;

/**
 * @author alex
 */
public interface PolicyCache {
    ServerAssertion getServerPolicy(Policy policy) throws ServerPolicyException, LicenseException, IOException;

    ServerAssertion getServerPolicy(long policyOid) throws ServerPolicyException, LicenseException, IOException, FindException;

    /**
     * Notify the PolicyCache that the specified policy is new or updated.
     * @param policy the policy that has been saved or updated
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
     * Find any Policies that directly use the policy with the specified OID.
     *
     * <p>This will not find ancestors other than parents.</p>
     *
     * @param oid the OID of the policy to find usages of
     * @return the Set of policies that use the policy with the specified OID. Never null.
     */
    Set<Policy> findUsages(long oid);

    /**
     * Gets the map of policy OID to version for the cached policy with the given OID.  May be null, if no policy with
     * the given OID is in the cache.  Always includes the provided policy OID, mapped to its version.
     */
    Map<Long, Integer> getDependentVersions(long policyOid);

    /**
     * Get the unique identifier for the current version of the policy.
     *
     * @param policyOid The policy oid
     * @return The policy unique version identifier
     */
    String getUniquePolicyVersionIdentifer(long policyOid);
}
