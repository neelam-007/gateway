/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyDeletionForbiddenException;
import com.l7tech.policy.CircularPolicyException;
import com.l7tech.policy.PolicyType;

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
    ServerPolicyHandle getServerPolicy( Policy policy);

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
     * Get a handle for the ServerPolicy of the given Policy.
     *
     * <p>The handle MUST be closed when no longer required.</p>
     *
     * @param policyGuid The GUID of the policy whose ServerPolicy is desired.
     * @return The handle for the policy or null if the policy is not valid
     */
    ServerPolicyHandle getServerPolicy(String policyGuid);

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
     * Get GUIDs for global policies by type.
     *
     * @param type The type of policy (null for any type)
     * @param tag The desired tag (null for any tag)
     * @return The set of GUIDs for matching policies
     */
    Set<String> getGlobalPoliciesByTypeAndTag( PolicyType type, String tag );

    /**
     * Register a global policy.
     *
     * @param name The name for the policy (required)
     * @param type The type of the policy (required)
     * @param tag The tag for the policy (may be null)
     * @param xml The policy XML (required)
     * @return The GUID for the registered policy
     */
    String registerGlobalPolicy( String name, PolicyType type, String tag, String xml );

    /**
     * Unregister a global policy.
     *
     * @param guid The GUID of the policy to unregister (required)
     */
    void unregisterGlobalPolicy( String guid );

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

    /**
     * Get the metadata of the given Policy.
     *
     * @param policy The policy whose metadata is desired.
     * @return The metadata for the policy or null if the policy is not valid
     */
    PolicyMetadata getPolicyMetadata(Policy policy);

    /**
     * Get the metadata of the given Policy.
     *
     * @param policyOid The OID of the policy whose metadata is desired.
     * @return The metadata for the policy or null if the policy is not valid
     */
    PolicyMetadata getPolicyMetadata(long policyOid);

    /**
     * Get the metadata of the given Policy.
     *
     * @param guid The GUID of the policy whose metadata is desired.
     * @return The metadata for the policy or null if the policy is not valid or is not known.
     */
    PolicyMetadata getPolicyMetadataByGuid(String guid);

    /**
     * Check if this policy cache is ready to use.
     *
     * @return false if this cache has not yet started.
     *         true if queries for policies should now be expected to succeed.
     */
    boolean isStarted();
}
