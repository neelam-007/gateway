/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.common.policy.CircularPolicyException;

import java.util.Set;
import java.util.Map;

/**
 * PolicyCache allows access to server policies.
 *
 * @author alex
 */
public interface PolicyCache {

    /**
     * Get a handle for the ServerPolicy of the given Policy.
     *
     * <p>The handle MUST be closed when no longer required.</p>
     *
     * @param policy The policy whose ServerPolicy is desired.
     * @return The handle for the policy or null if the policy is not valid
     */
    ServerPolicyHandle getServerPolicy(Policy policy);

    /**
     * Get a handle for the ServerPolicy of the given Policy.
     *
     * <p>The handle MUST be closed when no longer required.</p>
     *
     * @param policyOid The OID of the policy whose ServerPolicy is desired.
     * @return The handle for the policy or null if the policy is not valid
     */
    ServerPolicyHandle getServerPolicy(long policyOid);

    /**
     * Notify the PolicyCache that the specified policy is a candidate for use.
     *
     * <p>An exception is thrown if the policy would cause errors if used.</p>
     *
     * @param policy the policy that may be saved or updated
     * @throws CircularPolicyException if the policy is invalid due to circularity
     */
    void validate(Policy policy) throws CircularPolicyException;

    /**
     * Notify the PolicyCache that the specified policy is a candidate for removal.
     *
     * <p>An exception is thrown if the policy should not be deleted.</p>
     *
     * @param policyOid the OID of the policy that may be deleted
     * @throws PolicyDeletionForbiddenException if the policy must not be deleted
     */
    void validateRemove(long policyOid) throws PolicyDeletionForbiddenException;

    /**
     * Notify the PolicyCache that the specified policy is new or updated.
     *
     * @param policy the policy that has been saved or updated
     */
    void update(Policy policy);

    /**
     * Notify the PolicyCache that the policy with the specified OID has been deleted.
     *
     * @param policyOid the OID of the policy that was deleted
     * @return true if removed
     */
    boolean remove(long policyOid);

    /**
     * Find any Policies that directly use the policy with the specified OID.
     *
     * <p>This will not find ancestors other than parents.</p>
     *
     * <p>This will not find invalid ancestors.</p>
     *
     * @param policyOid the OID of the policy to find usages of
     * @return the Set of policies that use the policy with the specified OID. Never null.
     */
    Set<Policy> findUsages(long policyOid);

    /**
     * Gets the map of policy OID to version for the cached policy with the given OID.
     *
     * <p>If the policy is not known then an empty map is returned</p>
     *
     * <p>For known policies the result always includes the provided policy
     * OID, mapped to its version.</p>
     *
     * @param policyOid the OID of the policy to get version info for
     * @return the version map, may be empty but never null
     */
    Map<Long, Integer> getDependentVersions(long policyOid);

    /**
     * Get the unique identifier for the current version of the policy.
     *
     * @param policyOid The policy OID
     * @return The policy unique version identifier or null
     */
    String getUniquePolicyVersionIdentifer(long policyOid);
}
