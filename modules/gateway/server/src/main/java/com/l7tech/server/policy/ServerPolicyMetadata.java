package com.l7tech.server.policy;

import com.l7tech.policy.Policy;

import java.util.Set;
import java.util.Map;

/**
 * Metadata for ServicePolicies
 *
 * @author steve
 */
public interface ServerPolicyMetadata {

    /**
     * Get the unique identifier for this policy.
     *
     * <p>The unique identifer will change whenever the main policy or any of
     * the used policies is updated.</p>
     *
     * @return The unique identifier for this server policy.
     */
    public String getPolicyUniqueIdentifier();

    /**
     * Get the policy that the server policy related to.
     *
     * <p>This will return an immutable policy.</p>
     *
     * @return The policy.
     */
    public Policy getPolicy();

    /**
     * Get the identifiers of all policies used by the server policy.
     *
     * @return The set of policy identifiers.
     */
    public Set<Long> getUsedPolicyIds( boolean includeSelf );

    /**
     * Get a map of used policy versions.
     *
     * @param includeSelf True to include the top level policy in the result
     * @return The map of policy ids to versions
     */
    public Map<Long,Integer> getDependentVersions( boolean includeSelf );

}
